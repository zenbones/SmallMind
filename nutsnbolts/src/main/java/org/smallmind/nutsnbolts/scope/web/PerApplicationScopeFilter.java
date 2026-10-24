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
package org.smallmind.nutsnbolts.scope.web;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * Servlet filter that binds a {@link PerApplicationScope} for the dynamic extent of each request's filter chain and
 * optionally suppresses "Connection is closed" {@link IOException}s thrown during chain execution.
 * <p>
 * The scope is either handed to a constructor or, for container-instantiated filters, read in {@link #init} from the
 * servlet context attribute keyed by {@code PerApplicationScope.class.getName()}, which
 * {@code org.smallmind.nutsnbolts.spring.web.PerApplicationScopeContextLoaderListener} publishes. This filter never
 * creates a scope of its own, because a scope nothing else writes to would be silently empty. The binding ends when
 * {@code chain.doFilter} returns or throws; work dispatched asynchronously from the request is not bound. The scope's
 * map is shared, so data stored during one request is visible to every later request through the same scope.
 */
public class PerApplicationScopeFilter implements Filter {

  private PerApplicationScope perApplicationScope;
  private boolean suppressConnectionClosedException;

  /**
   * Constructs a filter that resolves its scope from the servlet context in {@link #init} and does not suppress
   * connection-closed {@link IOException}s.
   */
  public PerApplicationScopeFilter () {

  }

  // TODO: Per Application Scope Entry Point

  /**
   * Constructs a filter bound to the given scope that does not suppress connection-closed {@link IOException}s.
   *
   * @param perApplicationScope the scope to bind on each request
   */
  public PerApplicationScopeFilter (PerApplicationScope perApplicationScope) {

    this.perApplicationScope = perApplicationScope;
  }

  // TODO: Per Application Scope Entry Point

  /**
   * Constructs a filter bound to the given scope with explicit control over connection-closed {@link IOException}
   * suppression.
   *
   * @param perApplicationScope               the scope to bind on each request
   * @param suppressConnectionClosedException {@code true} to silently discard {@link IOException}s whose message is
   *                                          {@code "Connection is closed"}
   */
  public PerApplicationScopeFilter (PerApplicationScope perApplicationScope, boolean suppressConnectionClosedException) {

    this.perApplicationScope = perApplicationScope;
    this.suppressConnectionClosedException = suppressConnectionClosedException;
  }

  /**
   * Sets whether {@link IOException}s with the message {@code "Connection is closed"} are suppressed, and returns
   * this filter instance to support method chaining.
   *
   * @param suppressConnectionClosedException {@code true} to suppress connection-closed exceptions
   * @return this filter instance
   */
  public PerApplicationScopeFilter setSuppressConnectionClosedException (boolean suppressConnectionClosedException) {

    this.suppressConnectionClosedException = suppressConnectionClosedException;

    return this;
  }

  /**
   * Resolves the scope from the servlet context attribute keyed by {@code PerApplicationScope.class.getName()} when
   * none was supplied to the constructor. A scope supplied to the constructor is used as-is and the attribute is
   * ignored.
   *
   * @param filterConfig the filter configuration supplied by the servlet container
   * @throws MissingPerApplicationScopeException if no scope was supplied and the servlet context publishes none
   */
  @Override
  public void init (FilterConfig filterConfig) {

    if (perApplicationScope == null) {
      if ((perApplicationScope = (PerApplicationScope)filterConfig.getServletContext().getAttribute(PerApplicationScope.class.getName())) == null) {
        throw new MissingPerApplicationScopeException("No PerApplicationScope was supplied and none is published under servlet context attribute(%s)", PerApplicationScope.class.getName());
      }
    }
  }

  /**
   * Invokes the rest of the filter chain with the per-application scope bound for its dynamic extent, optionally
   * swallowing connection-closed {@link IOException}s.
   *
   * @param request  the current servlet request
   * @param response the current servlet response
   * @param chain    the remaining filter chain to invoke
   * @throws IOException      if an I/O error occurs and suppression of connection-closed exceptions is not enabled
   * @throws ServletException if a servlet error is thrown by the filter chain
   * @throws RuntimeException rethrown unchanged if the filter chain throws one
   */
  @Override
  public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {

    try {
      perApplicationScope.<Void, Exception>call(() -> {
        chain.doFilter(request, response);

        return null;
      });
    } catch (IOException ioException) {
      if ((!suppressConnectionClosedException) || (!"Connection is closed".equals(ioException.getMessage()))) {
        throw ioException;
      }
    } catch (ServletException | RuntimeException rawsException) {
      throw rawsException;
    } catch (Exception exception) {
      throw new ServletException(exception);
    }
  }

  /**
   * No-op destroy callback; this filter holds no resources that require release.
   */
  @Override
  public void destroy () {

  }
}
