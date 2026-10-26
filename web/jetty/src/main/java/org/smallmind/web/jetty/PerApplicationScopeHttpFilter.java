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
package org.smallmind.web.jetty;

import java.io.IOException;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * {@link Filter} for a {@code com.sun.net.httpserver.HttpContext} that runs the rest of the chain inside a
 * {@link PerApplicationScope}, so JAX-WS endpoints published through Jetty's {@code JettyHttpServer} see the same
 * per-application binding that {@code PerApplicationScopeFilter} gives servlets.
 * <p>
 * The binding covers exactly the extent of {@code chain.doFilter}. The JAX-WS runtime handles the exchange on this
 * thread unless the endpoint was given its own {@code Executor}; an endpoint that was is not bound and must hold
 * the scope itself.
 */
public class PerApplicationScopeHttpFilter extends Filter {

  private final PerApplicationScope perApplicationScope;

  /**
   * Constructs a filter that binds {@code perApplicationScope} around the rest of each exchange's chain.
   *
   * @param perApplicationScope the scope to bind
   */
  public PerApplicationScopeHttpFilter (PerApplicationScope perApplicationScope) {

    if (perApplicationScope == null) {
      throw new NullPointerException("The PerApplicationScope is required");
    }

    this.perApplicationScope = perApplicationScope;
  }

  @Override
  public String description () {

    return "per-application-scope";
  }

  /**
   * Continues the chain with the scope bound; an {@link IOException} thrown downstream is rethrown unchanged.
   *
   * @param exchange the exchange
   * @param chain    the rest of the chain
   * @throws IOException if the chain throws
   */
  @Override
  public void doFilter (HttpExchange exchange, Chain chain)
    throws IOException {

    perApplicationScope.<Void, IOException>call(() -> {
      chain.doFilter(exchange);

      return null;
    });
  }
}
