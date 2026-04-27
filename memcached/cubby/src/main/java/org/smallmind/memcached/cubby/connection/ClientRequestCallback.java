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
 * A {@link RequestCallback} implementation that allows the client thread that issued a command
 * to block until the corresponding server response arrives or a timeout expires.
 *
 * <p>When the NIO I/O loop receives a response it invokes {@link #setResult(Response)} or
 * {@link #setException(IOException)}, releasing the latch and unblocking any thread waiting
 * in {@link #getResult(long)}. A single instance is created per outbound command and must
 * not be reused.</p>
 */
public class ClientRequestCallback implements RequestCallback {

  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<Response> resultRef = new AtomicReference<>();
  private final AtomicReference<IOException> exceptionRef = new AtomicReference<>();
  private final Command command;

  /**
   * Creates a callback associated with the given command, which is used in timeout-exceeded
   * error messages.
   *
   * @param command the command for which a response is expected; must not be {@code null}
   */
  public ClientRequestCallback (Command command) {

    this.command = command;
  }

  /**
   * Blocks the calling thread until a response is available or the timeout elapses.
   *
   * <p>If {@code timeoutMilliseconds} is greater than zero the method waits at most that many
   * milliseconds; if the latch does not count down within that window a
   * {@link ResponseTimeoutException} is thrown. A value of zero or less causes the method to
   * wait indefinitely.</p>
   *
   * @param timeoutMilliseconds maximum wait time in milliseconds; zero or negative means wait
   *                            indefinitely
   * @return the {@link Response} delivered by the I/O loop
   * @throws InterruptedException if the calling thread is interrupted while waiting
   * @throws IOException          if the I/O loop reported an error via {@link #setException(IOException)},
   *                              or if the timeout is exceeded (thrown as {@link ResponseTimeoutException})
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
   * Delivers the successfully parsed server response to this callback.
   *
   * <p>This method is called by the I/O loop thread and must return quickly without blocking.</p>
   *
   * <p>Stores the response and releases the latch, unblocking any thread waiting in
   * {@link #getResult(long)}.</p>
   *
   * @param response the parsed server response; must not be {@code null}
   */
  public void setResult (Response response) {

    resultRef.set(response);
    resultLatch.countDown();
  }

  /**
   * Delivers an I/O exception that occurred while writing the command or reading its response.
   *
   * <p>This method is called by the I/O loop thread and must return quickly without blocking.</p>
   *
   * <p>Stores the exception and releases the latch so that {@link #getResult(long)} can
   * propagate it to the waiting client thread.</p>
   *
   * @param ioException the I/O error that occurred while processing the command
   */
  public void setException (IOException ioException) {

    exceptionRef.set(ioException);
    resultLatch.countDown();
  }
}
