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
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.ServerPool;

/**
 * Strategy interface for determining which memcached host should service a given cache key.
 *
 * <p>Implementations are responsible for maintaining whatever internal routing structures
 * (hash rings, lookup tables, etc.) are required to efficiently and consistently map keys to
 * hosts. The lifecycle mirrors the lifecycle of the client: {@link #installRouting(ServerPool)}
 * is called once when the client starts, and {@link #updateRouting(ServerPool)} is called
 * whenever host availability changes (e.g. after a host disconnects or reconnects).
 * Per-request routing is performed by {@link #find(ServerPool, String)}.</p>
 *
 * <p>Two built-in implementations are provided:
 * <ul>
 *   <li>{@link DefaultKeyLocator} — a simple modulo-hash strategy.</li>
 *   <li>{@link MaglevKeyLocator} — a consistent Maglev hash that minimises key remapping
 *       when the host set changes.</li>
 * </ul>
 */
public interface KeyLocator {

  /**
   * Performs one-time initialisation of the routing data structures for the given server pool.
   *
   * <p>This method is called once by the client after all hosts have been registered in the
   * pool. Implementations may pre-compute expensive structures (e.g. hash permutation tables)
   * that need only be built once per pool configuration.</p>
   *
   * @param serverPool the fully populated pool of candidate hosts
   * @throws CubbyOperationException if the routing structures cannot be built, for example
   *                                 because a required cryptographic algorithm is unavailable
   */
  void installRouting (ServerPool serverPool)
    throws CubbyOperationException;

  /**
   * Refreshes the routing structures to reflect the current availability of hosts in the pool.
   *
   * <p>This method is called whenever a host transitions between active and inactive states.
   * Implementations should check whether the active host set has actually changed before
   * rebuilding their routing tables to avoid unnecessary work.</p>
   *
   * @param serverPool the pool whose current host availability should be reflected in routing
   */
  void updateRouting (ServerPool serverPool);

  /**
   * Resolves the {@link MemcachedHost} that should handle the request for the given key.
   *
   * <p>This method is called on every cache operation and must be efficient. Implementations
   * should use a read lock or equivalent mechanism to allow concurrent lookups.</p>
   *
   * @param serverPool the pool used to look up host metadata by name after the routing
   *                   decision has been made
   * @param key        the normalized, translated cache key whose target host is to be found
   * @return the {@link MemcachedHost} that is currently responsible for the given key
   * @throws IOException             if no active host is available to service the key (e.g.
   *                                 all hosts are offline)
   * @throws CubbyOperationException if the routing lookup cannot be performed due to an
   *                                 internal state error
   */
  MemcachedHost find (ServerPool serverPool, String key)
    throws IOException, CubbyOperationException;
}
