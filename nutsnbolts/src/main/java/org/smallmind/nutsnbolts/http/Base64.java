/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
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

public final class Base64 {

  private static final String BASE64_BIBLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

  public static String encode (String original)
    throws IOException {

    return encode(original.getBytes());
  }

  public static String encode (byte[] bytes)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes));
  }

  public static String encode (ByteArrayInputStream byteInputStream)
    throws IOException {

    StringBuilder encodeBuilder = new StringBuilder();
    byte[] triplet = new byte[3];
    int bytesRead;

    while ((bytesRead = byteInputStream.read(triplet)) >= 0) {
      for (int index = 0; index < bytesRead; index++) {
        switch (index) {
          case 0:
            encodeBuilder.append(BASE64_BIBLE.charAt(triplet[0] >>> 2));
            break;
          case 1:
            encodeBuilder.append(BASE64_BIBLE.charAt(((triplet[0] & 3) << 4) | (triplet[1] >>> 4)));
            break;
          case 2:
            encodeBuilder.append(BASE64_BIBLE.charAt(((triplet[1] & 15) << 2) | (triplet[2] >>> 6)));
            encodeBuilder.append(BASE64_BIBLE.charAt(triplet[2] & 63));
            break;
        }
      }

      if (bytesRead == 1) {
        encodeBuilder.append(BASE64_BIBLE.charAt((triplet[0] & 3) << 4));
        encodeBuilder.append('=').append('=');
      }
      else if (bytesRead == 2) {
        encodeBuilder.append(BASE64_BIBLE.charAt((triplet[1] & 15) << 2));
        encodeBuilder.append('=');
      }
    }

    return encodeBuilder.toString();
  }

  public static byte[] decode (String encoded)
    throws IOException {

    return decode(encoded.getBytes());
  }

  public static byte[] decode (byte[] bytes)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes));
  }

  public static byte[] decode (ByteArrayInputStream byteInputStream)
    throws IOException {

    ByteArrayOutputStream byteOutputStream;
    boolean endOfStream = false;
    byte[] buffer = new byte[4];
    byte[] quartet = new byte[4];
    int bytesRead;

    byteOutputStream = new ByteArrayOutputStream();
    while ((bytesRead = byteInputStream.read(buffer)) >= 0) {
      if (endOfStream) {
        throw new UnsupportedEncodingException("Not a base64 encoded stream");
      }
      if ((bytesRead > 0) && (bytesRead < 4)) {
        throw new UnsupportedEncodingException("The length of the stream must be a multiple of 4");
      }

      for (int index = 0; index < buffer.length; index++) {
        if ((quartet[index] = (byte)BASE64_BIBLE.indexOf(buffer[index])) < 0) {
          throw new UnsupportedEncodingException("Not a base64 encoded stream");
        }
        if (buffer[index] == '=') {
          endOfStream = true;
        }
      }

      if ((buffer[0] == '=') || (buffer[1] == '=') || ((buffer[2] == '=') && (buffer[3] != '='))) {
        throw new UnsupportedEncodingException("Not a base64 encoded stream");
      }

      byteOutputStream.write(((quartet[0] & 63) << 2) | ((quartet[1] & 48) >>> 4));

      if (buffer[2] == '=') {
        byteOutputStream.write((quartet[1] & 15) << 4);
      }
      else {
        byteOutputStream.write(((quartet[1] & 15) << 4) | ((quartet[2] & 60) >>> 2));
      }

      if (buffer[3] != '=') {
        byteOutputStream.write(((quartet[2] & 3) << 6) | (quartet[3] & 63));
      }
    }

    return byteOutputStream.toByteArray();
  }
}