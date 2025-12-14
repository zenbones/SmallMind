/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.memcached.cubby.Authentication;
import org.smallmind.memcached.cubby.ConnectionCoordinator;
import org.smallmind.memcached.cubby.ConnectionTimeoutException;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.IncomprehensibleRequestException;
import org.smallmind.memcached.cubby.InvalidSelectionKeyException;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.command.AuthenticationCommand;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.command.NoopCommand;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * NIO-based implementation of a memcached connection managing read/write loops and health checks.
 */
public class NIOCubbyConnection implements CubbyConnection {

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final AtomicBoolean disconnected = new AtomicBoolean(false);
  private final ConnectionCoordinator connectionCoordinator;
  private final MemcachedHost memcachedHost;
  private final KeyTranslator keyTranslator;
  private final LinkedBlockingQueue<MissingLink> requestQueue = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<MissingLink> responseQueue = new LinkedBlockingQueue<>();
  private final AtomicLong commandCounter = new AtomicLong(0);
  private final Authentication authentication;
  private final long connectionTimeoutMilliseconds;
  private final long keepAliveSeconds;
  private final long defaultRequestTimeoutMilliseconds;
  private SocketChannel socketChannel;
  private Selector selector;
  private SelectionKey selectionKey;
  private RequestWriter requestWriter;
  private ResponseReader responseReader;

  /**
   * Creates a new non-blocking connection to the supplied host.
   *
   * @param connectionCoordinator coordinator to notify about connectivity changes
   * @param configuration         connection-related settings
   * @param memcachedHost         destination host
   */
  public NIOCubbyConnection (ConnectionCoordinator connectionCoordinator, CubbyConfiguration configuration, MemcachedHost memcachedHost) {

    this.connectionCoordinator = connectionCoordinator;
    this.memcachedHost = memcachedHost;

    keyTranslator = configuration.getKeyTranslator();
    authentication = configuration.getAuthentication();
    connectionTimeoutMilliseconds = configuration.getConnectionTimeoutMilliseconds();
    keepAliveSeconds = configuration.getKeepAliveSeconds();
    defaultRequestTimeoutMilliseconds = configuration.getDefaultRequestTimeoutMilliseconds();
  }

  /**
   * Opens the socket channel, configures NIO structures and authenticates if required.
   *
   * @throws InterruptedException    if interrupted while waiting for connection establishment
   * @throws IOException             if the socket cannot be opened or registered
   * @throws CubbyOperationException if authentication dispatch fails
   */
  @Override
  public void start ()
    throws InterruptedException, IOException, CubbyOperationException {

    long start = System.currentTimeMillis();

    socketChannel = SocketChannel.open()
                      .setOption(StandardSocketOptions.SO_KEEPALIVE, true)
                      .setOption(StandardSocketOptions.TCP_NODELAY, true);
    socketChannel.configureBlocking(false);
    socketChannel.connect(memcachedHost.getAddress());

    while ((!socketChannel.finishConnect()) && (System.currentTimeMillis() - start) < connectionTimeoutMilliseconds) {
      Thread.sleep(100);
    }

    if (socketChannel.isConnectionPending()) {
      throw new ConnectionTimeoutException();
    }

    selectionKey = socketChannel.register(selector = Selector.open(), SelectionKey.OP_READ);

    requestWriter = new RequestWriter(socketChannel);
    responseReader = new ResponseReader(socketChannel);

    requestQueue.clear();
    responseQueue.clear();
    commandCounter.set(0L);

    if (authentication != null) {
      send(new AuthenticationCommand().setAuthentication(authentication), 0L);
    }
  }

  /**
   * Signals termination and waits for the selector loop to finish before closing resources.
   *
   * @throws InterruptedException if interrupted while waiting for shutdown
   */
  @Override
  public void stop ()
    throws InterruptedException {

    finished.set(true);
    terminationLatch.await();

    shutdown(false);
  }

