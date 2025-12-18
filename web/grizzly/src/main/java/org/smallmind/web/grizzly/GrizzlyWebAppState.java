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
 * Holds per-context deployment state for the Grizzly server, including collected installer beans and the backing
 * {@link WebappContext}.
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
   * @param webAppContext the servlet context representing the application
   */
  public GrizzlyWebAppState (WebappContext webAppContext) {

    this.webAppContext = webAppContext;
  }

  /**
   * @return the underlying Grizzly {@link WebappContext}
   */
  public WebappContext getWebAppContext () {

    return webAppContext;
  }

  /**
   * Registers WebSocket extensions to apply to endpoints in this context.
   *
   * @param webSocketExtensionInstaller installer describing extensions for an endpoint
   */
  public void addWebSocketExtensionInstaller (WebSocketExtensionInstaller webSocketExtensionInstaller) {

    webSocketExtensionInstallerList.add(webSocketExtensionInstaller);
  }

  /**
   * @return collected WebSocket extension installers
   */
  public LinkedList<WebSocketExtensionInstaller> getWebSocketExtensionInstallerList () {

    return webSocketExtensionInstallerList;
  }

  /**
   * Registers a SOAP service to expose under this context.
   *
   * @param webServiceInstaller installer describing the service instance and path
   */
  public void addWebServiceInstaller (WebServiceInstaller webServiceInstaller) {

    webServiceInstallerList.add(webServiceInstaller);
  }

  /**
   * @return collected SOAP service installers
   */
  public LinkedList<WebServiceInstaller> getWebServiceInstallerList () {

    return webServiceInstallerList;
  }

  /**
   * Registers a servlet context listener to install.
   *
   * @param listenerInstaller listener installer
   */
  public void addListenerInstaller (ListenerInstaller listenerInstaller) {

    listenerInstallerList.add(listenerInstaller);
  }

  /**
   * @return collected listener installers
   */
  public LinkedList<ListenerInstaller> getListenerInstallerList () {

    return listenerInstallerList;
  }

  /**
   * Registers a servlet filter to install.
   *
   * @param filterInstaller filter installer
   */
  public void addFilterInstaller (FilterInstaller filterInstaller) {

    filterInstallerList.add(filterInstaller);
  }

  /**
   * @return collected filter installers
   */
  public LinkedList<FilterInstaller> getFilterInstallerList () {

    return filterInstallerList;
  }

  /**
   * Registers a servlet to install.
   *
   * @param servletInstaller servlet installer
   */
  public void addServletInstaller (ServletInstaller servletInstaller) {

    servletInstallerList.add(servletInstaller);
  }

  /**
   * @return collected servlet installers
   */
  public LinkedList<ServletInstaller> getServletInstallerList () {

    return servletInstallerList;
  }

  /**
   * @return the WebSocket server container associated with this context
   */
  public TyrusGrizzlyServerContainer getTyrusGrizzlyServerContainer () {

    return tyrusGrizzlyServerContainer;
  }

  /**
   * @param tyrusGrizzlyServerContainer initialized WebSocket container to store
   */
  public void setTyrusGrizzlyServerContainer (TyrusGrizzlyServerContainer tyrusGrizzlyServerContainer) {

    this.tyrusGrizzlyServerContainer = tyrusGrizzlyServerContainer;
  }
}
