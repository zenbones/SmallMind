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
package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.transform.Transformer;
import org.smallmind.scribe.pen.LoggerManager;

public class EventLoop implements Runnable {

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final LinkedBlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
  private final SocketChannel socketChannel;
  private final Selector selector;
  private final SelectionKey selectionKey;

  public EventLoop (String host, int port)
    throws IOException {

    socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.socket().bind(new InetSocketAddress(host, port));

    selectionKey = socketChannel.register(selector = Selector.open(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
  }

  public void send (Command command) {

    synchronized (commandQueue) {
      commandQueue.offer(command);
      selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      selector.wakeup();
    }
  }

  public void stop ()
    throws InterruptedException {

    shutdown();
    terminationLatch.await();
  }

  private void shutdown () {

    finished.set(true);

    try {
      selectionKey.channel().close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(Transformer.class).error(ioException);
    }

    selectionKey.cancel();

    try {
      selector.close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(Transformer.class).error(ioException);
    }

    try {
      socketChannel.close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(Transformer.class).error(ioException);
    }
  }

  @Override
  public void run () {

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
    byte[] commandBuffer = null;
    int writeIndex = 0;

    try {
      while (!finished.get()) {
        try {
          if (selector.select(1000) > 0) {

            Iterator<SelectionKey> selectionKeyIter = selector.selectedKeys().iterator();

            while (selectionKeyIter.hasNext()) {

              SelectionKey selectionKey = selectionKeyIter.next();

              try {
                if (!selectionKey.isValid()) {
                  throw new InvalidSelectionKeyException();
                } else {
                  if (selectionKey.isReadable() && selectionKey.channel().isOpen()) {

                    StringBuilder lineBuilder = new StringBuilder();
                    boolean data = true;

                    byteBuffer.clear();
                    if (((SocketChannel)selectionKey.channel()).read(byteBuffer) > 0) {

                      char singleChar;

                      byteBuffer.flip();
                      do {
                        switch (singleChar = (char)byteBuffer.getInt()) {
                          case '\r':
                            if (!data) {
                              lineBuilder.append('\r');
                            }
                            data = false;
                            break;
                          case '\n':
                            if (data) {
                              lineBuilder.append('\n');
                            } else {
                              System.out.println(lineBuilder);
                              lineBuilder = new StringBuilder();
                              data = true;
                            }
                            break;
                          default:
                            lineBuilder.append(singleChar);
                        }
                      } while (byteBuffer.remaining() > 0);
                    }
                  }
                  if (selectionKey.isWritable() && selectionKey.channel().isOpen()) {

                    if (commandBuffer == null) {
                      synchronized (commandQueue) {

                        Command command;

                        if ((command = commandQueue.poll()) == null) {
                          selectionKey.interestOps(SelectionKey.OP_READ);
                        } else {
                          commandBuffer = command.toString().getBytes();
                        }
                      }
                    }

                    if (commandBuffer != null) {
                      byteBuffer.clear();
                      byteBuffer.put(commandBuffer, writeIndex, Math.min(byteBuffer.remaining(), commandBuffer.length - writeIndex));

                      byteBuffer.flip();
                      writeIndex += socketChannel.write(byteBuffer);

                      if (writeIndex == commandBuffer.length) {
                        commandBuffer = null;
                        writeIndex = 0;
                      }
                    }
                  }
                }
              } finally {
                selectionKeyIter.remove();
              }
            }
          }
        } catch (IOException ioException) {
          LoggerManager.getLogger(EventLoop.class).error(ioException);
        }
      }
    } finally {
      terminationLatch.countDown();
    }
  }
}
