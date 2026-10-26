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
package org.smallmind.nutsnbolts.context.web;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;

/**
 * Servlet filter that binds a shared {@link PerApplicationContext} to each request thread and optionally suppresses
 * "Connection is closed" {@link IOException}s thrown during filter chain execution. The context is either supplied to
 * a constructor or, for the no-arg forms, read in {@link #init} from the servlet-context attribute named
 * {@code PerApplicationContext.class.getName()} (which {@code WebContextLoaderListener} publishes). The filter never
 * creates a context of its own: a filter-created map would be empty and would shadow the registries the application
 * actually wrote to, so {@code init} fails with {@link MissingPerApplicationContextException} when none is available.
 */
public class PerApplicationContextFilter implements Filter {

  private PerApplicationContext perApplicationContext;
  private boolean suppressConnectionClosedException;

  /**
   * Constructs a filter that does not suppress connection-closed {@link IOException}s.
   */
  public PerApplicationContextFilter () {

  }

  /**
   * Constructs a filter with explicit control over connection-closed {@link IOException} suppression.
   *
   * @param suppressConnectionClosedException {@code true} to silently discard {@link IOException}s whose message is
   *                                          {@code "Connection is closed"}
   */
  public PerApplicationContextFilter (boolean suppressConnectionClosedException) {

    this.suppressConnectionClosedException = suppressConnectionClosedException;
  }

  /**
   * Constructs a filter that binds the given context, ignoring any servlet-context attribute.
   *
   * @param perApplicationContext the context to bind on each request thread
   */
  public PerApplicationContextFilter (PerApplicationContext perApplicationContext) {

    this.perApplicationContext = perApplicationContext;
  }

  /**
   * Constructs a filter that binds the given context, ignoring any servlet-context attribute, with explicit control
   * over connection-closed {@link IOException} suppression.
   *
   * @param perApplicationContext             the context to bind on each request thread
   * @param suppressConnectionClosedException {@code true} to silently discard {@link IOException}s whose message is
   *                                          {@code "Connection is closed"}
   */
  public PerApplicationContextFilter (PerApplicationContext perApplicationContext, boolean suppressConnectionClosedException) {

    this.perApplicationContext = perApplicationContext;
    this.suppressConnectionClosedException = suppressConnectionClosedException;
  }

  /**
   * Sets whether {@link IOException}s with the message {@code "Connection is closed"} are suppressed, and returns
   * this filter instance to support method chaining.
   *
   * @param suppressConnectionClosedException {@code true} to suppress connection-closed exceptions
   * @return this filter instance
   */
  public PerApplicationContextFilter setSuppressConnectionClosedException (boolean suppressConnectionClosedException) {

    this.suppressConnectionClosedException = suppressConnectionClosedException;

    return this;
  }

  /**
   * Resolves the shared {@link PerApplicationContext}: the instance given to the constructor, or else the servlet
   * context attribute keyed by {@link PerApplicationContext}'s class name.
   *
   * @param filterConfig the filter configuration supplied by the servlet container
   * @throws MissingPerApplicationContextException if no context was supplied and none is published as a servlet
   *                                               context attribute
   */
  @Override
  public void init (FilterConfig filterConfig) {

    if (perApplicationContext == null) {
      if ((perApplicationContext = (PerApplicationContext)filterConfig.getServletContext().getAttribute(PerApplicationContext.class.getName())) == null) {
        throw new MissingPerApplicationContextException("No PerApplicationContext was supplied and none is published under servlet context attribute(%s)", PerApplicationContext.class.getName());
      }
    }
  }

  /**
   * Prepares the per-application context on the current thread, then invokes the rest of the filter chain,
   * optionally swallowing connection-closed {@link IOException}s.
   *
   * @param request  the current servlet request
   * @param response the current servlet response
   * @param chain    the remaining filter chain to invoke
   * @throws IOException      if an I/O error occurs and suppression of connection-closed exceptions is not enabled
   * @throws ServletException if a servlet error is thrown by the filter chain
   */
  @Override
  public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {

    try {
      perApplicationContext.prepareThread();
      chain.doFilter(request, response);
    } catch (IOException ioException) {
      if ((!suppressConnectionClosedException) || (!"Connection is closed".equals(ioException.getMessage()))) {
        throw ioException;
      }
    }
  }

  /**
   * No-op destroy callback; this filter holds no resources that require release.
   */
  @Override
  public void destroy () {

  }
}
