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
package org.smallmind.memcached.cubby.command;

/**
 * Enumerates the mutation variants available for the memcached meta-set
 * ({@code ms}) command, corresponding to the {@code M} flag values defined
 * by the memcached meta-protocol.
 *
 * <ul>
 *   <li>{@link #ADD} — store only if the key does not already exist ({@code E}).</li>
 *   <li>{@link #APPEND} — append the value to the end of an existing item ({@code A}).</li>
 *   <li>{@link #PREPEND} — prepend the value to the beginning of an existing item ({@code P}).</li>
 *   <li>{@link #REPLACE} — store only if the key already exists ({@code R}).</li>
 *   <li>{@link #SET} — unconditionally store the value ({@code S}).</li>
 * </ul>
 *
 * <p>Each constant carries the single-character protocol token that is embedded
 * in the {@code M} flag of the {@code ms} command line by
 * {@link SetCommand#construct(org.smallmind.memcached.cubby.translator.KeyTranslator)}.</p>
 */
public enum SetMode {

  /**
   * Store the value only if the key does not already exist. Protocol token: {@code E}.
   */
  ADD('E'),

  /**
   * Append the value to the tail of an existing item. Protocol token: {@code A}.
   */
  APPEND('A'),

  /**
   * Prepend the value to the head of an existing item. Protocol token: {@code P}.
   */
  PREPEND('P'),

  /**
   * Store the value only if the key already exists. Protocol token: {@code R}.
   */
  REPLACE('R'),

  /**
   * Unconditionally store the value. Protocol token: {@code S}.
   */
  SET('S');

  private final char token;

  /**
   * Constructs a {@code SetMode} constant with its protocol token.
   *
   * @param token the single character used in the {@code ms} command line
   *              to indicate the desired mutation variant
   */
  SetMode (char token) {

    this.token = token;
  }

  /**
   * Returns the single-character protocol token embedded in the {@code M} flag
   * of the memcached meta-set command line.
   *
   * @return the protocol token representing this set mode
   */
  public char getToken () {

    return token;
  }
}
