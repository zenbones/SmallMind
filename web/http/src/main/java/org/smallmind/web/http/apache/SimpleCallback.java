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
package org.smallmind.web.http.apache;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;

/**
 * Concrete {@link HttpCallback} that stores the HTTP response or the failure exception for retrieval after the call completes.
 */
public class SimpleCallback extends HttpCallback<SimpleHttpResponse> {

  private final AtomicReference<SimpleHttpResponse> responseRef = new AtomicReference<>();
  private final AtomicReference<Exception> exceptionRef = new AtomicReference<>();

  /**
   * Returns the captured HTTP response, or rethrows any exception recorded during the callback.
   *
   * @return the {@link SimpleHttpResponse} received from the server
   * @throws Exception if the request failed or was cancelled
   */
  public SimpleHttpResponse getResponse ()
    throws Exception {

    Exception exception;

    if ((exception = exceptionRef.get()) != null) {
      throw exception;
    } else {
      return responseRef.get();
    }
  }

  /**
   * Stores the successful HTTP response for later retrieval via {@link #getResponse()}.
   *
   * @param response the response returned by the server
   */
  @Override
  public void onCompleted (SimpleHttpResponse response) {

    responseRef.set(response);
  }

  /**
   * Records the failure exception so {@link #getResponse()} can rethrow it.
   *
   * @param exception the exception that caused the failure
   */
  @Override
  public void onFailed (Exception exception) {

    exceptionRef.set(exception);
  }

  /**
   * Records an {@link HttpRequestCancelledException} so {@link #getResponse()} reports the cancellation as an error.
   */
  @Override
  public void onCancelled () {

    exceptionRef.set(new HttpRequestCancelledException("The http request was cancelled before completion"));
  }
}
