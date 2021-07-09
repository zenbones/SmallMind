/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import org.smallmind.web.grizzly.tyrus.TyrusWebSocketAddOn;

public class GrizzlyWebAppState {

  private final LinkedList<WebSocketExtensionInstaller> webSocketExtensionInstallerList = new LinkedList<>();
  private final LinkedList<WebServiceInstaller> webServiceInstallerList = new LinkedList<>();
  private final LinkedList<ListenerInstaller> listenerInstallerList = new LinkedList<>();
  private final LinkedList<FilterInstaller> filterInstallerList = new LinkedList<>();
  private final LinkedList<ServletInstaller> servletInstallerList = new LinkedList<>();
  private final WebappContext webAppContext;
  private TyrusWebSocketAddOn tyrusWebSocketAddOn;

  public GrizzlyWebAppState (WebappContext webAppContext) {

    this.webAppContext = webAppContext;
  }

  public WebappContext getWebAppContext () {

    return webAppContext;
  }

  public void addWebSocketExtensionInstaller (WebSocketExtensionInstaller webSocketExtensionInstaller) {

    webSocketExtensionInstallerList.add(webSocketExtensionInstaller);
  }

  public LinkedList<WebSocketExtensionInstaller> getWebSocketExtensionInstallerList () {

    return webSocketExtensionInstallerList;
  }

  public void addWebServiceInstaller (WebServiceInstaller webServiceInstaller) {

    webServiceInstallerList.add(webServiceInstaller);
  }

  public LinkedList<WebServiceInstaller> getWebServiceInstallerList () {

    return webServiceInstallerList;
  }

  public void addListenerInstaller (ListenerInstaller listenerInstaller) {

    listenerInstallerList.add(listenerInstaller);
  }

  public LinkedList<ListenerInstaller> getListenerInstallerList () {

    return listenerInstallerList;
  }

  public void addFilterInstaller (FilterInstaller filterInstaller) {

    filterInstallerList.add(filterInstaller);
  }

  public LinkedList<FilterInstaller> getFilterInstallerList () {

    return filterInstallerList;
  }

  public void addServletInstaller (ServletInstaller servletInstaller) {

    servletInstallerList.add(servletInstaller);
  }

  public LinkedList<ServletInstaller> getServletInstallerList () {

    return servletInstallerList;
  }

  public TyrusWebSocketAddOn getTyrusWebSocketAddOn () {

    return tyrusWebSocketAddOn;
  }

  public TyrusWebSocketAddOn setTyrusWebSocketAddOn (TyrusWebSocketAddOn tyrusWebSocketAddOn) {

    this.tyrusWebSocketAddOn = tyrusWebSocketAddOn;

    return tyrusWebSocketAddOn;
  }
}
