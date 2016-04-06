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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.smallmind.scribe.pen.LoggerManager;

public class ReverseProxyService {

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ServerSocketChannel serverSocketChannel;
  private final Selector selector;
  private final ProxyDictionary dictionary;
  private final ProxyExecutor proxyExecutor;
  private final Lock selectLock = new ReentrantLock(true);
  private final Lock loopLock = new ReentrantLock(true);
  private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
  private final int connectTimeoutMillis;
  private EventLoop eventLoop;

  public ReverseProxyService (String host, int port, ProxyDictionary dictionary, int connectTimeoutMillis, int concurrencyLimit)
    throws IOException {

    this.dictionary = dictionary;
    this.connectTimeoutMillis = connectTimeoutMillis;

    proxyExecutor = new ProxyExecutor(concurrencyLimit);

    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.socket().bind(new InetSocketAddress(host, port));

    serverSocketChannel.register(selector = Selector.open(), SelectionKey.OP_ACCEPT);

    startEventLoop();
  }

  private void startEventLoop ()
    throws IOException {

    Thread eventThread;

    eventThread = new Thread(eventLoop = new EventLoop());
    eventThread.setDaemon(true);
    eventThread.start();
  }

  public void destroy ()
    throws IOException, InterruptedException {

    eventLoop.stop();

    if (closed.compareAndSet(false, true)) {
      selector.close();
      serverSocketChannel.close();
      proxyExecutor.shutdown();
    }
  }

  public void execute (SocketChannel sourceChannel, Runnable runnable) {

    proxyExecutor.execute(sourceChannel, runnable);
  }

  public ProxyTarget lookup (HttpRequestFrame httpRequestFrame)
    throws ProtocolException {

    ProxyTarget target;

    if ((target = dictionary.lookup(httpRequestFrame)) == null) {
      throw new ProtocolException(CannedResponse.NOT_FOUND);
    }

    return target;
  }

  public void connectDestination (final SocketChannel sourceSocketChannel, final HttpRequestFrameReader httpRequestFrameReader, final ProxyTarget target) {

    execute(sourceSocketChannel, new Runnable() {

      @Override
      public void run () {

        SocketChannel destinationChannel = null;

        try {
          destinationChannel = SocketChannel.open();
          destinationChannel.socket().connect(new InetSocketAddress(target.getHost(), target.getPort()), connectTimeoutMillis);
          destinationChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true).setOption(StandardSocketOptions.TCP_NODELAY, true).configureBlocking(false);

          loopLock.lock();
          try {
            selectLock.lock();
            try {
              destinationChannel.register(selector, SelectionKey.OP_READ, new HttpResponseFrameReader(ReverseProxyService.this, sourceSocketChannel, destinationChannel));
            } finally {
              selectLock.unlock();
            }
          } finally {
            loopLock.unlock();
          }

          httpRequestFrameReader.registerDestination(target, destinationChannel);
        } catch (IOException ioException) {
          try {
            if (destinationChannel != null) {
              destinationChannel.close();
            }
          } catch (IOException closeException) {
            LoggerManager.getLogger(ReverseProxyService.class).error(closeException);
          }

          httpRequestFrameReader.fail(CannedResponse.NOT_FOUND, null);
        }
      }
    });
  }

  private class EventLoop implements Runnable {

    private boolean stopped = false;

    public void stop ()
      throws InterruptedException {

      stopped = true;
      terminationLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!stopped) {
          try {

            int selectedKeyCount = 0;

            selectLock.lock();
            try {
              selectedKeyCount = selector.select(100);
            } catch (IOException ioException) {
              LoggerManager.getLogger(ReverseProxyService.class).error(ioException);
            } finally {
              selectLock.unlock();
            }

            if (selectedKeyCount > 0) {

              Iterator<SelectionKey> selectionKeyIter = selector.selectedKeys().iterator();

              loopLock.lock();
              try {
                while (selectionKeyIter.hasNext()) {

                  final SelectionKey selectionKey = selectionKeyIter.next();

                  try {
                    if (selectionKey.isValid()) {
                      if (selectionKey.isAcceptable()) {

                        SocketChannel sourceChannel = null;

                        try {
                          sourceChannel = (SocketChannel)((ServerSocketChannel)selectionKey.channel()).accept().setOption(StandardSocketOptions.SO_KEEPALIVE, true).setOption(StandardSocketOptions.TCP_NODELAY, true).configureBlocking(false);
                          sourceChannel.register(selector, SelectionKey.OP_READ, new HttpRequestFrameReader(ReverseProxyService.this, sourceChannel, connectTimeoutMillis));
                        } catch (IOException ioException) {
                          try {
                            if (sourceChannel != null) {
                              sourceChannel.close();
                            }
                          } catch (IOException closeException) {
                            LoggerManager.getLogger(ReverseProxyService.class).error(closeException);
                          }
                        }
                      } else if (selectionKey.isReadable() && selectionKey.channel().isOpen()) {

                        int bytesRead = 0;

                        try {
                          byteBuffer.clear();
                          bytesRead = ((SocketChannel)selectionKey.channel()).read(byteBuffer);
                        } catch (IOException ioException) {
                          ((FrameReader)selectionKey.attachment()).fail(CannedResponse.BAD_REQUEST, (SocketChannel)selectionKey.channel());
                        }

                        if (bytesRead > 0) {
                          byteBuffer.flip();
                          ((FrameReader)selectionKey.attachment()).processInput(selectionKey, byteBuffer);
                        } else if (bytesRead < 0) {
                          ((FrameReader)selectionKey.attachment()).closeChannels((SocketChannel)selectionKey.channel());
                        }
                      }
                    } else {
                      selectionKey.cancel();
                    }
                  } finally {
                    selectionKeyIter.remove();
                  }
                }
              } finally {
                loopLock.unlock();
              }
            }
          } catch (Exception exception) {
            LoggerManager.getLogger(ReverseProxyService.class).error(exception);
          }
        }
      } finally {
        terminationLatch.countDown();
      }
    }
  }
}

