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
package org.smallmind.web.grizzly;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * Grizzly {@link HttpHandler} that runs a delegate handler's {@link #service(Request, Response)} inside a
 * {@link PerApplicationScope}, so handlers registered outside the servlet filter chain (JAX-WS services in
 * particular) see the same per-application binding that {@code PerApplicationScopeFilter} gives servlets.
 * {@link #start()} and {@link #destroy()} are forwarded to the delegate unchanged.
 * <p>
 * The binding covers exactly the extent of the delegate's {@code service} call. A {@code JaxwsHandler} in async mode
 * still invokes an ordinary {@code @WebService} implementor on this thread, but an {@code AsyncProvider} that
 * completes on a thread of its own is not bound and must hold the scope itself.
 */
public class PerApplicationScopeHttpHandler extends HttpHandler {

  private final HttpHandler httpHandler;
  private final PerApplicationScope perApplicationScope;

  // TODO: Per Application Scope Entry Point
  /**
   * Constructs a handler that binds {@code perApplicationScope} around every call to {@code httpHandler}.
   *
   * @param httpHandler         the handler to delegate to
   * @param perApplicationScope the scope to bind for the extent of each {@code service} call
   */
  public PerApplicationScopeHttpHandler (HttpHandler httpHandler, PerApplicationScope perApplicationScope) {

    if (httpHandler == null) {
      throw new NullPointerException("The delegate HttpHandler is required");
    }
    if (perApplicationScope == null) {
      throw new NullPointerException("The PerApplicationScope is required");
    }

    this.httpHandler = httpHandler;
    this.perApplicationScope = perApplicationScope;
  }

  @Override
  public void start () {

    httpHandler.start();
  }

  @Override
  public void destroy () {

    httpHandler.destroy();
  }

  /**
   * Services the request through the delegate with the scope bound; whatever the delegate throws is rethrown
   * unchanged.
   *
   * @param request  the request
   * @param response the response
   * @throws Exception if the delegate throws
   */
  @Override
  public void service (Request request, Response response)
    throws Exception {

    perApplicationScope.<Void, Exception>call(() -> {
      httpHandler.service(request, response);

      return null;
    });
  }
}
