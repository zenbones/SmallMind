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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.smallmind.nutsnbolts.io.ByteBufferInputStream;

/**
 * Utility for Base64 encoding/decoding strings, byte arrays, {@link ByteBuffer}s, and streams.
 * Supports custom characters for 62/63, optional padding, and URL-safe alphabets.
 */
public final class Base64Codec {

  private static final String BASE64_BIBLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  /**
   * Encodes the UTF-8 bytes of a string using URL-safe Base64 without padding.
   *
   * @param original text to encode
   * @return Base64 string using '-' and '_'
   * @throws IOException if the input cannot be read
   */
  public static String urlSafeEncode (String original)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), false, '-', '_');
  }

  /**
   * Encodes bytes using URL-safe Base64 without padding.
   *
   * @param bytes data to encode
   * @return Base64 string using '-' and '_'
   * @throws IOException if the input cannot be read
   */
  public static String urlSafeEncode (byte[] bytes)
    throws IOException {

    return encode(bytes, false, '-', '_');
  }

  /**
   * Encodes a {@link ByteBuffer} using URL-safe Base64 without padding.
   *
   * @param buffer buffer to encode (consumed from current position)
   * @return Base64 string using '-' and '_'
   * @throws IOException if the input cannot be read
   */
  public static String urlSafeEncode (ByteBuffer buffer)
    throws IOException {

    return encode(buffer, false, '-', '_');
  }

  /**
   * Encodes all bytes from a stream using URL-safe Base64 without padding.
   *
   * @param inputStream stream to encode (fully consumed)
   * @return Base64 string using '-' and '_'
   * @throws IOException if reading fails
   */
  public static String urlSafeEncode (InputStream inputStream)
    throws IOException {

    return encode(inputStream, false, '-', '_');
  }

  /**
   * Encodes the UTF-8 bytes of a string using standard Base64 with padding.
   *
   * @param original text to encode
   * @return Base64 string using '+' and '/'
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), true, '+', '/');
  }

  /**
   * Encodes the UTF-8 bytes of a string using standard Base64.
   *
   * @param original       text to encode
   * @param includePadding {@code true} to append '=' padding
   * @return Base64 string using '+' and '/'
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original, boolean includePadding)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), includePadding, '+', '/');
  }

  /**
   * Encodes the UTF-8 bytes of a string using custom 62/63 characters with padding.
   *
   * @param original text to encode
   * @param char62   replacement for index 62
   * @param char63   replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original, char char62, char char63)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), true, char62, char63);
  }

  /**
   * Encodes the UTF-8 bytes of a string using custom alphabet and optional padding.
   *
   * @param original       text to encode
   * @param includePadding {@code true} to append '=' padding
   * @param char62         replacement for index 62
   * @param char63         replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original, boolean includePadding, char char62, char char63)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), includePadding, char62, char63);
  }

  /**
   * Encodes bytes using standard Base64 with padding.
   *
   * @param bytes data to encode
   * @return Base64 string using '+' and '/'
   * @throws IOException if reading fails
   */
  public static String encode (byte[] bytes)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), true, '+', '/');
  }

  /**
   * Encodes bytes using standard Base64.
   *
   * @param bytes          data to encode
   * @param includePadding {@code true} to append '=' padding
   * @return Base64 string using '+' and '/'
   * @throws IOException if reading fails
   */
  public static String encode (byte[] bytes, boolean includePadding)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), includePadding, '+', '/');
  }

  /**
   * Encodes bytes using custom 62/63 characters with padding.
   *
   * @param bytes  data to encode
   * @param char62 replacement for index 62
   * @param char63 replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if reading fails
   */
  public static String encode (byte[] bytes, char char62, char char63)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), true, char62, char63);
  }

  /**
   * Encodes bytes using custom alphabet and optional padding.
   *
   * @param bytes          data to encode
   * @param includePadding {@code true} to append '=' padding
   * @param char62         replacement for index 62
   * @param char63         replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if reading fails
   */
  public static String encode (byte[] bytes, boolean includePadding, char char62, char char63)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), includePadding, char62, char63);
  }

  /**
   * Encodes a {@link ByteBuffer} using standard Base64 with padding.
   *
   * @param buffer buffer to encode (consumed)
   * @return Base64 string using '+' and '/'
   * @throws IOException if reading fails
   */
  public static String encode (ByteBuffer buffer)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), true, '+', '/');
  }

  /**
   * Encodes a {@link ByteBuffer} using standard Base64.
   *
   * @param buffer         buffer to encode (consumed)
   * @param includePadding {@code true} to append '=' padding
   * @return Base64 string using '+' and '/'
   * @throws IOException if reading fails
   */
  public static String encode (ByteBuffer buffer, boolean includePadding)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), includePadding, '+', '/');
  }

  /**
   * Encodes a {@link ByteBuffer} using custom 62/63 characters with padding.
   *
   * @param buffer buffer to encode (consumed)
   * @param char62 replacement for index 62
   * @param char63 replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if reading fails
   */
  public static String encode (ByteBuffer buffer, char char62, char char63)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), true, char62, char63);
  }

  /**
   * Encodes a {@link ByteBuffer} using custom alphabet and optional padding.
   *
   * @param buffer         buffer to encode (consumed)
   * @param includePadding {@code true} to append '=' padding
   * @param char62         replacement for index 62
   * @param char63         replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if reading fails
   */
  public static String encode (ByteBuffer buffer, boolean includePadding, char char62, char char63)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), includePadding, char62, char63);
  }

  /**
   * Encodes all bytes from a stream using standard Base64 with padding.
   *
   * @param inputStream stream to encode (fully consumed)
   * @return Base64 string using '+' and '/'
   * @throws IOException if reading fails
   */
  public static String encode (InputStream inputStream)
    throws IOException {

    return encode(inputStream, true, '+', '/');
  }

  /**
   * Encodes all bytes from a stream using standard Base64.
   *
   * @param inputStream    stream to encode (fully consumed)
   * @param includePadding {@code true} to append '=' padding
   * @return Base64 string using '+' and '/'
   * @throws IOException if reading fails
   */
  public static String encode (InputStream inputStream, boolean includePadding)
    throws IOException {

    return encode(inputStream, includePadding, '+', '/');
  }

  /**
   * Encodes all bytes from a stream using custom characters with padding.
   *
   * @param inputStream stream to encode (fully consumed)
   * @param char62      replacement for index 62
   * @param char63      replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if reading fails
   */
  public static String encode (InputStream inputStream, char char62, char char63)
    throws IOException {

    return encode(inputStream, true, char62, char63);
  }

  /**
   * Encodes all bytes from a stream using custom alphabet and optional padding.
   *
   * @param inputStream    stream to encode (fully consumed)
   * @param includePadding {@code true} to append '=' padding
   * @param char62         replacement for index 62
   * @param char63         replacement for index 63
   * @return Base64 string using supplied alphabet
   * @throws IOException if reading fails
   */
  public static String encode (InputStream inputStream, boolean includePadding, char char62, char char63)
    throws IOException {

    StringBuilder encodeBuilder = new StringBuilder();
    byte[] triplet = new byte[3];
    int bytesRead;

    while ((bytesRead = inputStream.read(triplet)) >= 0) {
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
        if (includePadding) {
          encodeBuilder.append('=').append('=');
        }
      } else if (bytesRead == 2) {
        encodeBuilder.append(charAtBase64Bible((triplet[1] & 15) << 2, char62, char63));
        if (includePadding) {
          encodeBuilder.append('=');
        }
      }
    }

    return encodeBuilder.toString();
  }

  /**
   * Decodes a URL-safe Base64 string into bytes.
   *
   * @param encoded Base64 text using '-' and '_'
   * @return decoded bytes
   * @throws IOException if the text is not valid Base64
   */
  public static byte[] urlSafeDecode (String encoded)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), false, '-', '_');
  }

  /**
   * Decodes URL-safe Base64 bytes into the original bytes.
   *
   * @param bytes Base64 data using '-' and '_'
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] urlSafeDecode (byte[] bytes)
    throws IOException {

    return decode(bytes, false, '-', '_');
  }

  /**
   * Decodes URL-safe Base64 from a stream.
   *
   * @param inputStream Base64 stream using '-' and '_'
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] urlSafeDecode (InputStream inputStream)
    throws IOException {

    return decode(inputStream, false, '-', '_');
  }

  /**
   * Decodes a standard Base64 string with padding.
   *
   * @param encoded Base64 text using '+' and '/'
   * @return decoded bytes
   * @throws IOException if the text is not valid Base64
   */
  public static byte[] decode (String encoded)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), true, '+', '/');
  }

  /**
   * Decodes a standard Base64 string with optional strictness.
   *
   * @param encoded Base64 text using '+' and '/'
   * @param strict  {@code true} to reject truncated input
   * @return decoded bytes
   * @throws IOException if the text is not valid Base64
   */
  public static byte[] decode (String encoded, boolean strict)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), strict, '+', '/');
  }

  /**
   * Decodes a Base64 string using custom alphabet with padding.
   *
   * @param encoded Base64 text
   * @param char62  expected character for index 62
   * @param char63  expected character for index 63
   * @return decoded bytes
   * @throws IOException if the text is not valid Base64
   */
  public static byte[] decode (String encoded, char char62, char char63)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), true, char62, char63);
  }

  /**
   * Decodes a Base64 string using custom alphabet and optional strictness.
   *
   * @param encoded Base64 text
   * @param strict  {@code true} to reject truncated input
   * @param char62  expected character for index 62
   * @param char63  expected character for index 63
   * @return decoded bytes
   * @throws IOException if the text is not valid Base64
   */
  public static byte[] decode (String encoded, boolean strict, char char62, char char63)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), strict, char62, char63);
  }

  /**
   * Decodes standard Base64 bytes.
   *
   * @param bytes Base64 data
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), true, '+', '/');
  }

  /**
   * Decodes standard Base64 bytes with optional strictness.
   *
   * @param bytes  Base64 data
   * @param strict {@code true} to reject truncated input
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes, boolean strict)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), strict, '+', '/');
  }

  /**
   * Decodes Base64 bytes using custom alphabet with padding.
   *
   * @param bytes  Base64 data
   * @param char62 expected character for index 62
   * @param char63 expected character for index 63
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes, char char62, char char63)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), true, char62, char63);
  }

  /**
   * Decodes Base64 bytes using custom alphabet and optional strictness.
   *
   * @param bytes  Base64 data
   * @param strict {@code true} to reject truncated input
   * @param char62 expected character for index 62
   * @param char63 expected character for index 63
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes, boolean strict, char char62, char char63)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), strict, char62, char63);
  }

  /**
   * Decodes standard Base64 from a buffer.
   *
   * @param buffer Base64 data
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), true, '+', '/');
  }

  /**
   * Decodes standard Base64 from a buffer with optional strictness.
   *
   * @param buffer Base64 data
   * @param strict {@code true} to reject truncated input
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer, boolean strict)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), strict, '+', '/');
  }

  /**
   * Decodes Base64 from a buffer using custom alphabet with padding.
   *
   * @param buffer Base64 data
   * @param char62 expected character for index 62
   * @param char63 expected character for index 63
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer, char char62, char char63)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), true, char62, char63);
  }

  /**
   * Decodes Base64 from a buffer using custom alphabet and optional strictness.
   *
   * @param buffer Base64 data
   * @param strict {@code true} to reject truncated input
   * @param char62 expected character for index 62
   * @param char63 expected character for index 63
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer, boolean strict, char char62, char char63)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), strict, char62, char63);
  }

  /**
   * Decodes standard Base64 from a stream.
   *
   * @param inputStream Base64 stream
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] decode (InputStream inputStream)
    throws IOException {

    return decode(inputStream, true, '+', '/');
  }

  /**
   * Decodes standard Base64 from a stream with optional strictness.
   *
   * @param inputStream Base64 stream
   * @param strict      {@code true} to reject truncated input
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] decode (InputStream inputStream, boolean strict)
    throws IOException {

    return decode(inputStream, strict, '+', '/');
  }

  /**
   * Decodes Base64 from a stream using custom alphabet with padding.
   *
   * @param inputStream Base64 stream
   * @param char62      expected character for index 62
   * @param char63      expected character for index 63
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] decode (InputStream inputStream, char char62, char char63)
    throws IOException {

    return decode(inputStream, true, char62, char63);
  }

  /**
   * Decodes Base64 from a stream using custom alphabet with optional strictness.
   *
   * @param inputStream Base64 stream
   * @param strict      {@code true} to reject truncated input
   * @param char62      expected character for index 62
   * @param char63      expected character for index 63
   * @return decoded bytes
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] decode (InputStream inputStream, boolean strict, char char62, char char63)
    throws IOException {

    ByteArrayOutputStream byteOutputStream;
    boolean endOfStream;
    byte[] buffer = new byte[4];
    byte[] quartet = new byte[4];

    byteOutputStream = new ByteArrayOutputStream();

    do {
      endOfStream = fillBuffer(inputStream, buffer, strict);
      if ((!endOfStream) || (buffer[0] != '=') || (buffer[1] != '=') || (buffer[2] != '=') || (buffer[3] != '=')) {
        if ((buffer[0] == '=') || (buffer[1] == '=') || ((buffer[2] == '=') && (buffer[3] != '='))) {
          throw new UnsupportedEncodingException("Not a base64 encoded stream");
        }

        for (int index = 0; index < buffer.length; index++) {
          if ((quartet[index] = (byte)indexOfBase64Bible(buffer[index], char62, char63)) < 0) {
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

  /**
   * Fills a buffer with up to four Base64 characters, optionally rejecting short reads.
   *
   * @param inputStream source stream
   * @param buffer      destination buffer (length 4)
   * @param strict      {@code true} to reject partial blocks
   * @return {@code true} if EOF encountered before filling the buffer
   * @throws IOException if reading fails or strict mode detects truncation
   */
  private static boolean fillBuffer (InputStream inputStream, byte[] buffer, boolean strict)
    throws IOException {

    int bytesRead;
    int offset = 0;

    do {
      bytesRead = inputStream.read(buffer, offset, buffer.length - offset);
    } while ((bytesRead >= 0) && ((offset += bytesRead) < buffer.length));

    if (strict && (offset > 0) && (offset < buffer.length)) {
      throw new UnsupportedEncodingException("Not a base64 encoded stream");
    }

    for (int index = offset; index < buffer.length; index++) {
      buffer[index] = '=';
    }

    return bytesRead < 0;
  }

  /**
   * Resolves the character for the given Base64 index, substituting padding and custom chars.
   *
   * @param index  6-bit index or 64 for padding
   * @param char62 substitution for index 62
   * @param char63 substitution for index 63
   * @return encoded character
   */
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

  /**
   * Resolves a Base64 index from an encoded character, honoring padding and custom alphabet.
   *
   * @param singleChar encoded character
   * @param char62     expected character for index 62
   * @param char63     expected character for index 63
   * @return decoded index or {@code -1} if not part of the alphabet
   */
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
