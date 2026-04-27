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
 * NIO-based implementation of {@link CubbyConnection} that manages a single non-blocking TCP
 * connection to one memcached host.
 *
 * <p>This class runs an internal selector loop (via {@link Runnable#run()}) on a dedicated
 * thread. Client threads submit commands through {@link #send(Command, Long)}, which enqueues
 * a {@link MissingLink} on the request queue and wakes the selector. The selector loop drains
 * the queue through a {@link RequestWriter}, moves each entry to the response queue, and uses
 * a {@link ResponseReader} to parse replies and deliver them to the corresponding
 * {@link RequestCallback}.</p>
 *
 * <p>Keep-alive NOOP commands are issued automatically after periods of inactivity to detect
 * broken connections. If the selector key becomes persistently invalid, or if any unrecoverable
 * I/O error is encountered, the loop terminates and the {@link ConnectionCoordinator} is
 * notified so that the host can be marked unavailable.</p>
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
   * Constructs a new non-blocking connection to the given host using settings drawn from
   * the supplied configuration.
   *
   * @param connectionCoordinator coordinator to notify when the connection is unexpectedly lost
   * @param configuration         source of timeout values, key translator, and authentication
   *                              credentials
   * @param memcachedHost         the remote host to connect to
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
   * Opens the non-blocking socket channel, registers it with a new {@link Selector}, and, if
   * authentication credentials are configured, performs an authentication handshake before
   * returning.
   *
   * <p>The method polls for connection completion at 100 ms intervals up to the configured
   * connection timeout. If the channel is still pending after the timeout a
   * {@link ConnectionTimeoutException} is thrown.</p>
   *
   * @throws InterruptedException    if the calling thread is interrupted while polling for
   *                                 connection completion or while awaiting authentication
   * @throws IOException             if the socket channel cannot be opened, configured, or
   *                                 registered with the selector
   * @throws CubbyOperationException if the authentication command cannot be dispatched or
   *                                 processed
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
   * Signals the I/O loop to stop and waits for it to release the termination latch before
   * closing the selector and socket channel.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for the
   *                              I/O loop to finish
   */
  @Override
  public void stop ()
    throws InterruptedException {

    finished.set(true);
    terminationLatch.await();

    shutdown(false);
  }

  /**
   * Closes the selector and socket channel, guarding against duplicate execution with a
   * compare-and-set flag. If the shutdown was caused by an unexpected error the
   * {@link ConnectionCoordinator} is notified so that the host can be taken out of rotation.
   *
   * @param unexpected {@code true} if this shutdown was triggered by an unrecoverable I/O or
   *                   protocol error rather than a normal {@link #stop()} call
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
   * Serializes and dispatches a command to the memcached server, then blocks until the
   * corresponding response is received or the timeout expires.
   *
   * <p>Serializes the command via the configured {@link KeyTranslator}, wraps it in a
   * {@link MissingLink} with a {@link ClientRequestCallback}, places it on the request queue,
   * and wakes the selector. The calling thread then blocks on
   * {@link ClientRequestCallback#getResult(long)} until a response arrives or the timeout
   * elapses.</p>
   *
   * @param command        the memcached command to dispatch; must not be {@code null}
   * @param timeoutSeconds wait limit in milliseconds, or {@code null} to use the configured
   *                       default; {@code 0L} means wait indefinitely
   * @return the parsed server response
   * @throws InterruptedException    if the calling thread is interrupted while waiting
   * @throws IOException             if a network error occurs
   * @throws CubbyOperationException if the command cannot be constructed or the connection
   *                                 is in an invalid state
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
   * Removes and returns the next {@link MissingLink} from the response queue.
   *
   * <p>The response queue must always have an entry ready when the read side of the selector
   * loop receives a complete response. If the queue is empty the connection state is considered
   * desynchronized and a {@link CubbyOperationException} is thrown.</p>
   *
   * @return the next in-flight link whose callback should receive the parsed response
   * @throws CubbyOperationException if the response queue is unexpectedly empty, indicating
   *                                 that the request and response queues have become
   *                                 desynchronized
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
   * Runs the NIO selector loop that drives all I/O on this connection.
   *
   * <p>Each iteration of the loop selects with a one-second timeout. If no keys become ready
   * within that window the idle counter is incremented; once it exceeds {@code keepAliveSeconds}
   * a NOOP command is enqueued to probe the server. When a key is both readable and writable
   * the read path is processed before the write path to ensure responses are matched before
   * new requests are drained. An {@link InvalidSelectionKeyException} is raised after three
   * consecutive encounters with an invalid key, causing the loop to terminate and the host to
   * be disconnected. Any {@link IOException} or {@link CubbyOperationException} also terminates
   * the loop and triggers an unexpected shutdown.</p>
   *
   * <p>The termination latch is always counted down in the {@code finally} block so that
   * {@link #stop()} never deadlocks even if an exception escapes the loop.</p>
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
