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
 * Provides Base64 encoding and decoding for strings, byte arrays, {@link ByteBuffer}s, and streams,
 * supporting standard, URL-safe, and custom alphabets with optional padding.
 */
public final class Base64Codec {

  private static final String BASE64_BIBLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  /**
   * Encodes the UTF-8 bytes of a string to URL-safe Base64 ({@code -} and {@code _}) without padding.
   *
   * @param original the string to encode
   * @return URL-safe Base64 string
   * @throws IOException if the input cannot be read
   */
  public static String urlSafeEncode (String original)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), false, '-', '_');
  }

  /**
   * Encodes a byte array to URL-safe Base64 ({@code -} and {@code _}) without padding.
   *
   * @param bytes the data to encode
   * @return URL-safe Base64 string
   * @throws IOException if the input cannot be read
   */
  public static String urlSafeEncode (byte[] bytes)
    throws IOException {

    return encode(bytes, false, '-', '_');
  }

  /**
   * Encodes the remaining bytes of a {@link ByteBuffer} to URL-safe Base64 without padding.
   *
   * @param buffer the buffer to encode; consumed from its current position to its limit
   * @return URL-safe Base64 string
   * @throws IOException if the input cannot be read
   */
  public static String urlSafeEncode (ByteBuffer buffer)
    throws IOException {

    return encode(buffer, false, '-', '_');
  }

  /**
   * Reads all bytes from the stream and encodes them to URL-safe Base64 without padding.
   *
   * @param inputStream the stream to fully consume and encode
   * @return URL-safe Base64 string
   * @throws IOException if reading from the stream fails
   */
  public static String urlSafeEncode (InputStream inputStream)
    throws IOException {

    return encode(inputStream, false, '-', '_');
  }

  /**
   * Encodes the UTF-8 bytes of a string to standard Base64 with {@code =} padding.
   *
   * @param original the string to encode
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), true, '+', '/');
  }

  /**
   * Encodes the UTF-8 bytes of a string to standard Base64 with optional padding.
   *
   * @param original       the string to encode
   * @param includePadding {@code true} to append {@code =} padding characters
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original, boolean includePadding)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), includePadding, '+', '/');
  }

  /**
   * Encodes the UTF-8 bytes of a string to Base64 using custom characters at positions 62 and 63, with padding.
   *
   * @param original the string to encode
   * @param char62   character to use for index 62 in place of {@code +}
   * @param char63   character to use for index 63 in place of {@code /}
   * @return Base64 string using the specified alphabet
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original, char char62, char char63)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), true, char62, char63);
  }

  /**
   * Encodes the UTF-8 bytes of a string to Base64 using a custom alphabet and optional padding.
   *
   * @param original       the string to encode
   * @param includePadding {@code true} to append {@code =} padding characters
   * @param char62         character to use for index 62 in place of {@code +}
   * @param char63         character to use for index 63 in place of {@code /}
   * @return Base64 string using the specified alphabet
   * @throws IOException if the input cannot be read
   */
  public static String encode (String original, boolean includePadding, char char62, char char63)
    throws IOException {

    return encode(original.getBytes(StandardCharsets.UTF_8), includePadding, char62, char63);
  }

  /**
   * Encodes a byte array to standard Base64 with {@code =} padding.
   *
   * @param bytes the data to encode
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (byte[] bytes)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), true, '+', '/');
  }

  /**
   * Encodes a byte array to standard Base64 with optional padding.
   *
   * @param bytes          the data to encode
   * @param includePadding {@code true} to append {@code =} padding characters
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (byte[] bytes, boolean includePadding)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), includePadding, '+', '/');
  }

  /**
   * Encodes a byte array to Base64 using custom characters at positions 62 and 63, with padding.
   *
   * @param bytes  the data to encode
   * @param char62 character to use for index 62
   * @param char63 character to use for index 63
   * @return Base64 string using the specified alphabet
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (byte[] bytes, char char62, char char63)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), true, char62, char63);
  }

  /**
   * Encodes a byte array to Base64 using a custom alphabet and optional padding.
   *
   * @param bytes          the data to encode
   * @param includePadding {@code true} to append {@code =} padding characters
   * @param char62         character to use for index 62
   * @param char63         character to use for index 63
   * @return Base64 string using the specified alphabet
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (byte[] bytes, boolean includePadding, char char62, char char63)
    throws IOException {

    return encode(new ByteArrayInputStream(bytes), includePadding, char62, char63);
  }

  /**
   * Encodes the remaining bytes of a {@link ByteBuffer} to standard Base64 with {@code =} padding.
   *
   * @param buffer the buffer to encode; consumed from its current position to its limit
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (ByteBuffer buffer)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), true, '+', '/');
  }

  /**
   * Encodes the remaining bytes of a {@link ByteBuffer} to standard Base64 with optional padding.
   *
   * @param buffer         the buffer to encode; consumed from its current position to its limit
   * @param includePadding {@code true} to append {@code =} padding characters
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (ByteBuffer buffer, boolean includePadding)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), includePadding, '+', '/');
  }

  /**
   * Encodes the remaining bytes of a {@link ByteBuffer} to Base64 with custom characters at positions 62 and 63, with padding.
   *
   * @param buffer the buffer to encode; consumed from its current position to its limit
   * @param char62 character to use for index 62
   * @param char63 character to use for index 63
   * @return Base64 string using the specified alphabet
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (ByteBuffer buffer, char char62, char char63)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), true, char62, char63);
  }

  /**
   * Encodes the remaining bytes of a {@link ByteBuffer} to Base64 using a custom alphabet and optional padding.
   *
   * @param buffer         the buffer to encode; consumed from its current position to its limit
   * @param includePadding {@code true} to append {@code =} padding characters
   * @param char62         character to use for index 62
   * @param char63         character to use for index 63
   * @return Base64 string using the specified alphabet
   * @throws IOException if an I/O error occurs internally
   */
  public static String encode (ByteBuffer buffer, boolean includePadding, char char62, char char63)
    throws IOException {

    return encode(new ByteBufferInputStream(buffer), includePadding, char62, char63);
  }

  /**
   * Reads all bytes from a stream and encodes them to standard Base64 with {@code =} padding.
   *
   * @param inputStream the stream to fully consume and encode
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if reading from the stream fails
   */
  public static String encode (InputStream inputStream)
    throws IOException {

    return encode(inputStream, true, '+', '/');
  }

  /**
   * Reads all bytes from a stream and encodes them to standard Base64 with optional padding.
   *
   * @param inputStream    the stream to fully consume and encode
   * @param includePadding {@code true} to append {@code =} padding characters
   * @return standard Base64 string using {@code +} and {@code /}
   * @throws IOException if reading from the stream fails
   */
  public static String encode (InputStream inputStream, boolean includePadding)
    throws IOException {

    return encode(inputStream, includePadding, '+', '/');
  }

  /**
   * Reads all bytes from a stream and encodes them to Base64 with custom characters at positions 62 and 63, with padding.
   *
   * @param inputStream the stream to fully consume and encode
   * @param char62      character to use for index 62
   * @param char63      character to use for index 63
   * @return Base64 string using the specified alphabet
   * @throws IOException if reading from the stream fails
   */
  public static String encode (InputStream inputStream, char char62, char char63)
    throws IOException {

    return encode(inputStream, true, char62, char63);
  }

  /**
   * Reads all bytes from a stream and encodes them to Base64 using a custom alphabet and optional padding.
   *
   * @param inputStream    the stream to fully consume and encode
   * @param includePadding {@code true} to append {@code =} padding characters
   * @param char62         character to use for index 62
   * @param char63         character to use for index 63
   * @return Base64 string using the specified alphabet
   * @throws IOException if reading from the stream fails
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
   * Decodes a URL-safe Base64 string ({@code -} and {@code _}, no padding) back to raw bytes.
   *
   * @param encoded the URL-safe Base64 string to decode
   * @return decoded byte array
   * @throws IOException if the string is not valid Base64
   */
  public static byte[] urlSafeDecode (String encoded)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), false, '-', '_');
  }

  /**
   * Decodes URL-safe Base64 bytes ({@code -} and {@code _}, no padding) back to raw bytes.
   *
   * @param bytes the URL-safe Base64-encoded byte array
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] urlSafeDecode (byte[] bytes)
    throws IOException {

    return decode(bytes, false, '-', '_');
  }

  /**
   * Reads and decodes URL-safe Base64 ({@code -} and {@code _}, no padding) from a stream.
   *
   * @param inputStream the stream of URL-safe Base64 data to decode
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] urlSafeDecode (InputStream inputStream)
    throws IOException {

    return decode(inputStream, false, '-', '_');
  }

  /**
   * Decodes a standard Base64 string ({@code +} and {@code /} with {@code =} padding) to raw bytes.
   *
   * @param encoded the Base64-encoded string
   * @return decoded byte array
   * @throws IOException if the string is not valid Base64
   */
  public static byte[] decode (String encoded)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), true, '+', '/');
  }

  /**
   * Decodes a standard Base64 string with configurable strictness for truncated input.
   *
   * @param encoded the Base64-encoded string using {@code +} and {@code /}
   * @param strict  {@code true} to throw when the input length is not a multiple of 4
   * @return decoded byte array
   * @throws IOException if the string is not valid Base64
   */
  public static byte[] decode (String encoded, boolean strict)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), strict, '+', '/');
  }

  /**
   * Decodes a Base64 string that uses custom characters at positions 62 and 63.
   *
   * @param encoded the Base64-encoded string
   * @param char62  character used for index 62 during encoding
   * @param char63  character used for index 63 during encoding
   * @return decoded byte array
   * @throws IOException if the string is not valid Base64
   */
  public static byte[] decode (String encoded, char char62, char char63)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), true, char62, char63);
  }

  /**
   * Decodes a Base64 string that uses a custom alphabet, with configurable strictness.
   *
   * @param encoded the Base64-encoded string
   * @param strict  {@code true} to throw when the input length is not a multiple of 4
   * @param char62  character used for index 62 during encoding
   * @param char63  character used for index 63 during encoding
   * @return decoded byte array
   * @throws IOException if the string is not valid Base64
   */
  public static byte[] decode (String encoded, boolean strict, char char62, char char63)
    throws IOException {

    return decode(encoded.getBytes(StandardCharsets.UTF_8), strict, char62, char63);
  }

  /**
   * Decodes a standard Base64-encoded byte array to raw bytes.
   *
   * @param bytes the Base64-encoded data
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), true, '+', '/');
  }

  /**
   * Decodes a standard Base64-encoded byte array with configurable strictness.
   *
   * @param bytes  the Base64-encoded data
   * @param strict {@code true} to throw when the input length is not a multiple of 4
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes, boolean strict)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), strict, '+', '/');
  }

  /**
   * Decodes a Base64-encoded byte array that uses custom characters at positions 62 and 63.
   *
   * @param bytes  the Base64-encoded data
   * @param char62 character used for index 62 during encoding
   * @param char63 character used for index 63 during encoding
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes, char char62, char char63)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), true, char62, char63);
  }

  /**
   * Decodes a Base64-encoded byte array using a custom alphabet, with configurable strictness.
   *
   * @param bytes  the Base64-encoded data
   * @param strict {@code true} to throw when the input length is not a multiple of 4
   * @param char62 character used for index 62 during encoding
   * @param char63 character used for index 63 during encoding
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (byte[] bytes, boolean strict, char char62, char char63)
    throws IOException {

    return decode(new ByteArrayInputStream(bytes), strict, char62, char63);
  }

  /**
   * Decodes standard Base64 from the remaining bytes of a {@link ByteBuffer}.
   *
   * @param buffer the buffer containing Base64-encoded data
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), true, '+', '/');
  }

  /**
   * Decodes standard Base64 from the remaining bytes of a {@link ByteBuffer} with configurable strictness.
   *
   * @param buffer the buffer containing Base64-encoded data
   * @param strict {@code true} to throw when the input length is not a multiple of 4
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer, boolean strict)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), strict, '+', '/');
  }

  /**
   * Decodes Base64 from a {@link ByteBuffer} using custom characters at positions 62 and 63.
   *
   * @param buffer the buffer containing Base64-encoded data
   * @param char62 character used for index 62 during encoding
   * @param char63 character used for index 63 during encoding
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer, char char62, char char63)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), true, char62, char63);
  }

  /**
   * Decodes Base64 from a {@link ByteBuffer} using a custom alphabet and configurable strictness.
   *
   * @param buffer the buffer containing Base64-encoded data
   * @param strict {@code true} to throw when the input length is not a multiple of 4
   * @param char62 character used for index 62 during encoding
   * @param char63 character used for index 63 during encoding
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64
   */
  public static byte[] decode (ByteBuffer buffer, boolean strict, char char62, char char63)
    throws IOException {

    return decode(new ByteBufferInputStream(buffer), strict, char62, char63);
  }

  /**
   * Reads and decodes standard Base64 from a stream.
   *
   * @param inputStream the stream of Base64-encoded data
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] decode (InputStream inputStream)
    throws IOException {

    return decode(inputStream, true, '+', '/');
  }

  /**
   * Reads and decodes standard Base64 from a stream with configurable strictness.
   *
   * @param inputStream the stream of Base64-encoded data
   * @param strict      {@code true} to throw when the input length is not a multiple of 4
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] decode (InputStream inputStream, boolean strict)
    throws IOException {

    return decode(inputStream, strict, '+', '/');
  }

  /**
   * Reads and decodes Base64 from a stream using custom characters at positions 62 and 63.
   *
   * @param inputStream the stream of Base64-encoded data
   * @param char62      character used for index 62 during encoding
   * @param char63      character used for index 63 during encoding
   * @return decoded byte array
   * @throws IOException if the data is not valid Base64 or reading fails
   */
  public static byte[] decode (InputStream inputStream, char char62, char char63)
    throws IOException {

    return decode(inputStream, true, char62, char63);
  }

  /**
   * Reads and decodes Base64 from a stream using a custom alphabet and configurable strictness.
   *
   * @param inputStream the stream of Base64-encoded data
   * @param strict      {@code true} to throw when the input length is not a multiple of 4
   * @param char62      character used for index 62 during encoding
   * @param char63      character used for index 63 during encoding
   * @return decoded byte array
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
   * Reads exactly four bytes into {@code buffer} from the stream, padding with {@code =} on EOF
   * and optionally rejecting partial blocks.
   *
   * @param inputStream source stream to read from
   * @param buffer      four-byte destination buffer
   * @param strict      {@code true} to throw if a partial block (1–3 bytes) is found at end of input
   * @return {@code true} if end-of-stream was reached during or before this read
   * @throws IOException if reading fails or a partial block is detected in strict mode
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
   * Maps a six-bit Base64 index to its corresponding character in the configured alphabet.
   *
   * @param index  value 0–63 or 64 to emit the padding character {@code =}
   * @param char62 character representing index 62
   * @param char63 character representing index 63
   * @return the encoded Base64 character
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
   * Finds the six-bit index for an encoded Base64 character in the configured alphabet.
   *
   * @param singleChar the encoded byte to look up
   * @param char62     character representing index 62 in this alphabet
   * @param char63     character representing index 63 in this alphabet
   * @return the decoded index (0–64 where 64 represents padding), or {@code -1} if not in the alphabet
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
