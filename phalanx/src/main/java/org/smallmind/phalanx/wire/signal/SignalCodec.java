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
package org.smallmind.phalanx.wire.signal;

/**
 * Codec abstraction for serializing and deserializing wire signals.
 */
public interface SignalCodec {

  /**
   * Returns the MIME content type produced by this codec.
   *
   * @return content type string
   */
  String getContentType ();

  /**
   * Encodes the provided signal into a byte array.
   *
   * @param signal signal to encode
   * @return serialized bytes
   * @throws Exception if encoding fails
   */
  byte[] encode (Signal signal)
    throws Exception;

  /**
   * Decodes the supplied byte buffer into a signal of the given class.
   *
   * @param buffer      byte buffer containing the encoded signal
   * @param offset      offset into the buffer to start decoding
   * @param len         length of the payload to decode
   * @param signalClass target signal class
   * @param <S>         signal type
   * @return decoded signal instance
   * @throws Exception if decoding fails
   */
  <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws Exception;

  /**
   * Extracts a typed object from a decoded value when an adapter or converter is required.
   *
   * @param value decoded value
   * @param clazz target type
   * @param <T>   target type parameter
   * @return converted object instance
   */
  <T> T extractObject (Object value, Class<T> clazz);
}
