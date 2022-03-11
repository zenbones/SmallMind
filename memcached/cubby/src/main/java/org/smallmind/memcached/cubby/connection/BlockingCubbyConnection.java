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
package org.smallmind.memcached.cubby.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.memcached.cubby.ConnectionCoordinator;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.response.ResponseParser;
import org.smallmind.memcached.cubby.response.ServerResponse;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.scribe.pen.LoggerManager;

public class BlockingCubbyConnection implements CubbyConnection {

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final ConnectionCoordinator connectionCoordinator;
  private final MemcachedHost memcachedHost;
  private final KeyTranslator keyTranslator;
  private final CubbyCodec codec;
  private final LinkedBlockingQueue<CommandBuffer> requestQueue = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<CommandBuffer> responseQueue = new LinkedBlockingQueue<>();
  private final AtomicLong commandCounter = new AtomicLong(0);
  private final long connectionTimeoutMilliseconds;
  private final long defaultRequestTimeoutSeconds;
  private Socket socket;
  private SelfDestructiveMap<Long, RequestCallback> callbackMap;

  public BlockingCubbyConnection (ConnectionCoordinator connectionCoordinator, CubbyConfiguration configuration, MemcachedHost memcachedHost) {

    this.connectionCoordinator = connectionCoordinator;
    this.memcachedHost = memcachedHost;

    keyTranslator = configuration.getKeyTranslator();
    codec = configuration.getCodec();
    connectionTimeoutMilliseconds = configuration.getConnectionTimeoutMilliseconds();
    defaultRequestTimeoutSeconds = configuration.getDefaultRequestTimeoutSeconds();
  }

  @Override
  public void start ()
    throws IOException {

    socket = new Socket(((InetSocketAddress)memcachedHost.getAddress()).getHostName(), ((InetSocketAddress)memcachedHost.getAddress()).getPort());
    callbackMap = new SelfDestructiveMap<>(new Stint(defaultRequestTimeoutSeconds, TimeUnit.SECONDS), new Stint(100, TimeUnit.MILLISECONDS));

    requestQueue.clear();
    responseQueue.clear();
    commandCounter.set(0L);
  }

  @Override
  public void stop ()
    throws InterruptedException {

    shutdown(false);
    terminationLatch.await();
  }

  private void shutdown (boolean unexpected) {

    if (finished.compareAndSet(false, true)) {

      try {
        socket.close();
      } catch (IOException ioException) {
        LoggerManager.getLogger(NIOCubbyConnection.class).error(ioException);
      }

      try {
        callbackMap.shutdown();
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(NIOCubbyConnection.class).error(interruptedException);
      }

      if (unexpected) {
        connectionCoordinator.disconnect(memcachedHost);
      }
    }
  }

  @Override
  public ServerResponse send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    RequestCallback requestCallback;
    long commandIndex;

    callbackMap.putIfAbsent(commandIndex = commandCounter.getAndIncrement(), requestCallback = new RequestCallback(command), (timeoutSeconds == null) ? null : new Stint(timeoutSeconds, TimeUnit.SECONDS));

    synchronized (requestQueue) {
      requestQueue.offer(new CommandBuffer(commandIndex, command.construct(keyTranslator, codec)));
    }

    return requestCallback.getResult();
  }

  @Override
  public void run () {

    try {
      while (!finished.get()) {
        CommandBuffer commandBuffer;
        try {
          if ((commandBuffer = requestQueue.poll(1, TimeUnit.SECONDS)) != null) {
            socket.getOutputStream().write(commandBuffer.getRequest());

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            boolean complete = false;

            while (!complete) {

              int bytesRead = socket.getInputStream().read(buffer);

              for (int index = 0; index < bytesRead; index++) {
                if (buffer[index] == '\r') {
                  complete = true;
                } else if (buffer[index] == '\n') {
                  if (complete) {

                    ServerResponse response;

                    byteArrayOutputStream.write(buffer, 0, index - 1);
                    response = ResponseParser.parse(new StringBuilder(byteArrayOutputStream.toString()));

                    if (response.getValueLength() > 0) {

                      byte[] value = new byte[response.getValueLength()];

                      System.arraycopy(buffer, index + 1, value, 0, value.length);
                      response.setValue(value);
                    }

                    callbackMap.get(commandBuffer.getIndex()).setResult(response);
                    break;
                  }
                } else {
                  complete = false;
                }
              }

              byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
          }
        } catch (InterruptedException | IOException exception) {
          exception.printStackTrace();
        }
      }
    } finally {
      terminationLatch.countDown();
    }
  }
}
