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
package org.smallmind.nutsnbolts.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Stateful wrapper around an {@link HttpURLConnection} that enforces ordered write-then-read sequencing
 * and exposes a chainable API for sending request bodies and reading response bodies.
 */
public class HttpPipe {

  private enum State {DISCONNECTED, WRITE, READ, TERMINATED}

  private final HttpURLConnection urlConnection;
  private OutputStream httpOutput;
  private InputStream httpInput;
  private State state = State.DISCONNECTED;

  /**
   * Constructs a pipe around the given connection without opening it.
   *
   * @param urlConnection the underlying HTTP connection to manage
   * @throws IOException declared for subclass compatibility; not thrown by this constructor
   */
  public HttpPipe (HttpURLConnection urlConnection)
    throws IOException {

    this.urlConnection = urlConnection;
  }

  /**
   * Adds or replaces a request property on the underlying connection before it is opened.
   *
   * @param key   HTTP header field name
   * @param value header field value
   * @return this pipe for method chaining
   */
  public synchronized HttpPipe setRequestHeader (String key, String value) {

    urlConnection.setRequestProperty(key, value);

    return this;
  }

  /**
   * Opens the underlying connection and transitions to WRITE, READ, or TERMINATED state based on the configured I/O flags.
   *
   * @return this pipe for method chaining
   * @throws IOException           if the connection cannot be established
   * @throws IllegalStateException if the pipe has already been connected or disconnected
   */
  public synchronized HttpPipe connect ()
    throws IOException {

    if (!state.equals(State.DISCONNECTED)) {
      throw new IllegalStateException("Pipe has already been connected");
    }
    if (state.equals(State.TERMINATED)) {
      throw new IllegalStateException("Pipe has already been disconnected");
    }

    urlConnection.connect();
    state = urlConnection.getDoOutput() ? State.WRITE : urlConnection.getDoInput() ? State.READ : State.TERMINATED;

    switch (state) {
      case WRITE:
        httpOutput = urlConnection.getOutputStream();
        break;
      case READ:
        httpInput = urlConnection.getInputStream();
        break;
      case TERMINATED:
        urlConnection.disconnect();
        break;
      default:
        throw new UnknownSwitchCaseException(state.name());
    }

    return this;
  }

  /**
   * Encodes {@code body} as UTF-8 and writes it to the connection's output stream.
   *
   * @param body text to send as the request body
   * @return this pipe for method chaining
   * @throws IOException           if the write to the output stream fails
   * @throws IllegalStateException if the pipe is not currently in WRITE state
   */
  public synchronized HttpPipe write (String body)
    throws IOException {

    if (!state.equals(State.WRITE)) {
      throw new IllegalStateException("Pipe's state does not allow writing");
    }

    httpOutput.write(body.getBytes(StandardCharsets.UTF_8));
    httpOutput.flush();

    return this;
  }

  /**
   * Closes the output stream and transitions the pipe to READ state if reading is configured, or TERMINATED otherwise.
   *
   * @throws IOException           if closing the output stream fails
   * @throws IllegalStateException if the pipe is not currently in WRITE state
   */
  public synchronized void doneWriting ()
    throws IOException {

    if (!state.equals(State.WRITE)) {
      throw new IllegalStateException("Pipe's state does not allow writing");
    }

    httpOutput.close();

    state = urlConnection.getDoInput() ? State.READ : State.TERMINATED;

    if (state.equals(State.READ)) {
      httpInput = urlConnection.getInputStream();
    } else {
      urlConnection.disconnect();
    }
  }

  /**
   * Reads bytes from the response stream into {@code buffer}, automatically closing the pipe on EOF.
   *
   * @param buffer destination array to fill
   * @return number of bytes read, or {@code -1} when the response body is exhausted
   * @throws IOException           if the read operation fails
   * @throws IllegalStateException if the pipe is not currently in READ state
   */
  public synchronized int read (byte[] buffer)
    throws IOException {

    int bytesRead = 0;

    if (!state.equals(State.READ)) {
      throw new IllegalStateException("Pipe's state does not allow reading");
    }

    try {
      return (bytesRead = httpInput.read(buffer));
    } finally {
      if (bytesRead < 0) {
        doneReading();
      }
    }
  }

  /**
   * Closes the response input stream and disconnects the underlying connection.
   *
   * @throws IOException           if closing the input stream fails
   * @throws IllegalStateException if the pipe is not currently in READ state
   */
  public synchronized void doneReading ()
    throws IOException {

    if (!state.equals(State.READ)) {
      throw new IllegalStateException("Pipe's state does not allow reading");
    }

    state = State.TERMINATED;

    try {
      httpInput.close();
    } finally {
      urlConnection.disconnect();
    }
  }
}
