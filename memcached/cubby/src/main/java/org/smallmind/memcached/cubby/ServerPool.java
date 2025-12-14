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
 * Maintains a mapping of memcached hosts to their control metadata and supports quick lookups.
 */
public class ServerPool {

  private final HashMap<String, HostControl> hostMap = new HashMap<>();

  /**
   * Builds a pool from the supplied host list.
   *
   * @param memcachedHosts hosts to track
   */
  public ServerPool (MemcachedHost... memcachedHosts) {

    for (MemcachedHost memcachedHost : memcachedHosts) {
      hostMap.put(memcachedHost.getName(), new HostControl(memcachedHost));
    }
  }

  /**
   * @return number of hosts in the pool
   */
  public int size () {

    return hostMap.size();
  }

  /**
   * Retrieves the control object for the named host.
   *
   * @param name host name
   * @return control metadata or {@code null} if missing
   */
  public HostControl get (String name) {

    return hostMap.get(name);
  }

  /**
   * @return set of host names in the pool
   */
  public Set<String> keySet () {

    return hostMap.keySet();
  }

  /**
   * @return collection of control records
   */
  public Collection<HostControl> values () {

    return hostMap.values();
  }

  /**
   * Tests whether the pool exactly matches the supplied list of active hosts.
   *
   * @param hostList hosts to compare against
   * @return {@code true} if the pool and list represent the same active hosts
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
