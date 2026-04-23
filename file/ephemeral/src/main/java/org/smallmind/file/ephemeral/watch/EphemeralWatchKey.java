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

import java.nio.file.NoSuchFileException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.smallmind.file.ephemeral.EphemeralPath;

/**
 * {@link WatchKey} implementation that buffers {@link WatchEvent} instances for a single
 * ephemeral file-system path.
 *
 * <p>A key is created by registering an {@link EphemeralPath} with an
 * {@link EphemeralWatchService}. It maintains an internal queue of pending events and
 * tracks whether it has been signalled (i.e. added to the service's ready queue). Once
 * cancelled, the key becomes permanently invalid and is removed from the service.
 *
 * <p>All state-mutating methods are {@code synchronized} on the key instance to allow safe
 * concurrent access from both the thread that fires events and the thread that consumes them.
 */
public class EphemeralWatchKey implements WatchKey {

  /**
   * The watch service that owns and manages this key.
   */
  private final EphemeralWatchService watchService;

  /**
   * The set of event kinds that this key is subscribed to.
   */
  private final WatchEvent.Kind<?>[] events;

  /**
   * The ephemeral path being monitored by this key.
   */
  private final EphemeralPath path;

  /**
   * Thread-safe queue of pending {@link WatchEvent} instances that have not yet been
   * consumed by the client via {@link #pollEvents()}.
   */
  private final LinkedBlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>();

  /**
   * Whether this key is still valid. Set to {@code false} permanently when
   * {@link #cancel(boolean)} is called.
   */
  private boolean valid = true;

  /**
   * Whether this key is currently in the service's ready queue, waiting to be returned
   * by a {@code poll} or {@code take} call. Reset to {@code false} by {@link #reset()}
   * when the event queue is drained.
   */
  private boolean signalled = false;

  /**
   * Creates a watch key that monitors the given path for the specified event kinds.
   *
   * @param watchService the {@link EphemeralWatchService} that manages this key;
   *                     must not be {@code null}
   * @param events       the array of {@link WatchEvent.Kind} values that this key should
   *                     react to; must not be {@code null} or empty
   * @param path         the {@link EphemeralPath} to be monitored; must not be {@code null}
   */
  public EphemeralWatchKey (EphemeralWatchService watchService, WatchEvent.Kind<?>[] events, EphemeralPath path) {

    this.watchService = watchService;
    this.events = events;
    this.path = path;
  }

  /**
   * Returns the ephemeral path associated with this watch key.
   *
   * @return the {@link EphemeralPath} being monitored; never {@code null}
   */
  public EphemeralPath getPath () {

    return path;
  }

  /**
   * Returns whether this key is currently valid. A key is valid from the time it is
   * created until it is cancelled (via {@link #cancel()}) or the owning
   * {@link EphemeralWatchService} is closed.
   *
   * @return {@code true} if this key has not been cancelled and the owning service is
   * still open; {@code false} otherwise
   */
  @Override
  public synchronized boolean isValid () {

    return valid && (!watchService.isClosed());
  }

  /**
   * Attempts to enqueue a fired event if it matches one of the subscribed event kinds.
   *
   * <p>When the key is valid and the fired event kind matches a subscribed kind (by both
   * class identity and name), a new {@link EphemeralWatchEvent} is added to the internal
   * queue. If the key has not yet been signalled, it is marked as signalled and this
   * method returns {@code true} to indicate that the service should add the key to its
   * ready queue. Subsequent firings while the key is already signalled return {@code false}.
   *
   * <p>If the key is invalid, or the event kind does not match any subscription, this
   * method returns {@code false} without modifying any state.
   *
   * @param firedEvent the {@link WatchEvent.Kind} of the change that occurred;
   *                   must not be {@code null}
   * @return {@code true} if the key was just transitioned to the signalled state and
   * the calling service should enqueue it in its ready queue;
   * {@code false} if the key was already signalled, is invalid, or the event
   * kind is not subscribed
   */
  public synchronized boolean fire (WatchEvent.Kind<?> firedEvent) {

    if (valid) {
      for (WatchEvent.Kind<?> event : events) {
        if (event.getClass().equals(firedEvent.getClass()) && event.name().equals(firedEvent.name())) {
          eventQueue.add(new EphemeralWatchEvent<>(firedEvent, 1, null));

          if (!signalled) {
            signalled = true;

            return true;
          } else {

            return false;
          }
        }
      }
    }

    return false;
  }

