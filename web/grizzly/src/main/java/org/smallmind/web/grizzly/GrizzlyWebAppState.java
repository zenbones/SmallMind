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

import java.util.LinkedList;
import org.glassfish.grizzly.servlet.WebappContext;
import org.smallmind.web.grizzly.installer.FilterInstaller;
import org.smallmind.web.grizzly.installer.ListenerInstaller;
import org.smallmind.web.grizzly.installer.ServletInstaller;
import org.smallmind.web.grizzly.installer.WebServiceInstaller;
import org.smallmind.web.grizzly.installer.WebSocketExtensionInstaller;
import org.smallmind.web.grizzly.tyrus.TyrusGrizzlyServerContainer;

/**
 * Accumulates all deployment artifacts — installers, the servlet context, and the WebSocket container — for a single
 * Grizzly web application context.
 */
public class GrizzlyWebAppState {

  private final LinkedList<WebSocketExtensionInstaller> webSocketExtensionInstallerList = new LinkedList<>();
  private final LinkedList<WebServiceInstaller> webServiceInstallerList = new LinkedList<>();
  private final LinkedList<ListenerInstaller> listenerInstallerList = new LinkedList<>();
  private final LinkedList<FilterInstaller> filterInstallerList = new LinkedList<>();
  private final LinkedList<ServletInstaller> servletInstallerList = new LinkedList<>();
  private final WebappContext webAppContext;
  private TyrusGrizzlyServerContainer tyrusGrizzlyServerContainer;

  /**
   * Creates the state holder for the given servlet context.
   *
   * @param webAppContext Grizzly servlet context for this application
   */
  public GrizzlyWebAppState (WebappContext webAppContext) {

    this.webAppContext = webAppContext;
  }

  /**
   * Returns the Grizzly servlet context for this application.
   *
   * @return the underlying {@link WebappContext}
   */
  public WebappContext getWebAppContext () {

    return webAppContext;
  }

  /**
   * Adds a WebSocket extension installer to be applied when deploying endpoints.
   *
   * @param webSocketExtensionInstaller installer carrying extensions for a specific endpoint
   */
  public void addWebSocketExtensionInstaller (WebSocketExtensionInstaller webSocketExtensionInstaller) {

    webSocketExtensionInstallerList.add(webSocketExtensionInstaller);
  }

  /**
   * Returns all registered WebSocket extension installers.
   *
   * @return list of WebSocket extension installers
   */
  public LinkedList<WebSocketExtensionInstaller> getWebSocketExtensionInstallerList () {

    return webSocketExtensionInstallerList;
  }

  /**
   * Adds a SOAP web-service installer to be registered under this context.
   *
   * @param webServiceInstaller installer describing the service and its path
   */
  public void addWebServiceInstaller (WebServiceInstaller webServiceInstaller) {

    webServiceInstallerList.add(webServiceInstaller);
  }

  /**
   * Returns all registered SOAP service installers.
   *
   * @return list of web-service installers
   */
  public LinkedList<WebServiceInstaller> getWebServiceInstallerList () {

    return webServiceInstallerList;
  }

  /**
   * Adds a servlet context listener installer to be registered under this context.
   *
   * @param listenerInstaller installer wrapping the listener to install
   */
  public void addListenerInstaller (ListenerInstaller listenerInstaller) {

    listenerInstallerList.add(listenerInstaller);
  }

  /**
   * Returns all registered listener installers.
   *
   * @return list of listener installers
   */
  public LinkedList<ListenerInstaller> getListenerInstallerList () {

    return listenerInstallerList;
  }

  /**
   * Adds a servlet filter installer to be registered under this context.
   *
   * @param filterInstaller installer describing the filter and its mapping
   */
  public void addFilterInstaller (FilterInstaller filterInstaller) {

    filterInstallerList.add(filterInstaller);
  }

  /**
   * Returns all registered filter installers.
   *
   * @return list of filter installers
   */
  public LinkedList<FilterInstaller> getFilterInstallerList () {

    return filterInstallerList;
  }

  /**
   * Adds a servlet installer to be registered under this context.
   *
   * @param servletInstaller installer describing the servlet and its mapping
   */
  public void addServletInstaller (ServletInstaller servletInstaller) {

    servletInstallerList.add(servletInstaller);
  }

  /**
   * Returns all registered servlet installers.
   *
   * @return list of servlet installers
   */
  public LinkedList<ServletInstaller> getServletInstallerList () {

    return servletInstallerList;
  }

  /**
   * Returns the Tyrus WebSocket server container associated with this context.
   *
   * @return the WebSocket server container, or {@code null} if WebSocket support is not configured
   */
  public TyrusGrizzlyServerContainer getTyrusGrizzlyServerContainer () {

    return tyrusGrizzlyServerContainer;
  }

  /**
   * Sets the Tyrus WebSocket server container for this context.
   *
   * @param tyrusGrizzlyServerContainer initialized WebSocket container to store
   */
  public void setTyrusGrizzlyServerContainer (TyrusGrizzlyServerContainer tyrusGrizzlyServerContainer) {

    this.tyrusGrizzlyServerContainer = tyrusGrizzlyServerContainer;
  }
}
