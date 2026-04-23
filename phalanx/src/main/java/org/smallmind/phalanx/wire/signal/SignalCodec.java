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
 * Strategy interface for encoding {@link Signal} objects to bytes and decoding them back,
 * along with a helper for converting raw decoded values to typed instances.
 */
public interface SignalCodec {

  /**
   * Returns the MIME content type string that describes the format produced by {@link #encode}.
   *
   * @return the content type (e.g., {@code application/json} or {@code application/octet-stream})
   */
  String getContentType ();

  /**
   * Encodes {@code signal} into a byte array using the format specific to this codec.
   *
   * @param signal the signal to encode; must not be {@code null}
   * @return a non-null byte array containing the encoded signal
   * @throws Exception if an error occurs during encoding
   */
  byte[] encode (Signal signal)
    throws Exception;

  /**
   * Decodes the signal stored in the specified region of {@code buffer} into an instance of
   * {@code signalClass}.
   *
   * @param buffer      the byte array containing the encoded signal
   * @param offset      the start offset within {@code buffer}
   * @param len         the number of bytes to read
   * @param signalClass the expected concrete type of the signal
   * @param <S>         the signal type parameter
   * @return the decoded signal, cast to {@code signalClass}
   * @throws Exception if decoding or type conversion fails
   */
  <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws Exception;

  /**
   * Converts a raw decoded value (e.g., a tree-model node or an untyped map) into an instance
   * of {@code clazz} using whatever conversion mechanism the codec provides.
   *
   * @param value the raw value to convert; may be {@code null}
   * @param clazz the target type; must not be {@code null}
   * @param <T>   the target type parameter
   * @return the converted object, or {@code null} if {@code value} is {@code null}
   */
  <T> T extractObject (Object value, Class<T> clazz);
}
