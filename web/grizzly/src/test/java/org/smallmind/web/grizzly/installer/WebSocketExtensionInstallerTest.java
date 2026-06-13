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
package org.smallmind.web.grizzly.installer;

import java.util.Collections;
import java.util.List;
import jakarta.websocket.Extension;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class WebSocketExtensionInstallerTest {

  public static class NamedExtension implements Extension {

    private final String name;

    public NamedExtension (String name) {

      this.name = name;
    }

    @Override
    public String getName () {

      return name;
    }

    @Override
    public List<Parameter> getParameters () {

      return Collections.emptyList();
    }
  }

  public void testOptionTypeIsWebSocketExtension () {

    Assert.assertEquals(new WebSocketExtensionInstaller().getOptionType(), GrizzlyInstallerType.WEB_SOCKET_EXTENSION);
  }

  public void testDefaultsForUnsetFields () {

    WebSocketExtensionInstaller installer = new WebSocketExtensionInstaller();

    Assert.assertNull(installer.getExtensions());
    Assert.assertNull(installer.getEndpointClass());
    Assert.assertNull(installer.getPath());
    Assert.assertNull(installer.getContextPath());
  }

  public void testAccessorRoundTrip () {

    WebSocketExtensionInstaller installer = new WebSocketExtensionInstaller();
    Extension[] extensions = new Extension[] {new NamedExtension("permessage-deflate")};

    installer.setExtensions(extensions);
    installer.setEndpointClass(WebSocketExtensionInstallerTest.class);
    installer.setPath("/socket");
    installer.setContextPath("/context");

    Assert.assertSame(installer.getExtensions(), extensions);
    Assert.assertEquals(installer.getExtensions().length, 1);
    Assert.assertEquals(installer.getExtensions()[0].getName(), "permessage-deflate");
    Assert.assertEquals(installer.getEndpointClass(), WebSocketExtensionInstallerTest.class);
    Assert.assertEquals(installer.getPath(), "/socket");
    Assert.assertEquals(installer.getContextPath(), "/context");
  }
}
