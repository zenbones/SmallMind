/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public final class Base64Codec {

  private static final String BASE64_BIBLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  public static String encode (String original)
    throws IOException {

    return encode(original.getBytes(), '+', '/');
  }

  public static String encode (String original, char char62, char char63)
    throws IOException {

    return encode(original.getBytes(), char62, char63);
  }

  public static String encode (byte[] bytes)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), '+', '/');
  }

  public static String encode (byte[] bytes, char char62, char char63)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), char62, char63);
  }

  public static String encode (ByteArrayInputStream byteInputStream, char char62, char char63)
    throws IOException {

    StringBuilder encodeBuilder = new StringBuilder();
    byte[] triplet = new byte[3];
    int bytesRead;

    while ((bytesRead = byteInputStream.read(triplet)) >= 0) {
      for (int index = 0; index < bytesRead; index++) {
        switch (index) {
          case 0:
            encodeBuilder.append(charAtBase64Bible((triplet[0] & 0xFF) >>> 2, char62, char63));
            break;
          case 1:
            encodeBuilder.append(charAtBase64Bible(((triplet[0] & 3) << 4) | ((triplet[1] & 0xFF) >>> 4), char62, char63));
            break;
          case 2:
            encodeBuilder.append(charAtBase64Bible(((triplet[1] & 15) << 2) | ((triplet[2] & 0xFF) >>> 6), char62, char63));
            encodeBuilder.append(charAtBase64Bible(triplet[2] & 63, char62, char63));
            break;
        }
      }

      if (bytesRead == 1) {
        encodeBuilder.append(charAtBase64Bible((triplet[0] & 3) << 4, char62, char63));
        encodeBuilder.append('=').append('=');
      } else if (bytesRead == 2) {
        encodeBuilder.append(charAtBase64Bible((triplet[1] & 15) << 2, char62, char63));
        encodeBuilder.append('=');
      }
    }

    return encodeBuilder.toString();
  }

  public static byte[] decode (String encoded)
    throws IOException {

    return decode(encoded.getBytes(), true, '+', '/');
  }

  public static byte[] decode (String encoded, boolean strict)
    throws IOException {

    return decode(encoded.getBytes(), strict, '+', '/');
  }

  public static byte[] decode (String encoded, char char62, char char63)
    throws IOException {

    return decode(encoded.getBytes(), true, char62, char63);
  }

  public static byte[] decode (String encoded, boolean strict, char char62, char char63)
    throws IOException {

    return decode(encoded.getBytes(), strict, char62, char63);
  }

  public static byte[] decode (byte[] bytes)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), true, '+', '/');
  }

  public static byte[] decode (byte[] bytes, boolean strict)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), strict, '+', '/');
  }

  public static byte[] decode (byte[] bytes, char char62, char char63)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), true, char62, char63);
  }

  public static byte[] decode (byte[] bytes, boolean strict, char char62, char char63)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), strict, char62, char63);
  }

  public static byte[] decode (ByteArrayInputStream byteInputStream)
    throws IOException {

    return decode(byteInputStream, true, '+', '/');
  }

  public static byte[] decode (ByteArrayInputStream byteInputStream, boolean strict)
    throws IOException {

    return decode(byteInputStream, strict, '+', '/');
  }

  public static byte[] decode (ByteArrayInputStream byteInputStream, char char62, char char63)
    throws IOException {

    return decode(byteInputStream, true, char62, char63);
  }

  public static byte[] decode (ByteArrayInputStream byteInputStream, boolean strict, char char62, char char63)
    throws IOException {

    ByteArrayOutputStream byteOutputStream;
    boolean endOfStream;
    byte[] buffer = new byte[4];
    byte[] quartet = new byte[4];

    byteOutputStream = new ByteArrayOutputStream();

    do {
      endOfStream = fillBuffer(byteInputStream, buffer, strict);
      if ((!endOfStream) || (buffer[0] != '=') || (buffer[1] != '=') || (buffer[2] != '=') || (buffer[3] != '=')) {
        if ((buffer[0] == '=') || (buffer[1] == '=') || ((buffer[2] == '=') && (buffer[3] != '='))) {
          throw new UnsupportedEncodingException("Not a base64 encoded stream");
        }

        for (int index = 0; index < buffer.length; index++) {
          if ((quartet[index] = (byte) indexOfBase64Bible(buffer[index], char62, char63)) < 0) {
            throw new UnsupportedEncodingException("Not a base64 encoded stream");
          }
        }

        byteOutputStream.write(((quartet[0] & 63) << 2) | ((quartet[1] & 48) >>> 4));
        if (buffer[2] != '=') {
          byteOutputStream.write(((quartet[1] & 15) << 4) | ((quartet[2] & 60) >>> 2));
        }
        if (buffer[3] != '=') {
          byteOutputStream.write(((quartet[2] & 3) << 6) | (quartet[3] & 63));
        }
      }
    } while (!endOfStream);

    return byteOutputStream.toByteArray();
  }

  private static boolean fillBuffer (ByteArrayInputStream byteInputStream, byte[] buffer, boolean strict)
    throws IOException {

    int bytesRead;
    int offset = 0;

    do {
      bytesRead = byteInputStream.read(buffer, offset, buffer.length - offset);
    } while ((bytesRead >= 0) && ((offset += bytesRead) < buffer.length));

    if (strict && (offset > 0) && (offset < buffer.length)) {
      throw new UnsupportedEncodingException("Not a base64 encoded stream");
    }

    for (int index = offset; index < buffer.length; index++) {
      buffer[index] = '=';
    }

    return bytesRead < 0;
  }

  private static char charAtBase64Bible (int index, char char62, char char63) {

    if (index == 64) {

      return '=';
    }
    if (index == 63) {

      return char63;
    }
    if (index == 62) {

      return char62;
    }

    return BASE64_BIBLE.charAt(index);
  }

  private static int indexOfBase64Bible (byte singleChar, char char62, char char63) {

    if (singleChar == '=') {

      return 64;
    }
    if (singleChar == char63) {

      return 63;
    }
    if (singleChar == char62) {

      return 62;
    }

    return BASE64_BIBLE.indexOf(singleChar);
  }
}