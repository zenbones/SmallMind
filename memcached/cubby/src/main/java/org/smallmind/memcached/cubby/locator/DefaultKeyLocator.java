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
package org.smallmind.memcached.cubby.locator;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.memcached.cubby.HostControl;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;

/**
 * Simple locator that hashes keys and evenly distributes them across active hosts.
 */
public class DefaultKeyLocator implements KeyLocator {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private LinkedList<MemcachedHost> currentHostList;
  private String[] routingArray;

  /**
   * Builds a sorted routing table of active host names.
   *
   * @param serverPool pool of hosts to route across
   * @return ordered host name array
   */
  private String[] generateRoutingArray (ServerPool serverPool) {

    LinkedList<String> activeNameList = new LinkedList<>();

    currentHostList = new LinkedList<>();
    for (HostControl hostControl : serverPool.values()) {
      if (hostControl.isActive()) {
        currentHostList.add(hostControl.getMemcachedHost());
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

  /**
   * Initializes the routing information for the pool.
   *
   * @param serverPool pool that supplies host entries
   */
  @Override
  public void installRouting (ServerPool serverPool) {

    updateRouting(serverPool);
  }

  /**
   * Rebuilds the routing table if the underlying host list has changed.
   *
   * <p>Uses a write lock to ensure the routing array and host list are updated atomically.</p>
   *
   * @param serverPool pool that may have updated host state
   */
  @Override
  public void updateRouting (ServerPool serverPool) {

    lock.writeLock().lock();
    try {
      if ((currentHostList == null) || (!serverPool.representsHosts(currentHostList))) {
        routingArray = generateRoutingArray(serverPool);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Resolves the host responsible for the supplied cache key.
   *
   * <p>The routing array is read under a shared lock and the key hash is used to choose an active host.</p>
   *
   * @param serverPool pool that provides host lookups by name
   * @param key        cache key to route
   * @return active {@link MemcachedHost} that should service the key
   * @throws IOException if no active host is available
   */
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
