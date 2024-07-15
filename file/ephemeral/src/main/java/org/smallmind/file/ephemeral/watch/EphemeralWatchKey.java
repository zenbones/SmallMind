/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class EphemeralWatchKey implements WatchKey {

  private final EphemeralWatchService watchService;
  private final WatchEvent.Kind<?>[] events;
  private final EphemeralPath path;
  private final LinkedBlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>();
  private boolean valid = true;
  private boolean signalled = false;

  public EphemeralWatchKey (EphemeralWatchService watchService, WatchEvent.Kind<?>[] events, EphemeralPath path) {

    this.watchService = watchService;
    this.events = events;
    this.path = path;
  }

  public EphemeralPath getPath () {

    return path;
  }

  @Override
  public synchronized boolean isValid () {

    return valid && (!watchService.isClosed());
  }

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

  @Override
  public List<WatchEvent<?>> pollEvents () {

    LinkedList<WatchEvent<?>> eventList = new LinkedList<>();
    WatchEvent<?> event;

    while ((event = eventQueue.poll()) != null) {
      eventList.add(event);
    }

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

  @Override
  public Watchable watchable () {

    return path;
  }
}
