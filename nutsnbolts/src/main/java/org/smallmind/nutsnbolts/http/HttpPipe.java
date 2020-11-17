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
package org.smallmind.nutsnbolts.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class HttpPipe {

  private enum State {DISCONNECTED, WRITE, READ, TERMINATED}

  private final HttpURLConnection urlConnection;
  private OutputStream httpOutput;
  private InputStream httpInput;
  private State state = State.DISCONNECTED;

  public HttpPipe (HttpURLConnection urlConnection)
    throws IOException {

    this.urlConnection = urlConnection;
  }

  public synchronized HttpPipe setRequestHeader (String key, String value) {

    urlConnection.setRequestProperty(key, value);

    return this;
  }

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

  public synchronized HttpPipe write (String body)
    throws IOException {

    if (!state.equals(State.WRITE)) {
      throw new IllegalStateException("Pipe's state does not allow writing");
    }

    httpOutput.write(body.getBytes());
    httpOutput.flush();

    return this;
  }

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
