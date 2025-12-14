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
 * Strategy for mapping keys to memcached hosts.
 */
public interface KeyLocator {

  /**
   * Initializes routing state for the provided server pool.
   *
   * @param serverPool pool of available hosts
   * @throws CubbyOperationException if routing cannot be installed
   */
  void installRouting (ServerPool serverPool)
    throws CubbyOperationException;

  /**
   * Updates routing after host availability changes.
   *
   * @param serverPool pool of available hosts
   */
  void updateRouting (ServerPool serverPool);

  /**
   * Finds the host responsible for the given key.
   *
   * @param serverPool pool of available hosts
   * @param key        normalized cache key
   * @return host assigned to the key
   * @throws IOException             if routing requires I/O and fails
   * @throws CubbyOperationException if routing cannot be determined
   */
  MemcachedHost find (ServerPool serverPool, String key)
    throws IOException, CubbyOperationException;
}
