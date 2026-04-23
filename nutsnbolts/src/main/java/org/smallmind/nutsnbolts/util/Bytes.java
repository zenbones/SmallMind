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
package org.smallmind.nutsnbolts.util;

import java.nio.ByteBuffer;

/**
 * Utility methods for encoding primitive integer types to byte arrays and decoding them back,
 * using big-endian byte order via {@link ByteBuffer}.
 */
public class Bytes {

  /**
   * Encodes a {@code long} value into an 8-byte big-endian array.
   *
   * @param l the long value to encode
   * @return an 8-byte array containing the big-endian representation of {@code l}
   */
  public static byte[] getBytes (long l) {

    ByteBuffer translationBuffer;
    byte[] byteArray = new byte[Long.BYTES];

    translationBuffer = ByteBuffer.wrap(byteArray);
    translationBuffer.putLong(l);
    return byteArray;
  }

  /**
   * Encodes an {@code int} value into a 4-byte big-endian array.
   *
   * @param i the int value to encode
   * @return a 4-byte array containing the big-endian representation of {@code i}
   */
  public static byte[] getBytes (int i) {

    ByteBuffer translationBuffer;
    byte[] byteArray = new byte[Integer.BYTES];

    translationBuffer = ByteBuffer.wrap(byteArray);
    translationBuffer.putInt(i);
    return byteArray;
  }

  /**
   * Encodes a {@code short} value into a 2-byte big-endian array.
   *
   * @param s the short value to encode
   * @return a 2-byte array containing the big-endian representation of {@code s}
   */
  public static byte[] getBytes (short s) {

    ByteBuffer translationBuffer;
    byte[] byteArray = new byte[Short.BYTES];

    translationBuffer = ByteBuffer.wrap(byteArray);
    translationBuffer.putShort(s);
    return byteArray;
  }

  /**
   * Decodes a {@code long} value from an 8-byte big-endian array.
   *
   * @param byteArray the 8-byte array to decode
   * @return the decoded long value
   */
  public static long getLong (byte[] byteArray) {

    ByteBuffer translationBuffer;

    translationBuffer = ByteBuffer.wrap(byteArray);
    return translationBuffer.getLong();
  }

  /**
   * Decodes an {@code int} value from a 4-byte big-endian array.
   *
   * @param byteArray the 4-byte array to decode
   * @return the decoded int value
   */
  public static int getInt (byte[] byteArray) {

    ByteBuffer translationBuffer;

    translationBuffer = ByteBuffer.wrap(byteArray);
    return translationBuffer.getInt();
  }

  /**
   * Decodes a {@code short} value from a 2-byte big-endian array.
   *
   * @param byteArray the 2-byte array to decode
   * @return the decoded short value
   */
  public static short getShort (byte[] byteArray) {

    ByteBuffer translationBuffer;

    translationBuffer = ByteBuffer.wrap(byteArray);
    return translationBuffer.getShort();
  }
}
