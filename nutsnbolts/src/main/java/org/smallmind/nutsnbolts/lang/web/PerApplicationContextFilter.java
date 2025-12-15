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
package org.smallmind.nutsnbolts.lang.web;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;

/**
 * Servlet {@link Filter} that initializes and propagates a {@link PerApplicationContext} into
 * each request thread. Optionally suppresses noisy connection-closed exceptions.
 */
public class PerApplicationContextFilter implements Filter {

  private PerApplicationContext perApplicationContext;
  private boolean suppressConnectionClosedException;

  /**
   * Creates a filter with default behavior (connection closed exceptions are propagated).
   */
  public PerApplicationContextFilter () {

  }

  /**
   * Creates a filter with control over connection-closed exception suppression.
   *
   * @param suppressConnectionClosedException {@code true} to swallow "Connection is closed" IOExceptions
   */
  public PerApplicationContextFilter (boolean suppressConnectionClosedException) {

    this.suppressConnectionClosedException = suppressConnectionClosedException;
  }

  /**
   * Configures suppression of "Connection is closed" IOExceptions thrown from the filter chain.
   *
   * @param suppressConnectionClosedException whether to suppress the exception
   * @return this filter for chaining
   */
  public PerApplicationContextFilter setSuppressConnectionClosedException (boolean suppressConnectionClosedException) {

    this.suppressConnectionClosedException = suppressConnectionClosedException;

    return this;
  }

  /**
   * Initializes the filter, ensuring a {@link PerApplicationContext} is present on the servlet context.
   *
   * @param filterConfig the filter configuration supplied by the container
   */
  @Override
  public void init (FilterConfig filterConfig) {

    if ((perApplicationContext = (PerApplicationContext)filterConfig.getServletContext().getAttribute(PerApplicationContext.class.getName())) == null) {
      filterConfig.getServletContext().setAttribute(PerApplicationContext.class.getName(), perApplicationContext = new PerApplicationContext());
    }
  }

  /**
   * Prepares the per-application context for the current thread and delegates the request.
   * Optionally suppresses benign connection-closed IOExceptions from downstream filters.
   *
   * @param request  the incoming request
   * @param response the outgoing response
   * @param chain    the filter chain
   * @throws IOException      if an I/O error occurs and suppression is disabled
   * @throws ServletException if a downstream filter or servlet fails
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
   * {@inheritDoc}
   */
  @Override
  public void destroy () {

  }
}
