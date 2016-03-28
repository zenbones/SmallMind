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

  private final ServerSocketChannel serverSocketChannel;
  private final Selector selector;
  private final ProxyDictionary dictionary;
  private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);

  public ReverseProxyService (String host, int port, ProxyDictionary dictionary)
    throws IOException {

    this.dictionary = dictionary;

    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.socket().bind(new InetSocketAddress(host, port));

    serverSocketChannel.register(selector = Selector.open(), SelectionKey.OP_ACCEPT);

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
    Thread eventThread;

    eventThread = new Thread(eventLoop = new EventLoop(terminationLatch));
    eventThread.setDaemon(true);
    eventThread.start();
  }

  private void internalError (SelectionKey selectionKey, SocketChannel sourceSocketChannel, CannedResponse cannedResponse) {

    try {
      sourceSocketChannel.write(cannedResponse.getByteBuffer());
      sourceSocketChannel.close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(ReverseProxyService.class).error(ioException);
    }

    closeSelectionKey(selectionKey);
  }

  private void closeSelectionKey (SelectionKey selectionKey) {

    selectionKey.cancel();
  }

  public void connectDestination (final SocketChannel sourceSocketChannel, final HttpFrameReader httpFrameReader, HttpRequest httpRequest)
    throws ProtocolException {

    ProxyTarget target;

    if ((target = dictionary.lookup(httpRequest)) == null) {
      throw new ProtocolException(sourceSocketChannel, CannedResponse.NOT_FOUND);
    } else {
      try {

        final AsynchronousSocketChannel destinationSocketChannel = AsynchronousSocketChannel.open();

        destinationSocketChannel.connect(new InetSocketAddress(target.getHost(), target.getPort()), null, new CompletionHandler<Void, Void>() {

          @Override
          public void completed (Void result, Void attachment) {

            try {
              httpFrameReader.registerDestination(destinationSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true).setOption(StandardSocketOptions.TCP_NODELAY, true));
            } catch (IOException ioException) {
              LoggerManager.getLogger(ReverseProxyService.class).error(ioException);
            }
          }

          @Override
          public void failed (Throwable throwable, Void attachment) {

            internalError(sourceSocketChannel.keyFor(selector), sourceSocketChannel, CannedResponse.NOT_FOUND);
          }
        });
      } catch (IOException ioException) {
        throw new ProtocolException(sourceSocketChannel, CannedResponse.NOT_FOUND);
      }
    }
  }

  private class EventLoop implements Runnable {

    final CountDownLatch terminationLatch;

    public EventLoop (CountDownLatch terminationLatch) {

      this.terminationLatch = terminationLatch;
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
                      ((ServerSocketChannel)selectionKey.channel()).accept().setOption(StandardSocketOptions.SO_KEEPALIVE, true).setOption(StandardSocketOptions.TCP_NODELAY, true).configureBlocking(false).register(selector, SelectionKey.OP_READ, new HttpConversation());
                    } else if (selectionKey.isReadable() && selectionKey.channel().isOpen()) {
                      byteBuffer.clear();
                      if (((SocketChannel)selectionKey.channel()).read(byteBuffer) > 0) {
                        byteBuffer.flip();
                        ((ProxyConversation)selectionKey.attachment()).getFrameReader().read(ReverseProxyService.this, (SocketChannel)selectionKey.channel(), byteBuffer);
                      }
                    } else if (selectionKey.isWritable() && selectionKey.channel().isOpen()) {
                      System.out.println("Write...");
                    }
                  } else {
                    closeSelectionKey(selectionKey);
                  }
                } catch (ProtocolException protocolException) {
                  internalError(selectionKey, protocolException.getSourceSocketChannel(), protocolException.getCannedResponse());
                } catch (Exception exception) {
                  closeSelectionKey(selectionKey);
                } finally {
                  selectionKeyIter.remove();
                }
              }
            }
          } catch (Exception exception) {
            exception.printStackTrace();
            LoggerManager.getLogger(ReverseProxyService.class).error(exception);
          }
        }
      } finally {
        terminationLatch.countDown();
      }
    }
  }
}

