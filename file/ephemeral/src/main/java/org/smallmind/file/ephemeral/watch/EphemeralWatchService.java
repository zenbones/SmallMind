/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.file.ephemeral.watch;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.smallmind.file.ephemeral.EphemeralFileStore;
import org.smallmind.file.ephemeral.EphemeralPath;
import org.smallmind.file.ephemeral.heap.HeapEventListener;

/**
 * In-memory {@link WatchService} implementation that drives notifications from
 * {@link org.smallmind.file.ephemeral.heap.HeapEvent heap events} produced by the
 * {@link EphemeralFileStore}.
 *
 * <p>The service maintains two internal data structures:
 * <ul>
 *   <li>A map from {@link EphemeralPath} to the list of {@link EphemeralWatchKey} instances
 *       registered for that path, used to route fired events to interested keys.</li>
 *   <li>A {@link LinkedBlockingQueue} of signalled keys whose pending events are ready to
 *       be consumed via {@link #poll()}, {@link #poll(long, TimeUnit)}, or {@link #take()}.</li>
 * </ul>
 *
 * <p>A single {@link EphemeralHeapEventListener} is shared across all registrations; it is
 * attached to the {@link EphemeralFileStore} the first time a key is registered for a
 * given path and detached when the last key for that path is cancelled.
 *
 * <p>The service is thread-safe. State-mutating methods are {@code synchronized} on the
 * service instance. The {@code closed} flag is {@code volatile} to allow the
 * {@link #isClosed()} check to be read without locking.
 */
public class EphemeralWatchService implements WatchService {

  /**
   * The file store whose heap events are the source of all watch notifications.
   */
  private final EphemeralFileStore ephemeralFileStore;

  /**
   * Maps each watched {@link EphemeralPath} to the ordered list of
   * {@link EphemeralWatchKey} instances registered for it. When the list becomes empty the
   * entry is removed and the heap listener is detached from the file store.
   */
  private final HashMap<EphemeralPath, LinkedList<EphemeralWatchKey>> watchKeyMap = new HashMap<>();

  /**
   * Maps each watched {@link EphemeralPath} to the {@link HeapEventListener} that the service
   * has attached to that node in the {@link EphemeralFileStore}. One listener is created per
   * watched path so that the listener can carry its own watched-path identity when forwarding
   * heap events to {@link #fire}.
   */
  private final HashMap<EphemeralPath, HeapEventListener> heapListenerMap = new HashMap<>();

  /**
   * Queue of signalled keys available for consumption. Keys are added here when they
   * transition from the un-signalled to signalled state (i.e. their first event since the
   * last {@code reset} fires), and removed when consumed by {@code poll} or {@code take}.
   */
  private final LinkedBlockingQueue<EphemeralWatchKey> watchKeyQueue = new LinkedBlockingQueue<>();

  /**
   * {@code true} after {@link #close()} has been called. Declared {@code volatile} so
   * that read-only checks in {@link #isClosed()} and the polling loop in
   * {@link #poll(long, TimeUnit)} see the updated value without acquiring the monitor.
   */
  private volatile boolean closed = false;

  /**
   * Creates a watch service backed by the provided ephemeral file store.
   *
   * <p>The service creates an {@link EphemeralHeapEventListener} per watched path the first
   * time a key is registered for that path, and detaches the listener when the last key for
   * that path is unregistered.
   *
   * @param ephemeralFileStore the {@link EphemeralFileStore} whose heap this service will
   *                           monitor; must not be {@code null}
   */
  public EphemeralWatchService (EphemeralFileStore ephemeralFileStore) {

    this.ephemeralFileStore = ephemeralFileStore;
  }

  /**
   * Returns whether this watch service has been closed.
   *
   * <p>This method may be called without holding the service's monitor and is therefore
   * suitable for use in polling loops that need to detect closure without blocking.
   *
   * @return {@code true} if {@link #close()} has been called; {@code false} otherwise
   */
  public boolean isClosed () {

    return closed;
  }

