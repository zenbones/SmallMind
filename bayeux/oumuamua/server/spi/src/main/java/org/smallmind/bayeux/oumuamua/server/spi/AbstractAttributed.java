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
package org.smallmind.bayeux.oumuamua.server.spi;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.bayeux.oumuamua.server.api.Attributed;

/**
 * {@link Attributed} base implementation backed by a {@link ConcurrentHashMap},
 * providing thread-safe attribute storage for Bayeux sessions and channels.
 */
public class AbstractAttributed implements Attributed {

  private final ConcurrentHashMap<String, Object> attributeMap = new ConcurrentHashMap<>();

  /**
   * Returns an unmodifiable snapshot of all currently stored attribute names.
   *
   * @return unmodifiable {@link Set} containing the attribute names at the time of the call
   */
  @Override
  public Set<String> getAttributeNames () {

    return Set.copyOf(attributeMap.keySet());
  }

  /**
   * Retrieves the value bound to the given name.
   *
   * @param name key of the attribute to retrieve
   * @return the associated value, or {@code null} if no mapping exists
   */
  @Override
  public Object getAttribute (String name) {

    return attributeMap.get(name);
  }

  /**
   * Binds a value to the given name, replacing any existing mapping.
   *
   * @param name  key under which the value is stored
   * @param value value to associate with the name
   */
  @Override
  public void setAttribute (String name, Object value) {

    attributeMap.put(name, value);
  }

  /**
   * Removes the attribute bound to the given name.
   *
   * @param name key of the attribute to remove
   * @return the value that was associated with the name, or {@code null} if none existed
   */
  @Override
  public Object removeAttribute (String name) {

    return attributeMap.remove(name);
  }
}
