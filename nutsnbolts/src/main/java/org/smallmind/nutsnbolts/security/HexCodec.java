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
package org.smallmind.nutsnbolts.security;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Hex encoder/decoder for binary data.
 */
public class HexCodec {

  /**
   * Encodes an entire byte array to a lowercase hex string.
   *
   * @param bytes the data to encode
   * @return a hex string
   */
  public static String hexEncode (byte[] bytes) {

    return hexEncode(bytes, 0, bytes.length);
  }

  /**
   * Encodes a slice of a byte array to lowercase hex.
   *
   * @param bytes  the data to encode
   * @param offset starting offset
   * @param length number of bytes to encode
   * @return a hex string
   */
  public static String hexEncode (byte[] bytes, int offset, int length) {

    StringBuilder encodingBuilder;

    encodingBuilder = new StringBuilder();
    for (int index = offset; index < offset + length; index++) {
      if ((bytes[index] < 0x10) && (bytes[index] >= 0)) {
        encodingBuilder.append('0');
      }
      encodingBuilder.append(Integer.toHexString(bytes[index] & 0xff));
    }

    return encodingBuilder.toString();
  }

  /**
   * Decodes a hex string into bytes.
   *
   * @param toBeDecoded the hex string
   * @return the decoded bytes
   * @throws UnsupportedEncodingException if the input is not valid hex
   */
  public static byte[] hexDecode (String toBeDecoded)
    throws UnsupportedEncodingException {

    return hexDecode(toBeDecoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Decodes hex-encoded bytes into the original byte array.
   *
   * @param toBeDecoded the hex characters as bytes
   * @return the decoded bytes
   * @throws UnsupportedEncodingException if the input is not valid hex
   */
  public static byte[] hexDecode (byte[] toBeDecoded)
    throws UnsupportedEncodingException {

    if (toBeDecoded.length % 2 != 0) {
      throw new UnsupportedEncodingException("Not hex encoded");
    } else {

      byte[] bytes = new byte[toBeDecoded.length / 2];

      for (int count = 0; count < toBeDecoded.length; count += 2) {
        if (isHexDigit(toBeDecoded[count]) && isHexDigit(toBeDecoded[count + 1])) {
          bytes[count / 2] = (byte)((Character.digit(toBeDecoded[count], 16) * 16) + Character.digit(toBeDecoded[count + 1], 16));
        } else {
          throw new UnsupportedEncodingException("Not hex encoded");
        }
      }

      return bytes;
    }
  }

  /**
   * Tests whether the supplied byte represents a hex digit character.
   *
   * @param singleChar the character to test
   * @return {@code true} if the character is 0-9, a-f, or A-F
   */
  private static boolean isHexDigit (byte singleChar) {

    return ((singleChar >= '0') && (singleChar <= '9')) || ((singleChar >= 'A') && (singleChar <= 'F')) || ((singleChar >= 'a') && (singleChar <= 'f'));
  }
}
