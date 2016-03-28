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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import org.smallmind.scribe.pen.LoggerManager;

public class ReverseProxyService {

  private static final ByteBuffer NOT_FOUND_BUFFER = ByteBuffer.wrap("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
  private final ServerSocketChannel serverSocketChannel;
  private final ProxyDictionary dictionary;
  private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);

  public ReverseProxyService (String host, int port, ProxyDictionary dictionary)
    throws IOException {

    this.dictionary = dictionary;

    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.socket().bind(new InetSocketAddress(host, port));

    startEventLoop();
  }

  public static void main (String... args)
    throws Exception {

    CountDownLatch cdl = new CountDownLatch(1);
    ReverseProxyService reverseProxyService = new ReverseProxyService("0.0.0.0", 9030, null);

    cdl.await();
  }

  private void startEventLoop ()
    throws IOException {

    CountDownLatch terminationLatch = new CountDownLatch(1);
    EventLoop eventLoop;
    Selector selector;
    Thread eventThread;

    serverSocketChannel.register(selector = Selector.open(), SelectionKey.OP_ACCEPT);

    eventThread = new Thread(eventLoop = new EventLoop(terminationLatch, selector));
    eventThread.setDaemon(true);
    eventThread.start();
  }

  private class EventLoop implements Runnable {

    CountDownLatch terminationLatch;
    Selector selector;

    public EventLoop (CountDownLatch terminationLatch, Selector selector) {

      this.terminationLatch = terminationLatch;
      this.selector = selector;
    }

    private void closeSelectionKey (SelectionKey selectionKey, boolean foob) {

      selectionKey.cancel();
    }

    @Override
    public void run () {

      CountDownLatch terminationLatch = new CountDownLatch(1);
      boolean stopped = false;

      try {
        while (!stopped) {
          try {
            if (selector.select(1000) > 0) {

              Iterator<SelectionKey> selectionKeyIter = selector.selectedKeys().iterator();

              while (selectionKeyIter.hasNext()) {

                final SelectionKey selectionKey = selectionKeyIter.next();

                try {
                  if (selectionKey.isValid()) {
                    if (selectionKey.isAcceptable()) {

                      final SocketChannel sourceSocketChannel = (SocketChannel)((ServerSocketChannel)selectionKey.channel()).accept().setOption(StandardSocketOptions.SO_KEEPALIVE, true).setOption(StandardSocketOptions.TCP_NODELAY, true).configureBlocking(false);
                      final AsynchronousSocketChannel destinationSocketChannel = AsynchronousSocketChannel.open();

                      sourceSocketChannel.register(selector, SelectionKey.OP_READ, new HttpConversation());
                      destinationSocketChannel.connect(new InetSocketAddress("www.google.com", 80), null, new CompletionHandler<Void, Void>() {

                        @Override
                        public void completed (Void result, Void attachment) {

                          try {
                            destinationSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true).setOption(StandardSocketOptions.TCP_NODELAY, true);
                          } catch (IOException ioException) {
                            LoggerManager.getLogger(ReverseProxyService.class).error(ioException);
                          }
                        }

                        @Override
                        public void failed (Throwable exc, Void attachment) {

                          try {
                            sourceSocketChannel.write(NOT_FOUND_BUFFER);
                            sourceSocketChannel.close();
                          } catch (IOException ioException) {
                            LoggerManager.getLogger(ReverseProxyService.class).error(ioException);
                          }
                        }
                      });
                    } else if (selectionKey.isReadable() && selectionKey.channel().isOpen()) {
                      byteBuffer.clear();
                      if (((SocketChannel)selectionKey.channel()).read(byteBuffer) > 0) {
                        byteBuffer.flip();
                        ((ProxyConversation)selectionKey.attachment()).getFrameReader().read(byteBuffer);
                      }
                    } else if (selectionKey.isWritable() && selectionKey.channel().isOpen()) {
                      System.out.println("Write...");
                    }
                  } else {
                    closeSelectionKey(selectionKey, true);
                  }
                } catch (Exception exception) {
                  closeSelectionKey(selectionKey, true);
                } finally {
                  selectionKeyIter.remove();
                }
              }
            }
          } catch (IOException exception) {
            LoggerManager.getLogger(ReverseProxyService.class).error(exception);
          }
        }
      } finally {
        terminationLatch.countDown();
      }
    }
  }
}

