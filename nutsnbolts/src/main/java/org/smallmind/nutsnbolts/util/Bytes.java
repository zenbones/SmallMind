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
 * Utilities for encoding/decoding primitive integer types to and from byte arrays using big-endian {@link ByteBuffer}.
 */
public class Bytes {

  /**
   * @param l long value
   * @return 8-byte array representation
   */
  public static byte[] getBytes (long l) {

    ByteBuffer translationBuffer;
    byte[] byteArray = new byte[Long.BYTES];

    translationBuffer = ByteBuffer.wrap(byteArray);
    translationBuffer.putLong(l);
    return byteArray;
  }

  /**
   * @param i int value
   * @return 4-byte array representation
   */
  public static byte[] getBytes (int i) {

    ByteBuffer translationBuffer;
    byte[] byteArray = new byte[Integer.BYTES];

    translationBuffer = ByteBuffer.wrap(byteArray);
    translationBuffer.putInt(i);
    return byteArray;
  }

  /**
   * @param s short value
   * @return 2-byte array representation
   */
  public static byte[] getBytes (short s) {

    ByteBuffer translationBuffer;
    byte[] byteArray = new byte[Short.BYTES];

    translationBuffer = ByteBuffer.wrap(byteArray);
    translationBuffer.putShort(s);
    return byteArray;
  }

  /**
   * @param byteArray 8-byte array
   * @return decoded long
   */
  public static long getLong (byte[] byteArray) {

    ByteBuffer translationBuffer;

    translationBuffer = ByteBuffer.wrap(byteArray);
    return translationBuffer.getLong();
  }

  /**
   * @param byteArray 4-byte array
   * @return decoded int
   */
  public static int getInt (byte[] byteArray) {

    ByteBuffer translationBuffer;

    translationBuffer = ByteBuffer.wrap(byteArray);
    return translationBuffer.getInt();
  }

  /**
   * @param byteArray 2-byte array
   * @return decoded short
   */
  public static short getShort (byte[] byteArray) {

    ByteBuffer translationBuffer;

    translationBuffer = ByteBuffer.wrap(byteArray);
    return translationBuffer.getShort();
  }
}