  /**
   * Closes this watch service, invalidating all registered watch keys.
   *
   * <p>All {@link EphemeralWatchKey} instances currently registered with this service are
   * cancelled without deregistering them from the internal map (to avoid concurrent
   * modification). The {@code closed} flag is set to {@code true}, causing all subsequent
   * calls to {@link #poll()}, {@link #poll(long, TimeUnit)}, and {@link #take()} to throw
   * {@link ClosedWatchServiceException}.
   *
   * <p>Calling this method on an already-closed service has no effect.
   */
  @Override
  public synchronized void close () {

    if (!closed) {
      closed = true;

      for (LinkedList<EphemeralWatchKey> watchKeyList : watchKeyMap.values()) {
        for (EphemeralWatchKey watchKey : watchKeyList) {
          watchKey.cancel(false);
        }
      }
    }
  }

  /**
   * Registers an {@link EphemeralWatchKey} with this service.
   *
   * <p>If this is the first key registered for the key's path, a new list is created in the
   * internal map, a fresh {@link EphemeralHeapEventListener} carrying the watched path is
   * constructed and recorded, and the listener is attached to the {@link EphemeralFileStore}
   * node at that path. The key is then appended to the list regardless of whether other keys
   * for the same path already exist.
   *
   * @param ephemeralWatchKey the key to register; must not be {@code null}
   * @throws ClosedWatchServiceException if this service has already been closed
   * @throws NoSuchFileException         if the path associated with the key does not exist
   *                                     in the file store
   * @throws NotDirectoryException       if the path associated with the key exists but is
   *                                     not a directory
   */
  public synchronized void register (EphemeralWatchKey ephemeralWatchKey)
    throws NoSuchFileException, NotDirectoryException {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {

      LinkedList<EphemeralWatchKey> watchKeyList;

      if ((watchKeyList = watchKeyMap.get(ephemeralWatchKey.getPath())) == null) {

        HeapEventListener listener = new EphemeralHeapEventListener(this, ephemeralWatchKey.getPath());

        watchKeyMap.put(ephemeralWatchKey.getPath(), watchKeyList = new LinkedList<>());
        heapListenerMap.put(ephemeralWatchKey.getPath(), listener);
        ephemeralFileStore.registerHeapListener(ephemeralWatchKey.getPath(), listener);
      }

      watchKeyList.add(ephemeralWatchKey);
    }
  }

  /**
   * Removes an {@link EphemeralWatchKey} from this service's registry.
   *
   * <p>If the key is found in the list for its path, it is removed. When the list becomes
   * empty the path entry is removed from the map and the shared heap listener is detached
   * from the {@link EphemeralFileStore} for that path. The key is also removed from the
   * ready queue if it was signalled.
   *
   * <p>If the key is not currently registered, this method returns silently.
   *
   * @param ephemeralWatchKey the key to remove; must not be {@code null}
   * @throws NoSuchFileException if removing the heap listener requires the path to exist
   *                             but it no longer does
   */
  public synchronized void unregister (EphemeralWatchKey ephemeralWatchKey)
    throws NoSuchFileException {

    LinkedList<EphemeralWatchKey> watchKeyList;

    if ((watchKeyList = watchKeyMap.get(ephemeralWatchKey.getPath())) != null) {
      if (watchKeyList.remove(ephemeralWatchKey)) {
        if (watchKeyList.isEmpty()) {

          HeapEventListener listener = heapListenerMap.remove(ephemeralWatchKey.getPath());

          watchKeyMap.remove(ephemeralWatchKey.getPath());
          if (listener != null) {
            ephemeralFileStore.unregisterHeapListener(ephemeralWatchKey.getPath(), listener);
          }
        }

        watchKeyQueue.remove(ephemeralWatchKey);
      }
    }
  }

  /**
   * Dispatches a file-system change event to all keys registered for the given watched path.
   *
   * <p>The relative {@link WatchEvent#context() context} of the generated event is computed
   * by {@linkplain EphemeralPath#relativize relativizing} {@code changedPath} against
   * {@code watchedPath}; when the two paths are equal the context is {@code null}, indicating
   * a change that occurred on the watched directory itself rather than on one of its entries.
   * For each key registered against {@code watchedPath},
   * {@link EphemeralWatchKey#fire(WatchEvent.Kind, EphemeralPath)} is called. If a key
   * transitions to the signalled state as a result (i.e. it was not previously signalled),
   * it is added to the ready queue so it can be returned by the next {@code poll} or
   * {@code take} call.
   *
   * <p>This method is a no-op when the service has been closed.
   *
   * @param watchedPath the {@link EphemeralPath} identifying the directory whose registered
   *                    keys should be notified; must not be {@code null}
   * @param event       the {@link WatchEvent.Kind} describing the change that occurred;
   *                    must not be {@code null}
   * @param changedPath the {@link EphemeralPath} of the entry whose change triggered the
   *                    event; must not be {@code null}
   */
  public synchronized void fire (EphemeralPath watchedPath, WatchEvent.Kind<?> event, EphemeralPath changedPath) {

    if (!closed) {

      LinkedList<EphemeralWatchKey> watchKeyList;

      if ((watchKeyList = watchKeyMap.get(watchedPath)) != null) {

        EphemeralPath context = watchedPath.equals(changedPath) ? null : (EphemeralPath)watchedPath.relativize(changedPath);

        for (EphemeralWatchKey watchKey : watchKeyList) {
          if (watchKey.fire(event, context)) {
            watchKeyQueue.add(watchKey);
          }
        }
      }
    }
  }

