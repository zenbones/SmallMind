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
package org.smallmind.scribe.pen.adapter;

import java.io.Serializable;
import org.smallmind.scribe.pen.Parameter;

/**
 * MDC-like interface for associating serializable key/value parameters with the current logging context;
 * values stored here are attached to every log record emitted while they remain in the adapter.
 */
public interface ParameterAdapter {

  /**
   * Stores or replaces the value associated with {@code key} in the current context.
   *
   * @param key   the parameter key; must not be {@code null}
   * @param value the serializable value to associate with the key
   */
  void put (String key, Serializable value);

  /**
   * Removes the parameter identified by {@code key} from the current context; has no effect if the key
   * is not present.
   *
   * @param key the key of the parameter to remove
   */
  void remove (String key);

  /**
   * Removes all parameters from the current context.
   */
  void clear ();

  /**
   * Returns the value associated with {@code key} in the current context.
   *
   * @param key the key to look up
   * @return the associated value, or {@code null} if no mapping exists for the key
   */
  Serializable get (String key);

  /**
   * Returns a snapshot of all parameters currently stored in this adapter as an array of
   * {@link Parameter} objects.
   *
   * @return an array of all current parameters; never {@code null} but may be empty
   */
  Parameter[] getParameters ();
}
