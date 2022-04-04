/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class EphemeralWatchService implements WatchService {

  private final EphemeralFileStore ephemeralFileStore;
  private final HeapEventListener heapEventListener;
  private final HashMap<EphemeralPath, LinkedList<EphemeralWatchKey>> watchKeyMap = new HashMap<>();
  private final LinkedBlockingQueue<EphemeralWatchKey> watchKeyQueue = new LinkedBlockingQueue<>();
  private volatile boolean closed = false;

  public EphemeralWatchService (EphemeralFileStore ephemeralFileStore) {

    this.ephemeralFileStore = ephemeralFileStore;

    heapEventListener = new EphemeralHeapEventListener(this);
  }

  public synchronized boolean isClosed () {

    return closed;
  }

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

  public synchronized void register (EphemeralWatchKey ephemeralWatchKey)
    throws NotDirectoryException {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {

      LinkedList<EphemeralWatchKey> watchKeyList;

      if ((watchKeyList = watchKeyMap.get(ephemeralWatchKey.getPath())) == null) {
        watchKeyMap.put(ephemeralWatchKey.getPath(), watchKeyList = new LinkedList<>());
        ephemeralFileStore.registerHeapListener(ephemeralWatchKey.getPath(), heapEventListener);
      }

      watchKeyList.add(ephemeralWatchKey);
    }
  }

  public synchronized void unregister (EphemeralWatchKey ephemeralWatchKey) {

    LinkedList<EphemeralWatchKey> watchKeyList;

    if ((watchKeyList = watchKeyMap.get(ephemeralWatchKey.getPath())) != null) {
      if (watchKeyList.remove(ephemeralWatchKey)) {
        if (watchKeyList.isEmpty()) {
          watchKeyMap.remove(ephemeralWatchKey.getPath());
          ephemeralFileStore.unregisterHeapListener(ephemeralWatchKey.getPath(), heapEventListener);
        }

        watchKeyQueue.remove(ephemeralWatchKey);
      }
    }
  }

  public synchronized void fire (EphemeralPath path, WatchEvent.Kind<?> event) {

    if (!closed) {

      LinkedList<EphemeralWatchKey> watchKeyList;

      if ((watchKeyList = watchKeyMap.get(path)) != null) {
        for (EphemeralWatchKey watchKey : watchKeyList) {
          if (watchKey.fire(event)) {
            watchKeyQueue.add(watchKey);
          }
        }
      }
    }
  }

  public synchronized void requeue (EphemeralWatchKey watchKey) {

    if (!closed) {
      watchKeyQueue.add(watchKey);
    }
  }

  @Override
  public WatchKey poll () {

    if (closed) {
      throw new ClosedWatchServiceException();
    } else {
      return watchKeyQueue.poll();
    }
  }

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
