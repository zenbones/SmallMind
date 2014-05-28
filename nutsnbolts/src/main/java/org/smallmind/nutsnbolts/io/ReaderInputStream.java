/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class ReaderInputStream extends InputStream {

  private static final int DEFAULT_BUFFER_SIZE = 1024;

  private final Reader reader;
  private final CharsetEncoder encoder;
  private final CharBuffer encoderIn;
  private final ByteBuffer encoderOut = ByteBuffer.allocate(128);

  private CoderResult lastCoderResult;
  private boolean endOfInput;

  public ReaderInputStream (Reader reader) {

    this(reader, Charset.defaultCharset());
  }

  public ReaderInputStream (Reader reader, String charsetName) {

    this(reader, charsetName, DEFAULT_BUFFER_SIZE);
  }

  public ReaderInputStream (Reader reader, Charset charset) {

    this(reader, charset, DEFAULT_BUFFER_SIZE);
  }

  public ReaderInputStream (Reader reader, String charsetName, int bufferSize) {

    this(reader, Charset.forName(charsetName), bufferSize);
  }

  public ReaderInputStream (Reader reader, Charset charset, int bufferSize) {

    this.reader = reader;

    encoder = charset.newEncoder();
    encoderIn = CharBuffer.allocate(bufferSize);
    encoderIn.flip();
  }

  @Override
  public int read (byte[] b, int off, int len)
    throws IOException {

    int read = 0;
    while (len > 0) {
      if (encoderOut.position() > 0) {
        encoderOut.flip();
        int c = Math.min(encoderOut.remaining(), len);
        encoderOut.get(b, off, c);
        off += c;
        len -= c;
        read += c;
        encoderOut.compact();
      }
      else {
        if (!endOfInput && (lastCoderResult == null || lastCoderResult.isUnderflow())) {
          encoderIn.compact();
          int position = encoderIn.position();
          // We don't use Reader#read(CharBuffer) here because it is more efficient
          // to write directly to the underlying char array (the default implementation
          // copies data to a temporary char array).
          int c = reader.read(encoderIn.array(), position, encoderIn.remaining());
          if (c == -1) {
            endOfInput = true;
          }
          else {
            encoderIn.position(position + c);
          }
          encoderIn.flip();
        }
        lastCoderResult = encoder.encode(encoderIn, encoderOut, endOfInput);
        if (endOfInput && encoderOut.position() == 0) {
          break;
        }
      }
    }
    return read == 0 && endOfInput ? -1 : read;
  }

  @Override
  public int read (byte[] b)
    throws IOException {

    return read(b, 0, b.length);
  }

  @Override
  public int read ()
    throws IOException {

    byte[] b = new byte[1];
    return read(b) == -1 ? -1 : b[0] & 0xFF;
  }

  @Override
  public void close ()
    throws IOException {

    reader.close();
  }
}
