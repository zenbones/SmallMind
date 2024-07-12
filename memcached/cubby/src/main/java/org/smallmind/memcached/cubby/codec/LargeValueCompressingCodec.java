/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class LargeValueCompressingCodec implements CubbyCodec {

  private static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

  private final CubbyCodec codec;
  private final int compressionThreshold;

  public LargeValueCompressingCodec (CubbyCodec codec) {

    this(codec, DEFAULT_COMPRESSION_THRESHOLD);
  }

  public LargeValueCompressingCodec (CubbyCodec codec, int compressionThreshold) {

    this.codec = codec;
    this.compressionThreshold = compressionThreshold;
  }

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
