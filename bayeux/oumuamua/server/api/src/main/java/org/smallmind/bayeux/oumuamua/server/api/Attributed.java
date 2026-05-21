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
package org.smallmind.bayeux.oumuamua.server.api;

import java.util.Set;

/**
 * Key-value attribute store attached to server-side objects such as channels and sessions.
 */
public interface Attributed {

  /**
   * Returns the names of all currently stored attributes.
   *
   * @return set of attribute names; never {@code null}, possibly empty
   */
  Set<String> getAttributeNames ();

  /**
   * Returns the value associated with the given name.
   *
   * @param name attribute key to look up
   * @return stored value, or {@code null} if no attribute with that name exists
   */
  Object getAttribute (String name);

  /**
   * Stores a value under the given name, replacing any existing value.
   *
   * @param name  attribute key
   * @param value value to store; must be non-null
   */
  void setAttribute (String name, Object value);

  /**
   * Removes the attribute with the given name and returns its previous value.
   *
   * @param name attribute key to remove
   * @return the value that was removed, or {@code null} if no such attribute existed
   */
  Object removeAttribute (String name);
}
