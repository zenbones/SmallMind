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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

/**
 * Wraps a {@link Reader} as an {@link InputStream}, encoding characters into bytes as they are consumed. This is the inverse of {@link java.io.InputStreamReader}, which decodes bytes from an {@link InputStream} into characters.
 */
public class ReaderInputStream extends InputStream {

  private static final int DEFAULT_BUFFER_SIZE = 8192;

  private final Reader reader;
  private final CharsetEncoder encoder;
  private final CharBuffer charBuffer;
  private final ByteBuffer byteBuffer;

  private boolean endOfInput;
  private boolean endOfOutput;

  /**
   * Constructs a stream that encodes the given reader's characters using the platform's default charset.
   *
   * @param reader the character source to encode
   */
  public ReaderInputStream (Reader reader) {

    this(reader, Charset.defaultCharset());
  }

  /**
   * Constructs a stream that encodes the given reader's characters using the named charset.
   *
   * @param reader      the character source to encode
   * @param charsetName the name of the charset to encode with
   * @throws UnsupportedEncodingException if no charset is available for the given name
   */
  public ReaderInputStream (Reader reader, String charsetName)
    throws UnsupportedEncodingException {

    this(reader, forName(charsetName));
  }

  /**
   * Constructs a stream that encodes the given reader's characters using the given charset.
   *
   * @param reader  the character source to encode
   * @param charset the charset to encode with
   */
  public ReaderInputStream (Reader reader, Charset charset) {

    this(reader, charset, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructs a stream that encodes the given reader's characters using the given charset, buffering up to {@code bufferSize} characters at a time.
   *
   * @param reader     the character source to encode
   * @param charset    the charset to encode with
   * @param bufferSize the number of characters to buffer between reads of the underlying reader
   */
  public ReaderInputStream (Reader reader, Charset charset, int bufferSize) {

    this(reader, newEncoder(charset), bufferSize);
  }

  /**
   * Constructs a stream that encodes the given reader's characters using the given encoder.
   *
   * @param reader  the character source to encode
   * @param encoder the encoder used to convert characters into bytes
   */
  public ReaderInputStream (Reader reader, CharsetEncoder encoder) {

    this(reader, encoder, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructs a stream that encodes the given reader's characters using the given encoder, buffering up to {@code bufferSize} characters at a time.
   *
   * @param reader     the character source to encode
   * @param encoder    the encoder used to convert characters into bytes
   * @param bufferSize the number of characters to buffer between reads of the underlying reader
   * @throws IllegalArgumentException if {@code bufferSize} is not greater than 0
   */
  public ReaderInputStream (Reader reader, CharsetEncoder encoder, int bufferSize) {

    if (bufferSize <= 0) {
      throw new IllegalArgumentException("Buffer size must be greater than 0");
    }

    this.reader = reader;
    this.encoder = encoder;

    charBuffer = CharBuffer.allocate(bufferSize);
    byteBuffer = ByteBuffer.allocate((int)Math.ceil(bufferSize * (double)encoder.maxBytesPerChar()));

    charBuffer.limit(0);
    byteBuffer.limit(0);
  }

  private static Charset forName (String charsetName)
    throws UnsupportedEncodingException {

    try {

      return Charset.forName(charsetName);
    } catch (UnsupportedCharsetException unsupportedCharsetException) {
      throw new UnsupportedEncodingException(charsetName);
    }
  }

  private static CharsetEncoder newEncoder (Charset charset) {

    return charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  /**
   * Reads the next byte of the encoded character stream, or {@code -1} when the reader is exhausted.
   *
   * @return the next byte in the range 0–255, or {@code -1} if no bytes remain
   * @throws IOException if the underlying reader fails, or the encoder cannot represent a character
   */
  @Override
  public int read ()
    throws IOException {

    while (!byteBuffer.hasRemaining()) {
      if (!fillByteBuffer()) {
        return -1;
      }
    }

    return byteBuffer.get() & 0xFF;
  }

  /**
   * Reads up to {@code len} encoded bytes into the destination array.
   *
   * @param b   destination array
   * @param off starting index in {@code b}
   * @param len maximum number of bytes to transfer
   * @return the number of bytes actually read, or {@code -1} if the reader is exhausted
   * @throws IOException               if the underlying reader fails, or the encoder cannot represent a character
   * @throws IndexOutOfBoundsException if the offset or length are out of range for {@code b}
   */
  @Override
  public int read (byte[] b, int off, int len)
    throws IOException {

    Objects.checkFromIndexSize(off, len, b.length);

    if (len == 0) {
      return 0;
    }

    int totalRead = 0;

    while (totalRead < len) {

      if ((!byteBuffer.hasRemaining()) && (!fillByteBuffer())) {
        break;
      }

      int chunkLength;

      byteBuffer.get(b, off + totalRead, chunkLength = Math.min(len - totalRead, byteBuffer.remaining()));
      totalRead += chunkLength;
    }

    return (totalRead == 0) ? -1 : totalRead;
  }

  /**
   * Reports the number of already-encoded bytes immediately available without blocking on the underlying reader.
   *
   * @return the number of buffered bytes remaining to be read
   */
  @Override
  public int available () {

    return byteBuffer.remaining();
  }

  /**
   * Closes the underlying reader.
   *
   * @throws IOException if the underlying reader fails to close
   */
  @Override
  public void close ()
    throws IOException {

    reader.close();
  }

  private boolean fillByteBuffer ()
    throws IOException {

    if (endOfOutput) {
      return false;
    }

    byteBuffer.compact();

    try {
      if (!endOfInput) {

        charBuffer.compact();

        if (reader.read(charBuffer) < 0) {
          endOfInput = true;
        }

        charBuffer.flip();
      }

      CoderResult coderResult = encoder.encode(charBuffer, byteBuffer, endOfInput);

      if (coderResult.isError()) {
        coderResult.throwException();
      }

      if (endOfInput && (!coderResult.isOverflow())) {
        if (!(coderResult = encoder.flush(byteBuffer)).isOverflow()) {
          endOfOutput = true;
        }
        if (coderResult.isError()) {
          coderResult.throwException();
        }
      }
    } finally {
      byteBuffer.flip();
    }

    return true;
  }
}
