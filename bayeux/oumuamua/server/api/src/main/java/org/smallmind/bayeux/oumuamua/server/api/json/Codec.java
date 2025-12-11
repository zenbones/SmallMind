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
package org.smallmind.bayeux.oumuamua.server.api.json;

import java.io.IOException;

/**
 * Encodes and decodes Bayeux messages to and from transport representations.
 *
 * @param <V> concrete value subtype used within messages
 */
public interface Codec<V extends Value<V>> {

  /**
   * Creates an empty message bound to this codec's factory.
   *
   * @return new message instance
   */
  Message<V> create ();

  /**
   * Parses messages from a byte buffer.
   *
   * @param buffer encoded payload
   * @return decoded messages
   * @throws IOException if parsing fails
   */
  Message<V>[] from (byte[] buffer)
    throws IOException;

  /**
   * Parses messages from a string.
   *
   * @param data encoded payload
   * @return decoded messages
   * @throws IOException if parsing fails
   */
  Message<V>[] from (String data)
    throws IOException;

  /**
   * Converts an arbitrary object into a {@link Value} representation.
   *
   * @param object object to convert
   * @return converted value
   * @throws IOException if conversion fails
   */
  Value<V> convert (Object object)
    throws IOException;
}
