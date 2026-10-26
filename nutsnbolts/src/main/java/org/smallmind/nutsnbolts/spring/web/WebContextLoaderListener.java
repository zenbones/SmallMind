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

import jakarta.servlet.ServletContextEvent;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Spring {@link ContextLoaderListener} extension that makes sure the thread refreshing the Spring context carries a
 * {@link PerApplicationContext} — attaching an already-published one, or constructing and publishing one as the
 * servlet context attribute {@code PerApplicationContextFilter} reads — before delegating to the standard Spring
 * context initialization.
 */
public class WebContextLoaderListener extends ContextLoaderListener {

  /**
   * Makes sure the thread about to refresh the Spring context carries the application's {@link PerApplicationContext},
   * then delegates to Spring's standard context initialization. If a context is already published in the servlet
   * context under {@code PerApplicationContext.class.getName()}, its map is attached to this thread; otherwise a
   * context is constructed (adopting any map already attached to this thread, or creating one) and published under
   * that attribute for {@code PerApplicationContextFilter} to find. Either way the registrations made during the
   * refresh land in the published map.
   *
   * @param servletContextEvent the event carrying the servlet context being initialized
   */
  @Override
  public void contextInitialized (ServletContextEvent servletContextEvent) {

    PerApplicationContext perApplicationContext;

    if ((perApplicationContext = (PerApplicationContext)servletContextEvent.getServletContext().getAttribute(PerApplicationContext.class.getName())) == null) {
      servletContextEvent.getServletContext().setAttribute(PerApplicationContext.class.getName(), new PerApplicationContext());
    } else {
      perApplicationContext.prepareThread();
    }

    super.contextInitialized(servletContextEvent);
  }

  /**
   * Attaches the published {@link PerApplicationContext}, if any, to the thread running the destruction, so that
   * shutdown-time lookups made by beans resolve the same registrations the refresh made even when the container
   * destroys the context on a different thread, then delegates to Spring's standard context destruction. Like every
   * other attachment the map stays on the thread afterwards. When the {@code nutsnbolts} classes are shared at
   * container level, a replacement webapp initialized on that same thread with nothing published adopts the map
   * through {@code new PerApplicationContext()} and starts from the destroyed application's registrations,
   * overwriting them key by key as its own refresh registers; with a per-webapp copy in {@code WEB-INF/lib} nothing
   * is adopted, and the attachment instead pins the destroyed webapp's class loader for the thread's lifetime, the
   * same pin every request thread that ran {@code PerApplicationContextFilter} already holds.
   *
   * @param servletContextEvent the event carrying the servlet context being destroyed
   */
  @Override
  public void contextDestroyed (ServletContextEvent servletContextEvent) {

    PerApplicationContext perApplicationContext;

    if ((perApplicationContext = (PerApplicationContext)servletContextEvent.getServletContext().getAttribute(PerApplicationContext.class.getName())) != null) {
      perApplicationContext.prepareThread();
    }

    super.contextDestroyed(servletContextEvent);
  }
}
