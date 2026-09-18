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

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.smallmind.file.ephemeral.EphemeralFileStore;
import org.smallmind.file.ephemeral.EphemeralPath;
import org.smallmind.file.ephemeral.heap.HeapEventListener;

/**
 * {@link WatchService} over the ephemeral heap.
 *
 * <p>Registration is per directory and is <em>not</em> recursive, matching the platform's own watch
 * services: a key registered on a directory reports changes to that directory's own entries, and
 * says nothing about changes deeper in the tree.
 *
 * <p>Registering the same directory twice returns the <em>same</em> key with its event kinds
 * replaced, as {@link java.nio.file.Watchable#register} specifies, rather than handing out a second
 * key for the same directory.
 */
public class EphemeralWatchService implements WatchService {

  /**
   * Enqueued by {@link #close()} so that a thread blocked in {@link #take()} wakes immediately
   * instead of waiting out a polling interval.
   */
  private static final EphemeralWatchKey CLOSE_SENTINEL = new EphemeralWatchKey(null, new WatchEvent.Kind<?>[0], null);

  private final EphemeralFileStore ephemeralFileStore;
  private final HashMap<EphemeralPath, EphemeralWatchKey> watchKeyMap = new HashMap<>();
  private final HashMap<EphemeralPath, HeapEventListener> heapListenerMap = new HashMap<>();
  private final LinkedBlockingQueue<EphemeralWatchKey> watchKeyQueue = new LinkedBlockingQueue<>();
  private volatile boolean closed = false;

  /**
   * Creates a watch service over the given store.
   *
   * @param ephemeralFileStore the store whose heap will be observed
   */
  public EphemeralWatchService (EphemeralFileStore ephemeralFileStore) {

    this.ephemeralFileStore = ephemeralFileStore;
  }

  /**
   * Returns whether this service has been closed.
   *
   * @return {@code true} if closed
   */
  public boolean isClosed () {

    return closed;
  }

  /**
   * Closes this service, invalidating every key it handed out and waking any waiting thread.
   */
  @Override
  public synchronized void close () {

    if (!closed) {
      closed = true;

      for (EphemeralWatchKey watchKey : watchKeyMap.values()) {
        watchKey.cancel(false);
      }
      watchKeyMap.clear();
      heapListenerMap.clear();
      watchKeyQueue.add(CLOSE_SENTINEL);
    }
  }

  /**
   * Registers a directory, or updates the registration of one already being watched.
   *
   * @param path   the absolute path of the directory to watch
   * @param events the kinds of event to report
   * @return the key for this directory, which is the existing key when one is already registered
   * @throws IOException                 if the path does not name an existing directory
   * @throws ClosedWatchServiceException if this service has been closed
   */
  public synchronized WatchKey register (EphemeralPath path, WatchEvent.Kind<?>[] events)
    throws IOException {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {

      EphemeralWatchKey watchKey;

      if ((watchKey = watchKeyMap.get(path)) != null) {
        // re-registering a directory updates the key it already has
        watchKey.setEvents(events);

        return watchKey;
      } else {

        HeapEventListener listener = new EphemeralHeapEventListener(this, path);

        // registered before the key is published so that a failure leaves nothing behind
        ephemeralFileStore.registerHeapListener(path, listener);

        watchKeyMap.put(path, watchKey = new EphemeralWatchKey(this, events, path));
        heapListenerMap.put(path, listener);

        return watchKey;
      }
    }
  }

  /**
   * Removes the registration of a key, stopping observation of its directory.
   *
   * @param ephemeralWatchKey the key being cancelled
   * @throws IOException if the store cannot be updated
   */
  public synchronized void unregister (EphemeralWatchKey ephemeralWatchKey)
    throws IOException {

    if (watchKeyMap.remove(ephemeralWatchKey.getPath(), ephemeralWatchKey)) {

      HeapEventListener listener;

      if ((listener = heapListenerMap.remove(ephemeralWatchKey.getPath())) != null) {
        ephemeralFileStore.unregisterHeapListener(ephemeralWatchKey.getPath(), listener);
      }

      watchKeyQueue.remove(ephemeralWatchKey);
    }
  }

  /**
   * Reports a change observed in a watched directory.
   *
   * <p>A change to the watched directory itself, rather than to one of its entries, means the
   * directory has been removed; the key for it is cancelled, exactly as a platform watch service
   * invalidates a key whose directory is deleted.
   *
   * @param watchedPath the directory the registration was made on
   * @param event       the kind of change
   * @param changedPath the absolute path of what changed
   */
  public synchronized void fire (EphemeralPath watchedPath, WatchEvent.Kind<?> event, EphemeralPath changedPath) {

    if (!closed) {

      EphemeralWatchKey watchKey;

      if ((watchKey = watchKeyMap.get(watchedPath)) != null) {
        if (watchedPath.equals(changedPath)) {
          if (StandardWatchEventKinds.ENTRY_DELETE.equals(event)) {
            watchKey.cancel();
          }
        } else if (watchKey.fire(event, watchedPath.relativize(changedPath))) {
          watchKeyQueue.add(watchKey);
        }
      }
    }
  }

  /**
   * Re-queues a key that still holds unread events after being reset.
   *
   * @param watchKey the key to re-queue
   */
  public synchronized void requeue (EphemeralWatchKey watchKey) {

    if (!closed) {
      watchKeyQueue.add(watchKey);
    }
  }

  /**
   * Removes the sentinel from the queue when it surfaces, so that every waiting thread sees it.
   *
   * @param watchKey the key taken from the queue
   * @return the key, or {@code null} when the key was the close sentinel
   * @throws ClosedWatchServiceException if the sentinel was taken
   */
  private WatchKey checkSentinel (EphemeralWatchKey watchKey) {

    if (watchKey == CLOSE_SENTINEL) {
      // put it back so that any other blocked thread also wakes
      watchKeyQueue.add(CLOSE_SENTINEL);

      throw new ClosedWatchServiceException();
    }

    return watchKey;
  }

  @Override
  public WatchKey poll () {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {

      EphemeralWatchKey watchKey;

      return ((watchKey = watchKeyQueue.poll()) == null) ? null : checkSentinel(watchKey);
    }
  }

  @Override
  public WatchKey poll (long timeout, TimeUnit unit)
    throws InterruptedException {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {

      EphemeralWatchKey watchKey;

      return ((watchKey = watchKeyQueue.poll(timeout, unit)) == null) ? null : checkSentinel(watchKey);
    }
  }

  @Override
  public WatchKey take ()
    throws InterruptedException {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {

      return checkSentinel(watchKeyQueue.take());
    }
  }
}
