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
package org.smallmind.memcached.cubby.locator;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.memcached.cubby.HostControl;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;

public class DefaultKeyLocator implements KeyLocator {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private String[] routingArray;

  private String[] generateRoutingArray (ServerPool serverPool) {

    LinkedList<String> activeNameList = new LinkedList<>();

    for (HostControl hostControl : serverPool.values()) {
      if (hostControl.isActive()) {
        activeNameList.add(hostControl.getMemcachedHost().getName());
      }
    }

    if (activeNameList.isEmpty()) {

      return new String[0];
    } else {

      String[] activeNames;

      Collections.sort(activeNameList);
      activeNames = activeNameList.toArray(new String[0]);

      return activeNames;
    }
  }

  @Override
  public void installRouting (ServerPool serverPool) {

    updateRouting(serverPool);
  }

  @Override
  public void updateRouting (ServerPool serverPool) {

    lock.writeLock().lock();
    try {
      routingArray = generateRoutingArray(serverPool);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public MemcachedHost find (ServerPool serverPool, String key)
    throws IOException {

    lock.readLock().lock();
    try {
      if ((routingArray == null) || (routingArray.length == 0)) {
        throw new NoAvailableHostException();
      } else {

        return serverPool.get(routingArray[key.hashCode() % routingArray.length]).getMemcachedHost();
      }
    } finally {
      lock.readLock().unlock();
    }
  }
}
