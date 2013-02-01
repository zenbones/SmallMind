/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.plumber.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketChannelPipe {

  private SocketChannel downstreamSocketChannel;
  private SocketChannel upstreamSocketChannel;
  private ByteBuffer buffer;

  public SocketChannelPipe (SocketChannel downstreamSocketChannel, SocketChannel upstreamSocketChannel, int bufferSize) {

    this.downstreamSocketChannel = downstreamSocketChannel;
    this.upstreamSocketChannel = upstreamSocketChannel;

    buffer = ByteBuffer.allocate(bufferSize);
  }

  public void startPipe ()
    throws IOException {

    SocketChannel readySocketChannel;
    Selector readSelector;
    Set<SelectionKey> readyKeySet;
    Iterator<SelectionKey> readyKeyIter;
    SelectionKey readyKey;
    boolean downstreamOpen = true;
    boolean upstreamOpen = true;

    upstreamSocketChannel.configureBlocking(false);
    downstreamSocketChannel.configureBlocking(false);

    readSelector = Selector.open();

    upstreamSocketChannel.register(readSelector, SelectionKey.OP_READ);
    downstreamSocketChannel.register(readSelector, SelectionKey.OP_READ);

    while (downstreamOpen || upstreamOpen) {
      if (readSelector.select() > 0) {
        readyKeySet = readSelector.selectedKeys();
        readyKeyIter = readyKeySet.iterator();
        while (readyKeyIter.hasNext()) {
          readyKey = readyKeyIter.next();
          readyKeyIter.remove();
          readySocketChannel = (SocketChannel)readyKey.channel();

          if (downstreamOpen && (readySocketChannel == downstreamSocketChannel)) {
            downstreamOpen = transferBuffer(downstreamSocketChannel, upstreamSocketChannel);
          }
          else if (upstreamOpen) {
            upstreamOpen = transferBuffer(upstreamSocketChannel, downstreamSocketChannel);
          }
        }
      }
    }
  }

  private boolean transferBuffer (SocketChannel inChannel, SocketChannel outChannel)
    throws IOException {

    int totalBytes = 0;
    int bytesRead;

    while ((bytesRead = inChannel.read(buffer)) > 0) {
      totalBytes += bytesRead;
      buffer.flip();
      while (buffer.hasRemaining()) {
        outChannel.write(buffer);
      }
      buffer.clear();
    }

    if (totalBytes > 0) {
      return true;
    }
    else {
      inChannel.socket().shutdownInput();
      outChannel.socket().shutdownOutput();
      return false;
    }
  }

}
