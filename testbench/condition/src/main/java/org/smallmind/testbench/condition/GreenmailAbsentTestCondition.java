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
package org.smallmind.testbench.condition;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * A {@link TestCondition} satisfied only when nothing is listening on a GreenMail port, used to
 * confirm a previously embedded GreenMail mail server has fully shut down before a test proceeds.
 * Satisfaction is inferred from a failed TCP connection: a refused or timed-out connect to
 * {@code localhost} on the configured port means the server is gone.
 */
public class GreenmailAbsentTestCondition implements TestCondition {

  private final int port;

  /**
   * Creates a condition that waits for the GreenMail port to stop accepting connections.
   *
   * @param port the {@code localhost} TCP port GreenMail was bound to
   */
  public GreenmailAbsentTestCondition (int port) {

    this.port = port;
  }

  /**
   * Attempts a short-timeout TCP connection to the configured port.
   *
   * @return {@code null} when the connection is refused or times out (GreenMail is down), or a
   * {@link MessageTestConditionFailure} when the connection succeeds (GreenMail is still up)
   * @throws Exception if a socket error other than a connection refusal or timeout occurs
   */
  @Override
  public TestConditionFailure test ()
    throws Exception {

    try (Socket socket = new Socket()) {
      socket.setTcpNoDelay(true);
      socket.setSoTimeout(1000);
      socket.connect(new InetSocketAddress("localhost", port));

      return new MessageTestConditionFailure("Greenmail has not been shut down");
    } catch (ConnectException | SocketTimeoutException exception) {
      return null;
    }
  }
}