  /**
   * Retrieves and removes all pending events that have accumulated in this key's queue
   * since it was last reset.
   *
   * <p>This method drains the internal queue atomically, returning all available events
   * as an ordered list. If the queue is empty, an empty list is returned. The caller is
   * responsible for calling {@link #reset()} after processing the events to allow the
   * key to be signalled again.
   *
   * @return a non-{@code null}, possibly empty {@link List} of all pending
   * {@link WatchEvent} instances in the order they were enqueued
   */
  @Override
  public List<WatchEvent<?>> pollEvents () {

    LinkedList<WatchEvent<?>> eventList = new LinkedList<>();
    WatchEvent<?> event;

    while ((event = eventQueue.poll()) != null) {
      eventList.add(event);
    }

    return eventList;
  }

  /**
   * Resets this key so that it can be signalled again for future events.
   *
   * <p>If the key is still valid and its event queue is non-empty (new events arrived
   * while the key was being processed), the key is immediately re-enqueued in the
   * service's ready queue via {@link EphemeralWatchService#requeue(EphemeralWatchKey)}.
   * Otherwise the signalled flag is cleared so the next fired event will trigger a fresh
   * enqueue.
   *
   * <p>If the key is no longer valid (cancelled or the service is closed), this method
   * returns {@code false} without modifying any state.
   *
   * @return {@code true} if the key is valid and has been successfully reset;
   * {@code false} if the key is no longer valid
   */
  @Override
  public synchronized boolean reset () {

    if (isValid()) {

      if (!eventQueue.isEmpty()) {
        watchService.requeue(this);
      } else {
        signalled = false;
      }

      return true;
    } else {

      return false;
    }
  }

  /**
   * Cancels this watch key and deregisters it from the owning {@link EphemeralWatchService}.
   *
   * <p>Once cancelled, the key becomes permanently invalid and will never be signalled
   * again. This is equivalent to calling {@link #cancel(boolean) cancel(true)}.
   *
   * <p>If the key has already been cancelled, this method has no effect.
   */
  @Override
  public synchronized void cancel () {

    cancel(true);
  }

  /**
   * Cancels this watch key, optionally deregistering it from the owning service.
   *
   * <p>Setting {@code valid} to {@code false} permanently prevents this key from
   * receiving further events. When {@code deregister} is {@code true}, the key is also
   * removed from the service's path-to-key map via
   * {@link EphemeralWatchService#unregister(EphemeralWatchKey)}. Passing {@code false}
   * is used internally by {@link EphemeralWatchService#close()} to avoid re-entrant
   * modification of the map while iterating over it.
   *
   * <p>Any {@link NoSuchFileException} thrown during deregistration is silently ignored,
   * because the path may have already been removed from the file system.
   *
   * @param deregister {@code true} to also remove this key from the watch service's
   *                   internal registry; {@code false} to mark it invalid without
   *                   touching the registry
   */
  public synchronized void cancel (boolean deregister) {

    valid = false;

    if (deregister) {
      try {
        watchService.unregister(this);
      } catch (NoSuchFileException noSuchFileException) {
        // nothing to do here
      }
    }
  }

  /**
   * Returns the {@link Watchable} (the monitored path) associated with this key.
   *
   * <p>The returned object is the same {@link EphemeralPath} that was supplied when the
   * key was created, and can be used to identify which path this key is watching
   * regardless of the key's current validity.
   *
   * @return the {@link EphemeralPath} that this key is (or was) monitoring;
   * never {@code null}
   */
  @Override
  public Watchable watchable () {

    return path;
  }
}
