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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class HttpChunkedFrameReader implements FrameReader {

  private enum State {CHUNK, EXTENSION, END_CHUNK, DATA}

  private static final String LEGAL_HEXADECIMAL = "0123456789ABCDEFabcdef";
  private final HttpFrameReader httpFrameReader;
  private State state = State.CHUNK;
  private boolean last = false;
  private boolean finished = false;
  private int lastChar = 0;
  private int index = 0;
  private byte[] chunkArray = new byte[8];

  public HttpChunkedFrameReader (HttpFrameReader httpFrameReader) {

    this.httpFrameReader = httpFrameReader;
  }

  @Override
  public void closeChannels (SocketChannel sourceChannel) {

    httpFrameReader.closeChannels(sourceChannel);
  }

  @Override
  public void fail (CannedResponse cannedResponse, SocketChannel failedChannel) {

    httpFrameReader.fail(cannedResponse, failedChannel);
  }

  public void processInput (SelectionKey selectionKey, ByteBuffer byteBuffer) {

    try {
      while ((!finished) && (byteBuffer.remaining() > 0)) {

        byte currentChar;

        httpFrameReader.writeToBuffer(currentChar = byteBuffer.get());

        switch (state) {
          case CHUNK:
            if (LEGAL_HEXADECIMAL.indexOf(currentChar) >= 0) {
              if (index == chunkArray.length) {
                throw new ProtocolException(CannedResponse.BAD_REQUEST);
              }
              chunkArray[index++] = currentChar;
            } else if (currentChar == ';') {
              if (index == 0) {
                throw new ProtocolException(CannedResponse.BAD_REQUEST);
              }
              state = State.EXTENSION;
            } else if (currentChar == '\r') {
              if (index == 0) {
                throw new ProtocolException(CannedResponse.BAD_REQUEST);
              }
              state = State.END_CHUNK;
            } else {
              throw new ProtocolException(CannedResponse.BAD_REQUEST);
            }
            break;
          case EXTENSION:
            if ((currentChar == '\n') && (lastChar == '\r')) {
              try {
                last = (index = Integer.parseInt(new String(chunkArray, 0, index), 16)) == 0;
              } catch (NumberFormatException numberFormatException) {
                throw new ProtocolException(CannedResponse.BAD_REQUEST);
              }

              state = State.DATA;
            }
            break;
          case END_CHUNK:
            if (currentChar == '\n') {
              try {
                last = (index = Integer.parseInt(new String(chunkArray, 0, index), 16)) == 0;
              } catch (NumberFormatException numberFormatException) {
                throw new ProtocolException(CannedResponse.BAD_REQUEST);
              }

              state = State.DATA;
            } else {
              throw new ProtocolException(CannedResponse.BAD_REQUEST);
            }
            break;
          case DATA:
            index -= 1;
            if ((index == -1) && (currentChar != '\r')) {
              throw new ProtocolException(CannedResponse.BAD_REQUEST);
            } else if (index == -2) {
              if (currentChar != '\n') {
                throw new ProtocolException(CannedResponse.BAD_REQUEST);
              } else {
                if (last) {
                  httpFrameReader.flushBufferToTarget(true);
                  selectionKey.attach(httpFrameReader);
                  finished = true;
                } else {
                  state = State.CHUNK;
                  index = 0;
                }
              }
            }
            break;
          default:
            throw new ProtocolException(CannedResponse.BAD_GATEWAY);
        }

        lastChar = currentChar;
      }
    } catch (IOException ioException) {
      fail(CannedResponse.BAD_REQUEST, null);
    } catch (ProtocolException protocolException) {
      fail(protocolException.getCannedResponse(), null);
    }
  }
}
