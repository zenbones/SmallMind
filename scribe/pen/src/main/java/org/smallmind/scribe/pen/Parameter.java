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
package org.smallmind.scribe.pen;

import java.io.Serializable;

/**
 * Immutable, serializable key/value pair that attaches structured contextual data to a {@link Record}, enabling
 * appenders and formatters to include application-specific metadata alongside the log message.
 */
public class Parameter implements Serializable {

  private final String key;
  private final Serializable value;

  /**
   * Constructs an immutable parameter with the given key and value.
   *
   * @param key   the name that identifies this parameter; must not be {@code null}
   * @param value the serializable value to associate with the key; may be {@code null}
   */
  public Parameter (String key, Serializable value) {

    this.key = key;
    this.value = value;
  }

  /**
   * Returns the name that identifies this parameter.
   *
   * @return the parameter key; never {@code null}
   */
  public String getKey () {

    return key;
  }

  /**
   * Returns the value associated with this parameter.
   *
   * @return the parameter value; may be {@code null}
   */
  public Serializable getValue () {

    return value;
  }
}
