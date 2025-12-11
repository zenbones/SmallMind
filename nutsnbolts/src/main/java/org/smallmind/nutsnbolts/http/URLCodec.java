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
package org.smallmind.nutsnbolts.http;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.HashMap;

public class URLCodec {

  private static final BitSet UNRESERVED_BITSET = new BitSet(256);
  private static final HashMap<Character, Integer> HEX_VALUE_MAP = new HashMap<>();
  private static final String HEX_DIGITS = "0123456789ABCDEF";

  static {

    String unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~";

    for (int index = 0; index < unreserved.length(); index++) {
      UNRESERVED_BITSET.set(unreserved.charAt(index));
    }

    HEX_VALUE_MAP.put('0', 0);
    HEX_VALUE_MAP.put('1', 1);
    HEX_VALUE_MAP.put('2', 2);
    HEX_VALUE_MAP.put('3', 3);
    HEX_VALUE_MAP.put('4', 4);
    HEX_VALUE_MAP.put('5', 5);
    HEX_VALUE_MAP.put('6', 6);
    HEX_VALUE_MAP.put('7', 7);
    HEX_VALUE_MAP.put('8', 8);
    HEX_VALUE_MAP.put('9', 9);
    HEX_VALUE_MAP.put('A', 10);
    HEX_VALUE_MAP.put('a', 10);
    HEX_VALUE_MAP.put('B', 11);
    HEX_VALUE_MAP.put('b', 11);
    HEX_VALUE_MAP.put('C', 12);
    HEX_VALUE_MAP.put('c', 12);
    HEX_VALUE_MAP.put('D', 13);
    HEX_VALUE_MAP.put('d', 13);
    HEX_VALUE_MAP.put('E', 14);
    HEX_VALUE_MAP.put('e', 14);
    HEX_VALUE_MAP.put('F', 15);
    HEX_VALUE_MAP.put('f', 15);
  }

  public static String urlDecode (String encoded)
    throws UnsupportedEncodingException {

    StringBuilder decodedBuilder = null;
    int decoding = 0;
    byte expected = 0;

    for (int index = 0; index < encoded.length(); index++) {

      char singleChar;

      switch (singleChar = encoded.charAt(index)) {
        case '+':
          if (expected > 0) {
            throw new UnsupportedEncodingException("Not URL encoded");
          } else {
            if (decodedBuilder == null) {
              decodedBuilder = new StringBuilder(encoded.substring(0, index));
            }
            decodedBuilder.append(' ');
          }
          break;
        case '%':
          if (encoded.length() < index + 3) {
            throw new UnsupportedEncodingException("Not URL encoded");
          } else {
            if (decodedBuilder == null) {
              decodedBuilder = new StringBuilder(encoded.substring(0, index));
            }

            int decodedByte = getByte(encoded.charAt(++index), encoded.charAt(++index));

            if (expected > 0) {
              if ((decodedByte >> 6) != 0x2) {
                throw new UnsupportedEncodingException("Not URL encoded");
              } else {
                decoding |= (decodedByte & 0x3F) << (--expected * 6);
                if (expected == 0) {
                  decodedBuilder.append(Character.toChars(decoding));
                }
              }
            } else {
              if ((decodedByte & 0x80) == 0) {
                decodedBuilder.append((char)decodedByte);
              } else if ((decodedByte >> 5) == 0x6) {
                expected = 1;
                decoding = (decodedByte & 0x1F) << 6;
              } else if ((decodedByte >> 4) == 0xE) {
                expected = 2;
                decoding = (decodedByte & 0xF) << 12;
              } else if ((decodedByte >> 3) == 0x1E) {
                expected = 3;
                decoding = (decodedByte & 0x7) << 18;
              } else {
                throw new UnsupportedEncodingException("Not URL encoded");
              }
            }
          }
          break;
        default:
          if (expected > 0) {
            throw new UnsupportedEncodingException("Not URL encoded");
          } else {
            if (decodedBuilder != null) {
              decodedBuilder.append(singleChar);
            }
          }
      }
    }

    if (expected > 0) {
      throw new UnsupportedEncodingException("Not URL encoded");
    }

    return (decodedBuilder == null) ? encoded : decodedBuilder.toString();
  }

  private static int getByte (char highHexNumber, char lowHexNumber)
    throws UnsupportedEncodingException {

    Integer highByte = HEX_VALUE_MAP.get(highHexNumber);
    Integer lowByte = HEX_VALUE_MAP.get(lowHexNumber);

    if ((highByte == null) || (lowByte == null)) {
      throw new UnsupportedEncodingException("Not URL encoded");
    } else {

      return (highByte << 4) | lowByte;
    }
  }

  public static String urlEncode (String original)
    throws UnsupportedEncodingException {

    return urlEncode(original, true);
  }

  public static String urlEncode (String original, boolean spacesAsPluses)
    throws UnsupportedEncodingException {

    StringBuilder encodedBuilder = null;

    for (int index = 0; index < original.length(); index++) {

      char singleChar = original.charAt(index);

      if (UNRESERVED_BITSET.get(singleChar)) {
        if (encodedBuilder != null) {
          encodedBuilder.append(singleChar);
        }
      } else if ((singleChar == ' ') && spacesAsPluses) {
        if (encodedBuilder == null) {
          encodedBuilder = new StringBuilder(original.substring(0, index));
        }
        encodedBuilder.append('+');
      } else {
        if (encodedBuilder == null) {
          encodedBuilder = new StringBuilder(original.substring(0, index));
        }

        if (Character.isHighSurrogate(singleChar)) {
          if (original.length() < index + 2) {
            throw new UnsupportedEncodingException("Not able to URL encode");
          } else {

            char lowSurrogate = original.charAt(++index);

            if (!Character.isLowSurrogate(lowSurrogate)) {
              throw new UnsupportedEncodingException("Not able to URL encode");
            } else {

              int codePoint = Character.toCodePoint(singleChar, lowSurrogate);

              writeHexString(encodedBuilder, (codePoint >> 18) | 0x1E);
              writeHexString(encodedBuilder, ((codePoint >> 12) & 0x3F) | 0x80);
              writeHexString(encodedBuilder, ((codePoint >> 6) & 0x3F) | 0x80);
              writeHexString(encodedBuilder, (codePoint & 0x3f) | 0x80);
            }
          }
        } else if (singleChar <= 0x007F) {
          writeHexString(encodedBuilder, singleChar);
        } else if (singleChar <= 0x07FF) {
          writeHexString(encodedBuilder, (singleChar >> 6) | 0xC0);
          writeHexString(encodedBuilder, (singleChar & 0x3F) | 0x80);
        } else {
          writeHexString(encodedBuilder, (singleChar >> 12) | 0xE0);
          writeHexString(encodedBuilder, ((singleChar >> 6) & 0x3F) | 0x80);
          writeHexString(encodedBuilder, (singleChar & 0x3f) | 0x80);
        }
      }
    }

    return (encodedBuilder == null) ? original : encodedBuilder.toString();
  }

  private static void writeHexString (StringBuilder encodedBuilder, int singleByte) {

    encodedBuilder.append('%');
    encodedBuilder.append(HEX_DIGITS.charAt(singleByte >>> 4));
    encodedBuilder.append(HEX_DIGITS.charAt(singleByte & 0xF));
  }
}
