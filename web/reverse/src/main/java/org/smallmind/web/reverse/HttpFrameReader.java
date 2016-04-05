/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.web.reverse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class HttpFrameReader implements FrameReader {

  private final ReverseProxyService reverseProxyService;
  private final SocketChannel sourceChannel;
  private final AtomicReference<ByteArrayOutputStream> outputStreamRef = new AtomicReference<>(new ByteArrayOutputStream());
  private final AtomicBoolean failed = new AtomicBoolean(false);
  private boolean lineEnd = false;
  private int lastChar = 0;
  private int writeIndex = 0;

  public HttpFrameReader (ReverseProxyService reverseProxyService, SocketChannel sourceChannel) {

    this.reverseProxyService = reverseProxyService;
    this.sourceChannel = sourceChannel;
  }

  public abstract void closeChannels (SocketChannel sourceChannel);

  public abstract SocketChannel getTargetChannel (SocketChannel sourceChannel);

  public abstract HttpFrame getHttpFrame (ReverseProxyService reverseProxyService, SocketChannel sourceSocketChannel, HttpProtocolInputStream httpProtocolInputStream)
    throws ProtocolException;

  @Override
  public void processInput (SelectionKey selectionKey, ByteBuffer byteBuffer) {

    try {
      while (byteBuffer.remaining() > 0) {

        byte currentChar;

        writeToBuffer(currentChar = byteBuffer.get());
        if ((currentChar == '\n') && (lastChar == '\r')) {
          if (lineEnd) {

            HttpFrame httpFrame = getHttpFrame(reverseProxyService, sourceChannel, new HttpProtocolInputStream(getBufferAsArray()));
            HttpHeader expectHeader;
            HttpHeader bodyHeader;

            if (((expectHeader = httpFrame.getHeader("Expect")) != null) && expectHeader.getValues().get(0).equals("100-continue")) {
              flushBufferToTarget(false);
            }

            if ((bodyHeader = httpFrame.getHeader("Content-Length")) != null) {

              int contentLength;

              try {
                if ((contentLength = Integer.parseInt(bodyHeader.getValues().get(0))) > 0) {

                  HttpContentLengthFrameReader httpContentLengthFrameReader;

                  selectionKey.attach(httpContentLengthFrameReader = new HttpContentLengthFrameReader(this, contentLength));
                  httpContentLengthFrameReader.processInput(selectionKey, byteBuffer);
                } else {
                  flushBufferToTarget(true);
                }
              } catch (NumberFormatException numberFormatException) {
                throw new ProtocolException(CannedResponse.LENGTH_REQUIRED);
              }
            } else if (((bodyHeader = httpFrame.getHeader("Transfer-Encoding")) != null) && bodyHeader.getValues().get(0).equals("chunked")) {

              HttpChunkedFrameReader httpChunkedFrameReader = new HttpChunkedFrameReader(this);

              selectionKey.attach(httpChunkedFrameReader);
              httpChunkedFrameReader.processInput(selectionKey, byteBuffer);
            } else {
              flushBufferToTarget(true);
            }
          }
          lineEnd = true;
        } else if (currentChar != '\r') {
          lineEnd = false;
        }

        lastChar = currentChar;
      }
    } catch (ProtocolException protocolException) {
      fail(protocolException.getCannedResponse(), null);
    }
  }

  public void clearBuffer () {

    synchronized (outputStreamRef) {
      outputStreamRef.set(new ByteArrayOutputStream());

      lineEnd = false;
      lastChar = 0;
      writeIndex = 0;
    }
  }

  public byte[] getBufferAsArray () {

    synchronized (outputStreamRef) {

      return outputStreamRef.get().toByteArray();
    }
  }

  public void writeToBuffer (byte singleByte) {

    synchronized (outputStreamRef) {
      outputStreamRef.get().write(singleByte);
    }
  }

  public void writeToBuffer (byte[] bytes) {

    writeToBuffer(bytes, 0, bytes.length);
  }

  public void writeToBuffer (byte[] bytes, int offset, int length) {

    synchronized (outputStreamRef) {
      outputStreamRef.get().write(bytes, offset, length);
    }
  }

  public synchronized void flushBufferToTarget (boolean complete) {

    if (!failed.get()) {
      synchronized (outputStreamRef) {

        byte[] buffer;

        reverseProxyService.execute(new FlushWorker(buffer = outputStreamRef.get().toByteArray(), writeIndex, buffer.length));
        if (complete) {
          clearBuffer();
        } else {
          writeIndex = buffer.length;
        }
      }
    }
  }

  @Override
  public void fail (CannedResponse cannedResponse, SocketChannel failedChannel) {

    if (failed.compareAndSet(false, true)) {
      if (!sourceChannel.equals(failedChannel)) {
        try {
          sourceChannel.write(cannedResponse.getByteBuffer());
        } catch (IOException ioException) {
          LoggerManager.getLogger(HttpFrameReader.class).error(ioException);
        }
      }

      closeChannels(sourceChannel);
    }
  }

  private class FlushWorker implements Runnable {

    private byte[] buffer;
    private int offset;
    private int length;

    public FlushWorker (byte[] buffer, int offset, int length) {

      this.buffer = buffer;
      this.offset = offset;
      this.length = length;
    }

    @Override
    public void run () {

      SocketChannel targetChannel;

      if ((targetChannel = getTargetChannel(sourceChannel)) == null) {
        fail(CannedResponse.GATEWAY_TIMEOUT, null);
      } else {
        try {
          LoggerManager.getLogger(HttpRequestFrameReader.class).debug(new ProxyDebug(buffer, offset, length));
          targetChannel.write(ByteBuffer.wrap(buffer, offset, length));
        } catch (IOException ioException) {
          fail(CannedResponse.BAD_GATEWAY, targetChannel);
        }
      }
    }
  }
}
