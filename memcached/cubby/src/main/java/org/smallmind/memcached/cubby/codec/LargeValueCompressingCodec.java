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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A {@link CubbyCodec} decorator that transparently applies GZIP compression to
 * serialized payloads that meet or exceed a configurable size threshold. Values smaller
 * than the threshold are passed through to the delegate codec unchanged.
 *
 * <p>During serialization, if the byte array produced by the delegate codec is at least
 * {@code compressionThreshold} bytes long, it is compressed with GZIP and re-serialized
 * inside a {@link GzipObjectWrapper} envelope. During deserialization, the presence of a
 * {@code GzipObjectWrapper} triggers the reverse process: the bytes are decompressed
 * before being handed back to the delegate for final decoding.
 *
 * <p>The default compression threshold is {@value #DEFAULT_COMPRESSION_THRESHOLD} bytes.
 */
public class LargeValueCompressingCodec implements CubbyCodec {

  private static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

  private final CubbyCodec codec;
  private final int compressionThreshold;

  /**
   * Constructs a compressing codec that wraps the supplied delegate and uses the default
   * compression threshold of {@value #DEFAULT_COMPRESSION_THRESHOLD} bytes.
   *
   * @param codec the delegate {@link CubbyCodec} used for underlying serialization
   */
  public LargeValueCompressingCodec (CubbyCodec codec) {

    this(codec, DEFAULT_COMPRESSION_THRESHOLD);
  }

  /**
   * Constructs a compressing codec that wraps the supplied delegate and applies
   * compression only when the serialized payload is at least {@code compressionThreshold}
   * bytes.
   *
   * @param codec                the delegate {@link CubbyCodec} used for underlying serialization
   * @param compressionThreshold minimum serialized payload size in bytes that triggers
   *                             GZIP compression
   */
  public LargeValueCompressingCodec (CubbyCodec codec, int compressionThreshold) {

    this.codec = codec;
    this.compressionThreshold = compressionThreshold;
  }

  /**
   * Serializes {@code obj} using the delegate codec and, if the resulting byte array
   * meets the compression threshold, compresses it with GZIP and wraps it in a
   * {@link GzipObjectWrapper} before re-serializing with the delegate.
   *
   * @param obj the object to serialize; must not be {@code null}
   * @return the encoded (and possibly compressed) byte array
   * @throws IOException if an I/O error occurs during serialization or compression
   */
  @Override
  public byte[] serialize (Object obj)
    throws IOException {

    byte[] bytes;

    if ((bytes = codec.serialize(obj)).length >= compressionThreshold) {

      ByteArrayOutputStream byteStream;

      try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteStream = new ByteArrayOutputStream())) {
        gzipOut.write(bytes);
      }

      return codec.serialize(new GzipObjectWrapper(byteStream.toByteArray()));
    } else {

      return bytes;
    }
  }

  /**
   * Deserializes {@code bytes} using the delegate codec. If the result is a
   * {@link GzipObjectWrapper}, the compressed payload is decompressed with GZIP and the
   * uncompressed bytes are passed back to the delegate for final deserialization.
   *
   * @param bytes the byte array to deserialize
   * @return the deserialized object
   * @throws IOException            if an I/O error occurs during deserialization or
   *                                decompression
   * @throws ClassNotFoundException if the class of a serialized object cannot be found
   */
  @Override
  public Object deserialize (byte[] bytes)
    throws IOException, ClassNotFoundException {

    Object obj;

    if ((obj = codec.deserialize(bytes)) instanceof GzipObjectWrapper) {

      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

      try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(((GzipObjectWrapper)obj).getCompressedBytes()))) {

        int bytesRead;
        byte[] buffer = new byte[8196];

        while ((bytesRead = gzipIn.read(buffer)) >= 0) {
          byteStream.write(buffer, 0, bytesRead);
        }
      }

      return codec.deserialize(byteStream.toByteArray());
    } else {

      return obj;
    }
  }
}
