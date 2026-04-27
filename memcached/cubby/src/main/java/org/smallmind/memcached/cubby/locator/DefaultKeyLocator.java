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
 * A {@link KeyLocator} implementation that distributes keys across active hosts using a
 * simple modulo-hash strategy.
 *
 * <p>Active host names are sorted alphabetically and stored in an array. A key is routed to
 * the host at position {@code key.hashCode() % routingArray.length}, providing an even
 * distribution across the active pool. Unlike consistent-hash strategies, this approach does
 * not minimise remapping when the host set changes: adding or removing a host will re-route
 * approximately all keys.</p>
 *
 * <p>Read and write access to the routing array and current host list is protected by a
 * {@link ReentrantReadWriteLock} so that concurrent {@link #find(ServerPool, String)} calls
 * from multiple client threads are safe.</p>
 */
public class DefaultKeyLocator implements KeyLocator {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private LinkedList<MemcachedHost> currentHostList;
  private String[] routingArray;

  /**
   * Builds a sorted array of active host names from the current server pool, also updating
   * the cached host list used for change detection.
   *
   * <p>If there are no active hosts an empty array is returned and subsequent
   * {@link #find(ServerPool, String)} calls will throw {@link NoAvailableHostException}.</p>
   *
   * @param serverPool the pool from which active hosts are collected
   * @return a sorted array of active host names, or an empty array if no host is active
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
   * Performs one-time initialisation of the routing data structures for the given server pool.
   *
   * <p>This method is called once by the client after all hosts have been registered in the
   * pool. Implementations may pre-compute expensive structures (e.g. hash permutation tables)
   * that need only be built once per pool configuration.</p>
   *
   * <p>Delegates to {@link #updateRouting(ServerPool)} to perform the initial build of the
   * routing array.</p>
   *
   * @param serverPool the pool that supplies the initial set of candidate hosts
   */
  @Override
  public void installRouting (ServerPool serverPool) {

    updateRouting(serverPool);
  }

  /**
   * Refreshes the routing structures to reflect the current availability of hosts in the pool.
   *
   * <p>This method is called whenever a host transitions between active and inactive states.
   * Implementations should check whether the active host set has actually changed before
   * rebuilding their routing tables to avoid unnecessary work.</p>
   *
   * <p>Acquires the write lock and rebuilds the routing array only if the active host set has
   * changed since the last build. Change detection is performed by comparing the current pool
   * state against the cached {@code currentHostList}.</p>
   *
   * @param serverPool the pool whose current active host set should be reflected in the
   *                   routing array
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
   * Resolves the {@link MemcachedHost} that should handle the request for the given key.
   *
   * <p>This method is called on every cache operation and must be efficient. Implementations
   * should use a read lock or equivalent mechanism to allow concurrent lookups.</p>
   *
   * <p>Acquires the read lock and returns the host at index
   * {@code key.hashCode() % routingArray.length}. Throws {@link NoAvailableHostException} if
   * the routing array is empty or has not been initialised, meaning no active host exists.</p>
   *
   * @param serverPool the pool used to resolve the winning host name to a {@link MemcachedHost}
   * @param key        the cache key to route
   * @return the active {@link MemcachedHost} responsible for this key
   * @throws IOException if no active host is available ({@link NoAvailableHostException})
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
