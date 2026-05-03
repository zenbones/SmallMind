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
package org.smallmind.memcached.cubby;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * An indexed collection of {@link HostControl} records, one per memcached node in the cluster.
 *
 * <p>{@code ServerPool} is the central registry consulted by
 * {@link org.smallmind.memcached.cubby.locator.KeyLocator} implementations when building or
 * refreshing consistent-hashing rings. It is also iterated by the {@link ServerDefibrillator} to
 * find inactive hosts that require a reconnection attempt.</p>
 *
 * <p>Hosts are keyed by their {@linkplain MemcachedHost#getName() logical name}, which serves as
 * the stable identifier across connection lifecycle events.</p>
 */
public class ServerPool {

  private final HashMap<String, HostControl> hostMap = new HashMap<>();

  /**
   * Constructs a pool pre-populated with a {@link HostControl} for each supplied host.
   * Every host starts in the active state.
   *
   * @param memcachedHosts the hosts that constitute the memcached cluster
   */
  public ServerPool (MemcachedHost... memcachedHosts) {

    for (MemcachedHost memcachedHost : memcachedHosts) {
      hostMap.put(memcachedHost.getName(), new HostControl(memcachedHost));
    }
  }

  /**
   * Returns the total number of hosts registered in the pool, regardless of their health state.
   *
   * @return the number of hosts in the pool
   */
  public int size () {

    return hostMap.size();
  }

  /**
   * Retrieves the {@link HostControl} record for the named host.
   *
   * @param name the logical name of the host to look up
   * @return the corresponding {@link HostControl}, or {@code null} if the name is not registered
   */
  public HostControl get (String name) {

    return hostMap.get(name);
  }

  /**
   * Returns the set of logical host names registered in this pool.
   *
   * @return an unmodifiable view of the host name keys
   */
  public Set<String> keySet () {

    return hostMap.keySet();
  }

  /**
   * Returns the collection of all {@link HostControl} records in the pool.
   *
   * @return the control records for all registered hosts
   */
  public Collection<HostControl> values () {

    return hostMap.values();
  }

  /**
   * Tests whether this pool represents exactly the same set of active hosts as the given list.
   *
   * <p>Returns {@code true} only when:
   * <ul>
   *   <li>the pool and the list contain the same number of entries, and</li>
   *   <li>every host in the list has a corresponding {@link HostControl} in the pool that is
   *       currently {@linkplain HostControl#isActive() active}.</li>
   * </ul>
   * This is used by the {@link org.smallmind.memcached.cubby.locator.KeyLocator} to detect
   * whether the active topology has changed and the routing table requires rebuilding.
   *
   * @param hostList the candidate host list to compare against the current pool state
   * @return {@code true} if the pool exactly represents the supplied list of active hosts
   */
  public boolean representsHosts (List<MemcachedHost> hostList) {

    if (hostMap.size() != hostList.size()) {

      return false;
    } else {
      for (MemcachedHost memcachedHost : hostList) {

        HostControl hostControl;

        if (((hostControl = hostMap.get(memcachedHost.getName())) == null) || (!hostControl.isActive())) {

          return false;
        }
      }

      return true;
    }
  }
}
