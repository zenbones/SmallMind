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
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.LoggerManager;

public class HttpRequestFrameReader implements FrameReader {

  private final ReverseProxyService reverseProxyService;
  private final SelectionKey selectionKey;
  private final SocketChannel sourceChannel;
  private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
  private final AtomicReference<SocketChannel> destinationChannelRef = new AtomicReference<>();
  private final AtomicBoolean failed = new AtomicBoolean(false);
  private boolean lineEnd = false;
  private int connectTimeoutMillis;
  private int lastChar = 0;
  private int writeIndex = 0;

  public HttpRequestFrameReader (ReverseProxyService reverseProxyService, SelectionKey selectionKey, SocketChannel sourceChannel, int connectTimeoutMillis) {

    this.reverseProxyService = reverseProxyService;
    this.selectionKey = selectionKey;
    this.sourceChannel = sourceChannel;
    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  public void read (ByteBuffer byteBuffer)
    throws ProtocolException {

    try {
      while (byteBuffer.remaining() > 0) {

        int currentChar;

        byteArrayOutputStream.write(currentChar = byteBuffer.get());

        if ((currentChar == '\n') && (lastChar == '\r')) {
          if (lineEnd) {

            HttpRequest httpRequest = new HttpRequest(sourceChannel, new HttpProtocolInputStream(byteArrayOutputStream.toByteArray()));
            HttpHeader contentLengthHeader;
            HttpHeader expectHeader;

            if (((expectHeader = httpRequest.getHeader("Expect")) != null) && expectHeader.getValues().get(0).equals("100-continue")) {
              flushBufferToDestination();
            }

            if ((contentLengthHeader = httpRequest.getHeader("Content-Length")) != null) {

              HttpContentLengthFrameReader httpContentLengthFrameReader;

              try {
                selectionKey.attach(httpContentLengthFrameReader = new HttpContentLengthFrameReader(this, byteArrayOutputStream, Integer.parseInt(contentLengthHeader.getValues().get(0))));
              } catch (NumberFormatException numberFormatException) {
                throw new ProtocolException(sourceChannel, CannedResponse.LENGTH_REQUIRED);
              }

              httpContentLengthFrameReader.read(byteBuffer);
            } else {

            }

            reverseProxyService.connectDestination(sourceChannel, this, httpRequest);
          }
          lineEnd = true;
        } else if (currentChar != '\r') {
          lineEnd = false;
        }

        lastChar = currentChar;
      }
    } catch (ProtocolException protocolException) {
      fail(protocolException);
    }
  }

  public synchronized void flushBufferToDestination () {

    if (!failed.get()) {

      final int startIndex = writeIndex;
      final int stopIndex = byteArrayOutputStream.size();

      writeIndex = stopIndex;

      reverseProxyService.execute(new Runnable() {

        @Override
        public void run () {

          SocketChannel destinationChannel = null;
          long start = System.currentTimeMillis();
          long elapsed;

          try {
            while (((elapsed = System.currentTimeMillis() - start) < connectTimeoutMillis) && ((destinationChannel = destinationChannelRef.get()) == null)) {
              synchronized (destinationChannelRef) {
                destinationChannelRef.wait(connectTimeoutMillis - elapsed);
              }
            }
          } catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(HttpRequestFrameReader.class).error(interruptedException);
          }

          if (destinationChannel == null) {
            fail(CannedResponse.GATEWAY_TIMEOUT);
          } else {
            try {
              System.out.println(new String(byteArrayOutputStream.toByteArray(), startIndex, stopIndex - startIndex));
              destinationChannel.write(ByteBuffer.wrap(byteArrayOutputStream.toByteArray(), startIndex, stopIndex - startIndex));
            } catch (IOException ioException) {
              fail(CannedResponse.BAD_GATEWAY);
            }
          }
        }
      });
    }
  }

  public synchronized void registerDestination (SocketChannel destinationChannel) {

    destinationChannelRef.set(destinationChannel);

    if (failed.get()) {
      closeDestination();
    } else {
      try {
        destinationChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true).setOption(StandardSocketOptions.TCP_NODELAY, true);

        synchronized (destinationChannelRef) {
          destinationChannelRef.notify();
        }
      } catch (IOException ioException) {
        fail(CannedResponse.BAD_GATEWAY);
      }
    }
  }

  public synchronized void fail (ProtocolException protocolException)
    throws ProtocolException {

    if (failed.compareAndSet(false, true)) {
      closeDestination();
    }

    throw protocolException;
  }

  public synchronized void fail (CannedResponse cannedResponse) {

    if (failed.compareAndSet(false, true)) {
      closeDestination();
      reverseProxyService.internalError(selectionKey, sourceChannel, cannedResponse);
    }
  }

  private void closeDestination () {

    try {

      SocketChannel destinationChannel;

      if ((destinationChannel = destinationChannelRef.get()) != null) {
        destinationChannel.close();
      }
    } catch (IOException ioException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
    }
  }
}