  /**
   * Re-enqueues a key that has been reset but still has pending events.
   *
   * <p>Called by {@link EphemeralWatchKey#reset()} when the key's event queue is
   * non-empty at the time of the reset, ensuring the key remains available for immediate
   * consumption. This method is a no-op when the service has been closed.
   *
   * @param watchKey the {@link EphemeralWatchKey} to re-add to the ready queue;
   *                 must not be {@code null}
   */
  public synchronized void requeue (EphemeralWatchKey watchKey) {

    if (!closed) {
      watchKeyQueue.add(watchKey);
    }
  }

  /**
   * Retrieves and removes the next signalled watch key, returning {@code null} if none is
   * currently available.
   *
   * <p>This method does not block. If no key is ready, {@code null} is returned
   * immediately.
   *
   * @return the next signalled {@link WatchKey}, or {@code null} if the ready queue is
   * empty
   * @throws ClosedWatchServiceException if this service has been closed
   */
  @Override
  public WatchKey poll () {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {
      return watchKeyQueue.poll();
    }
  }

  /**
   * Retrieves and removes the next signalled watch key, waiting up to the specified timeout
   * for one to become available.
   *
   * <p>For timeouts shorter than 500 milliseconds the call is delegated directly to
   * {@link LinkedBlockingQueue#poll(long, TimeUnit)}. For longer timeouts the service polls
   * in 500-millisecond increments so that it can detect mid-wait closure and throw
   * {@link ClosedWatchServiceException} promptly rather than waiting out the full timeout
   * after the service is closed.
   *
   * @param timeout the maximum time to wait for a key
   * @param unit    the {@link TimeUnit} of the {@code timeout} argument
   * @return the next signalled {@link WatchKey}, or {@code null} if the timeout elapses
   * before one becomes available
   * @throws ClosedWatchServiceException if this service is or becomes closed before a key
   *                                     is available
   * @throws InterruptedException        if the current thread is interrupted while waiting
   */
  @Override
  public WatchKey poll (long timeout, TimeUnit unit)
    throws InterruptedException {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {

      long wait;

      if ((wait = unit.toMillis(timeout)) < 500) {
        return watchKeyQueue.poll(timeout, unit);
      } else {

        long started = System.currentTimeMillis();

        do {

          WatchKey watchKey;

          if ((watchKey = watchKeyQueue.poll(500, TimeUnit.MILLISECONDS)) != null) {

            return watchKey;
          }
        } while ((!closed) && (System.currentTimeMillis() - started < wait));

        if (closed) {
          throw new ClosedWatchServiceException();
        } else {

          return null;
        }
      }
    }
  }

  /**
   * Retrieves and removes the next signalled watch key, blocking indefinitely until one
   * becomes available.
   *
   * <p>The service polls in 500-millisecond increments so that it can detect closure and
   * throw {@link ClosedWatchServiceException} promptly rather than blocking forever.
   *
   * @return the next signalled {@link WatchKey}; never {@code null}
   * @throws ClosedWatchServiceException if this service is or becomes closed before a key
   *                                     is available
   * @throws InterruptedException        if the current thread is interrupted while waiting
   */
  @Override
  public WatchKey take ()
    throws InterruptedException {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {
      do {

        WatchKey watchKey;

        if ((watchKey = watchKeyQueue.poll(500, TimeUnit.MILLISECONDS)) != null) {

          return watchKey;
        }
      } while (!closed);

      throw new ClosedWatchServiceException();
    }
  }
}
