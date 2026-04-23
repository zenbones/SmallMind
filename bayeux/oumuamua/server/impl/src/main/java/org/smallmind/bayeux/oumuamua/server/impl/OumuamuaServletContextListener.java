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
package org.smallmind.bayeux.oumuamua.server.impl;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Servlet context lifecycle listener that publishes a pre-configured {@link OumuamuaServer} into
 * the servlet context so that the {@link OumuamuaServlet} can find it, and shuts it down when the
 * context is destroyed.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class OumuamuaServletContextListener<V extends Value<V>> implements ServletContextListener {

  private OumuamuaServer<V> oumuamuaServer;

  /**
   * Injects the server instance that will be placed into the servlet context on initialization.
   * Intended for dependency-injection frameworks that construct this listener externally.
   *
   * @param oumuamuaServer the fully configured server to use; must be set before the context
   *                       initializes
   */
  public void setOumuamuaServer (OumuamuaServer<V> oumuamuaServer) {

    this.oumuamuaServer = oumuamuaServer;
  }

  /**
   * Stores the server under the {@link Server#ATTRIBUTE} key in the servlet context so that
   * {@link OumuamuaServlet} can retrieve it during its own initialization.
   *
   * @param servletContextEvent the initialization event carrying the servlet context
   */
  @Override
  public void contextInitialized (ServletContextEvent servletContextEvent) {

    servletContextEvent.getServletContext().setAttribute(Server.ATTRIBUTE, oumuamuaServer);
  }

  /**
   * Shuts down the server and releases its resources when the servlet context is being destroyed.
   *
   * @param servletContextEvent the destruction event; the context attribute is not cleared because
   *                            the context itself is being torn down
   */
  @Override
  public void contextDestroyed (ServletContextEvent servletContextEvent) {

    oumuamuaServer.stop();
  }
}
