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
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.smallmind.file.ephemeral.EphemeralPath;

/**
 * {@link WatchKey} handed out by an {@link EphemeralWatchService} for one watched directory.
 *
 * <p>The queue of pending events is bounded. Once it is full an {@link StandardWatchEventKinds#OVERFLOW}
 * event is recorded in place of the events that could not be kept, which is how a platform watch
 * service signals that a consumer has fallen too far behind to be given a complete history.
 */
public class EphemeralWatchKey implements WatchKey {

  /**
   * The number of pending events a key will hold before reporting an overflow.
   */
  private static final int MAXIMUM_PENDING_EVENTS = 512;

  private final EphemeralWatchService watchService;
  private final EphemeralPath path;
  private final LinkedBlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>(MAXIMUM_PENDING_EVENTS);
  private WatchEvent.Kind<?>[] events;
  private boolean overflowed = false;
  private boolean valid = true;
  private boolean signalled = false;

  /**
   * Creates a key for a watched directory.
   *
   * @param watchService the service that issued this key
   * @param events       the kinds of event this key reports
   * @param path         the absolute path of the watched directory
   */
  public EphemeralWatchKey (EphemeralWatchService watchService, WatchEvent.Kind<?>[] events, EphemeralPath path) {

    this.watchService = watchService;
    this.events = events;
    this.path = path;
  }

  /**
   * Narrows an event kind to the context type this key produces.
   *
   * @param kind the kind to narrow
   * @return the same kind, typed for an {@link EphemeralPath} context
   */
  @SuppressWarnings("unchecked")
  private static WatchEvent.Kind<EphemeralPath> castKind (WatchEvent.Kind<?> kind) {

    return (WatchEvent.Kind<EphemeralPath>)kind;
  }

  /**
   * Returns the absolute path of the watched directory.
   *
   * @return the watched path
   */
  public EphemeralPath getPath () {

    return path;
  }

  /**
   * Replaces the kinds of event this key reports, as a repeated registration does.
   *
   * @param events the kinds of event to report from now on
   */
  synchronized void setEvents (WatchEvent.Kind<?>[] events) {

    this.events = events;
  }

  @Override
  public synchronized boolean isValid () {

    return valid && (!watchService.isClosed());
  }

  /**
   * Records an event against this key if its kind was registered for.
   *
   * @param firedEvent the kind of change observed
   * @param context    the path of the changed entry, relative to the watched directory
   * @return {@code true} if this key should be queued for collection, which happens only on the
   * first event after each {@link #reset()}
   */
  public synchronized boolean fire (WatchEvent.Kind<?> firedEvent, EphemeralPath context) {

    if (valid) {
      for (WatchEvent.Kind<?> event : events) {
        if (event.name().equals(firedEvent.name())) {

          enqueue(new EphemeralWatchEvent<>(castKind(firedEvent), 1, context));

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
   * Adds an event, degrading to a single overflow event once the queue is full.
   *
   * @param watchEvent the event to record
   */
  private void enqueue (WatchEvent<EphemeralPath> watchEvent) {

    if (!eventQueue.offer(watchEvent)) {
      if (!overflowed) {
        overflowed = true;
        // the queue is full, so the one slot that matters is a marker saying events were lost
        eventQueue.poll();
        eventQueue.offer(new EphemeralWatchEvent<>(castKind(StandardWatchEventKinds.OVERFLOW), 1, null));
      }
    }
  }

  @Override
  public synchronized List<WatchEvent<?>> pollEvents () {

    LinkedList<WatchEvent<?>> eventList = new LinkedList<>();
    WatchEvent<?> event;

    while ((event = eventQueue.poll()) != null) {
      eventList.add(event);
    }
    overflowed = false;

    return eventList;
  }

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

  @Override
  public synchronized void cancel () {

    cancel(true);
  }

  /**
   * Invalidates this key, optionally removing its registration from the service.
   *
   * @param deregister {@code false} when the service is already discarding its registrations, as
   *                   it does while closing
   */
  public synchronized void cancel (boolean deregister) {

    valid = false;

    if (deregister) {
      try {
        watchService.unregister(this);
      } catch (IOException ioException) {
        // the directory is already gone, which is the usual reason for cancelling
      }
    }
  }

  @Override
  public Watchable watchable () {

    return path;
  }
}
