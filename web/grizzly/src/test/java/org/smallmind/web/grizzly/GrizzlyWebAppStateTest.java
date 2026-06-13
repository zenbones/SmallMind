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

import org.glassfish.grizzly.servlet.WebappContext;
import org.smallmind.web.grizzly.installer.FilterInstaller;
import org.smallmind.web.grizzly.installer.ListenerInstaller;
import org.smallmind.web.grizzly.installer.ServletInstaller;
import org.smallmind.web.grizzly.installer.WebServiceInstaller;
import org.smallmind.web.grizzly.installer.WebSocketExtensionInstaller;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class GrizzlyWebAppStateTest {

  private GrizzlyWebAppState newState () {

    return new GrizzlyWebAppState(new WebappContext("Test Context", "/context"));
  }

  public void testConstructorStoresWebAppContext () {

    WebappContext webAppContext = new WebappContext("Test Context", "/context");
    GrizzlyWebAppState state = new GrizzlyWebAppState(webAppContext);

    Assert.assertSame(state.getWebAppContext(), webAppContext);
  }

  public void testInstallerListsStartEmpty () {

    GrizzlyWebAppState state = newState();

    Assert.assertTrue(state.getWebSocketExtensionInstallerList().isEmpty());
    Assert.assertTrue(state.getWebServiceInstallerList().isEmpty());
    Assert.assertTrue(state.getListenerInstallerList().isEmpty());
    Assert.assertTrue(state.getFilterInstallerList().isEmpty());
    Assert.assertTrue(state.getServletInstallerList().isEmpty());
  }

  public void testTyrusContainerDefaultsToNull () {

    Assert.assertNull(newState().getTyrusGrizzlyServerContainer());
  }

  public void testAddWebSocketExtensionInstaller () {

    GrizzlyWebAppState state = newState();
    WebSocketExtensionInstaller installer = new WebSocketExtensionInstaller();

    state.addWebSocketExtensionInstaller(installer);

    Assert.assertEquals(state.getWebSocketExtensionInstallerList().size(), 1);
    Assert.assertSame(state.getWebSocketExtensionInstallerList().getFirst(), installer);
  }

  public void testAddWebServiceInstaller () {

    GrizzlyWebAppState state = newState();
    WebServiceInstaller installer = new WebServiceInstaller("/path", new Object());

    state.addWebServiceInstaller(installer);

    Assert.assertEquals(state.getWebServiceInstallerList().size(), 1);
    Assert.assertSame(state.getWebServiceInstallerList().getFirst(), installer);
  }

  public void testAddListenerInstaller () {

    GrizzlyWebAppState state = newState();
    ListenerInstaller installer = new ListenerInstaller();

    state.addListenerInstaller(installer);

    Assert.assertEquals(state.getListenerInstallerList().size(), 1);
    Assert.assertSame(state.getListenerInstallerList().getFirst(), installer);
  }

  public void testAddFilterInstaller () {

    GrizzlyWebAppState state = newState();
    FilterInstaller installer = new FilterInstaller();

    state.addFilterInstaller(installer);

    Assert.assertEquals(state.getFilterInstallerList().size(), 1);
    Assert.assertSame(state.getFilterInstallerList().getFirst(), installer);
  }

  public void testAddServletInstaller () {

    GrizzlyWebAppState state = newState();
    ServletInstaller installer = new ServletInstaller();

    state.addServletInstaller(installer);

    Assert.assertEquals(state.getServletInstallerList().size(), 1);
    Assert.assertSame(state.getServletInstallerList().getFirst(), installer);
  }
}
