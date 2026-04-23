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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import jakarta.ws.rs.core.MediaType;

/**
 * {@link SignalCodec} implementation that serializes and deserializes signals using standard
 * Java object serialization, producing {@code application/octet-stream} payloads.
 */
public class JavaSerializationSignalCodec implements SignalCodec {

  /**
   * Returns {@code application/octet-stream} as the content type for Java-serialized payloads.
   *
   * @return the MIME type string {@code application/octet-stream}
   */
  @Override
  public String getContentType () {

    return MediaType.APPLICATION_OCTET_STREAM;
  }

  /**
   * Serializes {@code signal} to a byte array using {@link ObjectOutputStream}.
   *
   * @param signal the signal to encode
   * @return serialized byte array representing the signal
   * @throws IOException if an I/O error occurs during serialization
   */
  @Override
  public byte[] encode (Signal signal)
    throws IOException {

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

      objectOutputStream.writeObject(signal);

      return byteArrayOutputStream.toByteArray();
    }
  }

  /**
   * Deserializes a signal from the specified region of {@code buffer} using {@link ObjectInputStream}.
   *
   * @param buffer      byte array containing the serialized signal
   * @param offset      starting offset within {@code buffer}
   * @param len         number of bytes to read
   * @param signalClass the expected signal type
   * @param <S>         the signal type parameter
   * @return the deserialized signal cast to {@code signalClass}
   * @throws IOException            if an I/O error occurs during deserialization
   * @throws ClassNotFoundException if the class of the serialized object cannot be found
   */
  @Override
  public <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws IOException, ClassNotFoundException {

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, offset, len); ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

      return signalClass.cast(objectInputStream.readObject());
    }
  }

  /**
   * Returns {@code value} cast to {@code clazz}; no conversion is performed because
   * Java deserialization already produces the correct runtime type.
   *
   * @param value the decoded value to cast
   * @param clazz the target type
   * @param <T>   the target type parameter
   * @return {@code value} cast to {@code T}
   */
  @Override
  public <T> T extractObject (Object value, Class<T> clazz) {

    return clazz.cast(value);
  }
}
