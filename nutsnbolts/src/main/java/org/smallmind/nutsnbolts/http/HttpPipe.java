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
 * Thin wrapper around {@link HttpURLConnection} that enforces read/write sequencing.
 * Allows chaining writes and reads while managing connection state transitions.
 */
public class HttpPipe {

  private enum State {DISCONNECTED, WRITE, READ, TERMINATED}

  private final HttpURLConnection urlConnection;
  private OutputStream httpOutput;
  private InputStream httpInput;
  private State state = State.DISCONNECTED;

  /**
   * Wraps an existing {@link HttpURLConnection}.
   *
   * @param urlConnection connection to manage
   * @throws IOException unused but retained for compatibility
   */
  public HttpPipe (HttpURLConnection urlConnection)
    throws IOException {

    this.urlConnection = urlConnection;
  }

  /**
   * Sets a request header on the underlying connection.
   *
   * @param key   header name
   * @param value header value
   * @return this pipe for chaining
   */
  public synchronized HttpPipe setRequestHeader (String key, String value) {

    urlConnection.setRequestProperty(key, value);

    return this;
  }

  /**
   * Opens the connection and prepares for write or read depending on configured I/O flags.
   *
   * @return this pipe for chaining
   * @throws IOException                if the connection cannot be opened
   * @throws IllegalStateException      if called in an invalid state
   * @throws UnknownSwitchCaseException if an unexpected state is reached
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
   * Writes a UTF-8 body to the connection output stream.
   *
   * @param body content to send
   * @return this pipe for chaining
   * @throws IOException           if writing fails
   * @throws IllegalStateException if the pipe is not in write mode
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
   * Signals end-of-stream for writing, transitioning to read or termination.
   *
   * @throws IOException           if closing fails
   * @throws IllegalStateException if the pipe is not in write mode
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
   * Reads bytes into the provided buffer.
   *
   * @param buffer destination buffer
   * @return number of bytes read or -1 on EOF
   * @throws IOException           if reading fails
   * @throws IllegalStateException if the pipe is not in read mode
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
   * Finishes reading and closes the connection.
   *
   * @throws IOException           if closing fails
   * @throws IllegalStateException if the pipe is not in read mode
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
