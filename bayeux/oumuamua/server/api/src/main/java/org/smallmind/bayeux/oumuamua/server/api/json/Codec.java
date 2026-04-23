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
 * Serialization gateway that converts between Bayeux {@link Message} objects and their JSON
 * wire representations, and that bridges arbitrary Java objects into the {@link Value} type hierarchy.
 *
 * @param <V> concrete value subtype used within messages produced by this codec
 */
public interface Codec<V extends Value<V>> {

  /**
   * Allocates a new, empty message whose value factory is bound to this codec.
   *
   * @return blank message ready for field population
   */
  Message<V> create ();

  /**
   * Deserializes a JSON byte array into one or more Bayeux messages.
   *
   * @param buffer UTF-8 encoded JSON payload, containing either a single object or an array of objects
   * @return array of decoded messages; never {@code null}, never empty on success
   * @throws IOException if the payload is malformed or I/O fails during reading
   */
  Message<V>[] from (byte[] buffer)
    throws IOException;

  /**
   * Deserializes a JSON string into one or more Bayeux messages.
   *
   * @param data JSON string containing either a single object or an array of objects
   * @return array of decoded messages; never {@code null}, never empty on success
   * @throws IOException if the string is malformed JSON
   */
  Message<V>[] from (String data)
    throws IOException;

  /**
   * Converts an arbitrary Java object to its {@link Value} equivalent using the codec's
   * underlying serialization strategy.
   *
   * @param object object to convert; may be a primitive wrapper, collection, or bean
   * @return value representation of the object
   * @throws IOException if the object cannot be represented as a {@link Value}
   */
  Value<V> convert (Object object)
    throws IOException;
}
