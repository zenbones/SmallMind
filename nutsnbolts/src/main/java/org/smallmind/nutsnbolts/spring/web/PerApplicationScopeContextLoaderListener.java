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
package org.smallmind.nutsnbolts.spring.web;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Spring {@link ContextLoaderListener} extension that runs the web application context's initialization and shutdown
 * inside a {@link PerApplicationScope}, and publishes that scope under the servlet context attribute keyed by
 * {@code PerApplicationScope.class.getName()} so that {@code org.smallmind.nutsnbolts.scope.web.PerApplicationScopeFilter}
 * can bind it on request threads.
 * <p>
 * Beans that call {@link PerApplicationScope#setPerApplicationData} while the context refreshes write into this
 * scope's map. Threads those beans start inherit no binding; they must be built from
 * {@link PerApplicationScope#wrapThreadFactory} or run their work through an
 * {@link PerApplicationScope.ApplicationCarrier}.
 */
public class PerApplicationScopeContextLoaderListener extends ContextLoaderListener {

  private PerApplicationScope perApplicationScope;

  /**
   * Constructs a listener that reuses the scope already published in the servlet context, or creates a new one if
   * none is published.
   */
  public PerApplicationScopeContextLoaderListener () {

  }

  /**
   * Constructs a listener that publishes and binds the given scope.
   *
   * @param perApplicationScope the scope the web application context will be initialized within
   */
  public PerApplicationScopeContextLoaderListener (PerApplicationScope perApplicationScope) {

    this.perApplicationScope = perApplicationScope;
  }

  /**
   * Publishes the scope in the servlet context under its class name as the attribute key, then delegates to Spring's
   * standard context initialization with that scope bound for the whole refresh.
   *
   * @param servletContextEvent the event carrying the servlet context being initialized
   * @throws ScopeException if a scope was supplied to the constructor and a different scope is already published
   */
  @Override
  public void contextInitialized (ServletContextEvent servletContextEvent) {

    ServletContext servletContext = servletContextEvent.getServletContext();
    PerApplicationScope publishedPerApplicationScope = (PerApplicationScope)servletContext.getAttribute(PerApplicationScope.class.getName());

    if (perApplicationScope == null) {
      perApplicationScope = (publishedPerApplicationScope != null) ? publishedPerApplicationScope : new PerApplicationScope();
    } else if ((publishedPerApplicationScope != null) && (publishedPerApplicationScope != perApplicationScope)) {
      throw new ScopeException("A different PerApplicationScope is already published under servlet context attribute(%s)", PerApplicationScope.class.getName());
    }

    if (publishedPerApplicationScope != perApplicationScope) {
      servletContext.setAttribute(PerApplicationScope.class.getName(), perApplicationScope);
    }

    perApplicationScope.run(() -> super.contextInitialized(servletContextEvent));
  }

  /**
   * Delegates to Spring's standard context shutdown with the scope bound, so bean destruction sees the same
   * per-application data that initialization did. The published attribute is left in place.
   *
   * @param servletContextEvent the event carrying the servlet context being destroyed
   */
  @Override
  public void contextDestroyed (ServletContextEvent servletContextEvent) {

    if (perApplicationScope != null) {
      perApplicationScope.run(() -> super.contextDestroyed(servletContextEvent));
    } else {
      super.contextDestroyed(servletContextEvent);
    }
  }
}
