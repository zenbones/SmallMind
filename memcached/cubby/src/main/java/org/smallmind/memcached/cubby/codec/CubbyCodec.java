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
package org.smallmind.memcached.cubby.codec;

import java.io.IOException;

/**
 * Strategy interface for serializing objects to bytes and deserializing bytes back to
 * objects for storage in and retrieval from memcached. Implementations may apply any
 * encoding scheme (e.g., Java object streams, JSON, Kryo) and may be composed with
 * decorators such as {@link LargeValueCompressingCodec} to add compression.
 */
public interface CubbyCodec {

  /**
   * Serializes the supplied object into a byte array suitable for storage in memcached.
   *
   * @param obj the object to serialize; must not be {@code null}
   * @return the encoded byte representation of {@code obj}
   * @throws IOException if an I/O error occurs during serialization
   */
  byte[] serialize (Object obj)
    throws IOException;

  /**
   * Deserializes a byte array previously produced by {@link #serialize} back into
   * an object.
   *
   * @param bytes the serialized byte array to decode
   * @return the deserialized object
   * @throws IOException            if an I/O error occurs during deserialization
   * @throws ClassNotFoundException if the class of a serialized object cannot be found
   */
  Object deserialize (byte[] bytes)
    throws IOException, ClassNotFoundException;
}
