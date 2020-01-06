/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.web.reverse.http1_1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.io.ByteArrayIOStream;
import org.smallmind.scribe.pen.LoggerManager;

public class WebSocketFrameReader implements FrameReader {

  private enum State {ONE_BYTE, TWO_BYTES, EIGHT_BYTES, DATA}

  private final ReverseProxyService reverseProxyService;
  private final SocketChannel sourceChannel;
  private final SocketChannel originChannel;
  private final SocketChannel targetChannel;
  private final ByteArrayIOStream byteArrayIOStream = new ByteArrayIOStream();
  private final AtomicBoolean failed = new AtomicBoolean(false);
  private State state = State.ONE_BYTE;
  private boolean opCodeRead = false;
  private long dataLength;
  private long dataBytesRead = 0;
  private int lengthIndex = 0;
  private byte[] lengthArray = new byte[8];

  public WebSocketFrameReader (ReverseProxyService reverseProxyService, SocketChannel sourceChannel, SocketChannel originChannel, SocketChannel targetChannel) {

    this.reverseProxyService = reverseProxyService;
    this.sourceChannel = sourceChannel;
    this.originChannel = originChannel;
    this.targetChannel = targetChannel;
  }

  @Override
  public void closeChannels () {

    try {
      targetChannel.close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
    }

    try {
      originChannel.close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
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

      closeChannels();
    }
  }

  @Override
  public void processInput (SelectionKey selectionKey, ByteBuffer byteBuffer) {

    try {
      while (byteBuffer.remaining() > 0) {

        byte currentByte;

        byteArrayIOStream.asOutputStream().write(currentByte = byteBuffer.get());

        if (!opCodeRead) {
          opCodeRead = true;
        } else {
          switch (state) {
            case ONE_BYTE:

              byte lengthByte;

              lengthByte = (byte)(currentByte & 0x7F);
              if (lengthByte < 126) {
                dataLength = lengthByte;
                state = State.DATA;
              } else if (lengthByte == 126) {
                state = State.TWO_BYTES;
              } else {
                state = State.EIGHT_BYTES;
              }
              break;
            case TWO_BYTES:
              lengthArray[lengthIndex++] = currentByte;
              if (lengthIndex == 2) {
                dataLength = ((lengthArray[0] & 0xFF) << 8) + (lengthArray[1] & 0xFF);
                state = State.DATA;
              }
              break;
            case EIGHT_BYTES:
              lengthArray[lengthIndex++] = currentByte;
              if (lengthIndex == 8) {
                // largest array will never be more than 2^31-1
                dataLength = ((lengthArray[4] & 0xFF) << 24) + ((lengthArray[5] & 0xFF) << 16) + ((lengthArray[6] & 0xFF) << 8) + (lengthArray[7] & 0xFF);
                state = State.DATA;
              }
              break;
            case DATA:
              if (dataBytesRead++ == dataLength) {
                state = State.ONE_BYTE;
                lengthIndex = 0;
                dataBytesRead = 0;

                reverseProxyService.execute(sourceChannel, new FlushWorker(byteArrayIOStream.asInputStream().readAvailable()));
              }
              break;
          }
        }
      }
    } catch (IOException ioException) {
      fail(CannedResponse.BAD_REQUEST, null);
    }
  }

  private class FlushWorker implements Runnable {

    private byte[] buffer;

    public FlushWorker (byte[] buffer) {

      this.buffer = buffer;
    }

    @Override
    public void run () {

      try {
        LoggerManager.getLogger(HttpRequestFrameReader.class).debug(new ProxyDebug(buffer, 0, buffer.length));
        targetChannel.write(ByteBuffer.wrap(buffer));
      } catch (IOException ioException) {
        fail(CannedResponse.BAD_GATEWAY, targetChannel);
      }
    }
  }
}
