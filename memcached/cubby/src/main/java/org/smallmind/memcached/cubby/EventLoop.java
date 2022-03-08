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
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.transform.Transformer;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.scribe.pen.LoggerManager;

public class EventLoop implements Runnable {

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final SelfDestructiveMap<String, RequestCallback> callbackMap = new SelfDestructiveMap<>(new Stint(3, TimeUnit.SECONDS), new Stint(100, TimeUnit.MILLISECONDS));
  private final LinkedBlockingQueue<byte[]> requestQueue = new LinkedBlockingQueue<>();
  private final TokenGenerator tokenGenerator = new TokenGenerator();
  private final SocketChannel socketChannel;
  private final Selector selector;
  private final SelectionKey selectionKey;
  private final Stint defaultTimeoutStint;

  public EventLoop (String host, int port, long connectionTimeout, long defaultRequestTimeout)
    throws IOException, InterruptedException {

    defaultTimeoutStint = new Stint(defaultRequestTimeout, TimeUnit.MILLISECONDS);

    long start = System.currentTimeMillis();

    socketChannel = SocketChannel.open()
      .setOption(StandardSocketOptions.SO_KEEPALIVE, true)
      .setOption(StandardSocketOptions.TCP_NODELAY, true);
    socketChannel.configureBlocking(false);
    socketChannel.connect(new InetSocketAddress(host, port));

    while ((!socketChannel.finishConnect()) && (System.currentTimeMillis() - start) < connectionTimeout) {
      Thread.sleep(100);
    }

    if (socketChannel.isConnectionPending()) {
      throw new ConnectionTimeoutException();
    }

    selectionKey = socketChannel.register(selector = Selector.open(), SelectionKey.OP_WRITE);
  }

  public Response send (Command command, Long timeout)
    throws InterruptedException, IOException {

    RequestCallback requestCallback;
    String opaqueToken;

    callbackMap.putIfAbsent(opaqueToken = tokenGenerator.next(), requestCallback = new RequestCallback(command), (timeout == null) ? defaultTimeoutStint : new Stint(timeout, TimeUnit.MILLISECONDS));

    synchronized (requestQueue) {
      requestQueue.offer(command.construct(opaqueToken));
      selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      selector.wakeup();
    }

    return requestCallback.getResult();
  }

  public void stop ()
    throws InterruptedException {

    shutdown();
    terminationLatch.await();
  }

  private void shutdown () {

    finished.set(true);
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
    ResponseReader responseReader = new ResponseReader();
    RequestWriter requestWriter = null;

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
                    byteBuffer.clear();
                    if (((SocketChannel)selectionKey.channel()).read(byteBuffer) > 0) {
                      byteBuffer.flip();
                      do {

                        Response response;

                        if ((response = responseReader.read(byteBuffer)) != null) {

                          RequestCallback requestCallback;

                          if ((response.getToken() != null) && ((requestCallback = callbackMap.get(response.getToken())) != null)) {
                            requestCallback.setResult(response);
                          }
                        }
                      } while (byteBuffer.remaining() > 0);
                    }
                  }
                  if (selectionKey.isWritable() && selectionKey.channel().isOpen()) {

                    int totalBytesWritten = 0;
                    boolean complete = true;

                    do {
                      if (requestWriter == null) {
                        synchronized (requestQueue) {

                          byte[] request;

                          if ((request = requestQueue.poll()) == null) {
                            complete = false;
                            selectionKey.interestOps(SelectionKey.OP_READ);
                          } else {
                            requestWriter = new RequestWriter(request);
                          }
                        }
                      }
                      if (requestWriter != null) {

                        int bytesWritten;

                        if ((bytesWritten = requestWriter.write(socketChannel, byteBuffer)) > 0) {
                          requestWriter = null;
                        } else {
                          complete = false;
                        }

                        totalBytesWritten += Math.abs(bytesWritten);
                      }
                    } while (complete && (totalBytesWritten < byteBuffer.capacity()));
                  }
                }
              } finally {
                selectionKeyIter.remove();
              }
            }
          }
        } catch (IOException ioException) {
          LoggerManager.getLogger(EventLoop.class).error(ioException);
          shutdown();
        }
      }
    } finally {
      terminationLatch.countDown();
    }
  }
}
