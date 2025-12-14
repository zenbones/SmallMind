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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.memcached.cubby.ResponseTimeoutException;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.response.Response;

/**
 * Callback used by client-issued commands to await a response or failure.
 */
public class ClientRequestCallback implements RequestCallback {

  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<Response> resultRef = new AtomicReference<>();
  private final AtomicReference<IOException> exceptionRef = new AtomicReference<>();
  private final Command command;

  /**
   * Creates a callback bound to the originating command.
   *
   * @param command command awaiting a response
   */
  public ClientRequestCallback (Command command) {

    this.command = command;
  }

  /**
   * Waits for the response to arrive or throws if the wait exceeds the timeout.
   *
   * @param timeoutMilliseconds wait duration; zero means wait indefinitely
   * @return the received response
   * @throws InterruptedException if interrupted while waiting
   * @throws IOException          if an I/O error occurred or the wait timed out
   */
  public Response getResult (long timeoutMilliseconds)
    throws InterruptedException, IOException {

    Response response;

    if (timeoutMilliseconds > 0) {
      if (!resultLatch.await(timeoutMilliseconds, TimeUnit.MILLISECONDS)) {
        throw new ResponseTimeoutException("The timeout(%d) milliseconds was exceeded while waiting for a response from command(%s)", timeoutMilliseconds, command);
      }
    } else {
      resultLatch.await();
    }

    if ((response = resultRef.get()) == null) {

      IOException exception;

      if ((exception = exceptionRef.get()) != null) {

        throw exception;
      } else {
        throw new IllegalArgumentException("Missing response");
      }
    } else {

      return response;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setResult (Response response) {

    resultRef.set(response);
    resultLatch.countDown();
  }

  /**
   * {@inheritDoc}
   */
  public void setException (IOException ioException) {

    exceptionRef.set(ioException);
    resultLatch.countDown();
  }
}
