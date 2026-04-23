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
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * A {@link RequestCallback} implementation used for internally generated maintenance commands
 * such as keep-alive NOOPs, where no external caller is waiting for the server's response.
 *
 * <p>Successful responses are silently discarded. I/O errors are logged at the error level
 * via the application's logging infrastructure so that connection-level anomalies are still
 * visible without propagating an exception to any client thread.</p>
 */
public class ServerRequestCallback implements RequestCallback {

  /**
   * {@inheritDoc}
   *
   * <p>Silently discards the response, as no caller is waiting for the result of a
   * server-initiated maintenance command.</p>
   *
   * @param response the parsed server response; ignored by this implementation
   */
  @Override
  public void setResult (Response response) {

  }

  /**
   * {@inheritDoc}
   *
   * <p>Logs the exception at the error level. Because no client thread is blocked on this
   * callback, the error cannot be propagated and is instead recorded for diagnostic purposes.</p>
   *
   * @param ioException the I/O error that occurred while processing the maintenance command
   */
  @Override
  public void setException (IOException ioException) {

    LoggerManager.getLogger(ServerRequestCallback.class).error(ioException);
  }
}