  /**
   * Closes selector and socket resources, optionally notifying the coordinator of an unexpected loss.
   *
   * @param unexpected whether the shutdown was triggered by an error
   */
  private void shutdown (boolean unexpected) {

    if (disconnected.compareAndSet(false, true)) {
      selectionKey.cancel();

      try {
        selector.close();
      } catch (IOException ioException) {
        LoggerManager.getLogger(NIOCubbyConnection.class).error(ioException);
      }

      try {
        socketChannel.close();
      } catch (IOException ioException) {
        LoggerManager.getLogger(NIOCubbyConnection.class).error(ioException);
      }

      if (unexpected) {
        connectionCoordinator.disconnect(memcachedHost);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    ClientRequestCallback requestCallback = new ClientRequestCallback(command);

    synchronized (requestQueue) {
      requestQueue.offer(new MissingLink(requestCallback, new CommandBuffer(commandCounter.getAndIncrement(), command.construct(keyTranslator))));
      selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      selector.wakeup();
    }

    return requestCallback.getResult((timeoutSeconds == null) ? defaultRequestTimeoutMilliseconds : timeoutSeconds);
  }

  /**
   * Retrieves the next pending request awaiting a response.
   *
   * @return pairing of callback and buffer
   * @throws CubbyOperationException if the connection state is out of sync
   */
  private MissingLink retrieveMissingLink ()
    throws CubbyOperationException {

    MissingLink missingLink;

    if ((missingLink = responseQueue.poll()) == null) {
      throw new CubbyOperationException("Desynchronized connection state");
    }

    return missingLink;
  }

  /**
   * Main selector loop handling read/write readiness, keep-alive probes and response dispatch.
   */
  @Override
  public void run () {

    int invalidSelectionKeyCount = 0;
    int secondsSinceLastSelection = 0;

    try {
      while (!finished.get()) {
        try {
          if (selector.select(1000) <= 0) {
            if (++secondsSinceLastSelection > keepAliveSeconds) {
              secondsSinceLastSelection = 0;

              synchronized (requestQueue) {
                if (selectionKey.isValid()) {
                  requestQueue.offer(new MissingLink(new ServerRequestCallback(), new CommandBuffer(commandCounter.getAndIncrement(), new NoopCommand().construct(keyTranslator))));
                  selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                  selector.wakeup();
                } else if (++invalidSelectionKeyCount >= 3) {
                  throw new InvalidSelectionKeyException();
                }
              }
            }
          } else {

            Iterator<SelectionKey> selectionKeyIter = selector.selectedKeys().iterator();

            while (selectionKeyIter.hasNext()) {

              SelectionKey selectionKey = selectionKeyIter.next();

              try {
                if (!selectionKey.isValid()) {
                  if (++invalidSelectionKeyCount >= 3) {
                    throw new InvalidSelectionKeyException();
                  }
                } else {
                  invalidSelectionKeyCount = 0;

                  if (selectionKey.isReadable()) {
                    if (responseReader.read()) {

                      boolean proceed = true;

                      do {

                        Response response;

                        try {
                          if ((response = responseReader.extract()) == null) {
                            proceed = false;
                          } else {

                            MissingLink missingLink;

                            if ((missingLink = retrieveMissingLink()).getRequestCallback() != null) {
                              missingLink.getRequestCallback().setResult(response);
                            }
                          }
                        } catch (IOException ioException) {

                          IOException exception = ioException;
                          MissingLink missingLink;

                          if ((missingLink = retrieveMissingLink()).getRequestCallback() != null) {
                            if (exception instanceof IncomprehensibleRequestException) {
                              exception = new IncomprehensibleRequestException(new String(missingLink.getCommandBuffer().getRequest(), StandardCharsets.UTF_8));
                            }

                            missingLink.getRequestCallback().setException(exception);
                          }
                        }
                      } while (proceed);
                    }
                  }
                  if (selectionKey.isWritable()) {
                    if (requestWriter.prepare()) {

                      MissingLink missingLink;

                      do {
                        synchronized (requestQueue) {
                          if ((missingLink = requestQueue.poll()) == null) {
                            selectionKey.interestOps(SelectionKey.OP_READ);
                          }
                        }

                        if (missingLink != null) {
                          responseQueue.add(missingLink);

                          if (!requestWriter.add(missingLink.getCommandBuffer())) {
                            break;
                          }
                        }
                      } while (missingLink != null);
                    }

                    requestWriter.write();
                  }
                }
              } finally {
                selectionKeyIter.remove();
              }
            }
          }
        } catch (IOException | CubbyOperationException exception) {
          LoggerManager.getLogger(NIOCubbyConnection.class).error(exception);

          finished.set(true);
          shutdown(true);
        }
      }
    } finally {
      terminationLatch.countDown();
    }
  }
}
