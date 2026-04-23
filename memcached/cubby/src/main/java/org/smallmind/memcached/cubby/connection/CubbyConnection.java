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
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.response.Response;

/**
 * Represents a live, managed connection to a single memcached server node.
 *
 * <p>Implementations are expected to run an internal I/O loop (via {@link Runnable#run()}) that
 * handles both reading responses from and writing commands to the server. The {@link #start()}
 * method establishes the underlying transport and performs any required handshake (such as
 * authentication), while {@link #stop()} performs an orderly shutdown. Client code submits
 * work through {@link #send(Command, Long)} and blocks until the corresponding response is
 * available or a timeout elapses.</p>
 */
public interface CubbyConnection extends Runnable {

  /**
   * Opens the connection to the remote memcached host and prepares all I/O resources.
   *
   * <p>This method must be called before {@link #send(Command, Long)} or the I/O loop
   * ({@link Runnable#run()}) is started. Implementations typically open a socket, configure
   * NIO structures, and, if credentials are provided, perform an authentication handshake.</p>
   *
   * @throws InterruptedException    if the calling thread is interrupted while waiting for the
   *                                 connection to be established
   * @throws IOException             if the underlying socket cannot be opened, configured, or
   *                                 connected to the remote host
   * @throws CubbyOperationException if a logical error occurs during initialization, such as
   *                                 a failed authentication exchange
   */
  void start ()
    throws InterruptedException, IOException, CubbyOperationException;

  /**
   * Signals the I/O loop to terminate and waits for it to finish before releasing resources.
   *
   * <p>After this method returns the connection must not be used again. Any in-flight requests
   * may or may not receive responses depending on implementation.</p>
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for the
   *                              I/O loop to terminate
   * @throws IOException          if an error occurs while closing the underlying socket or
   *                              selector
   */
  void stop ()
    throws InterruptedException, IOException;

  /**
   * Serializes and dispatches a command to the memcached server, then blocks until the
   * corresponding response is received or the timeout expires.
   *
   * @param command        the memcached command to send; must not be {@code null}
   * @param timeoutSeconds maximum number of milliseconds to wait for the response, or
   *                       {@code null} to use the connection's configured default timeout;
   *                       pass {@code 0L} to wait indefinitely
   * @return the parsed {@link Response} returned by the server for this command
   * @throws InterruptedException    if the calling thread is interrupted while waiting for
   *                                 the response
   * @throws IOException             if a network I/O error occurs during send or receive
   * @throws CubbyOperationException if the command cannot be routed or the connection is
   *                                 in an invalid state
   */
  Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException;
}
