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

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ServletInstallerTest {

  public static class NoOpServlet extends GenericServlet {

    @Override
    public void service (ServletRequest request, ServletResponse response) {

    }
  }

  public void testOptionTypeIsServlet () {

    Assert.assertEquals(new ServletInstaller().getOptionType(), GrizzlyInstallerType.SERVLET);
  }

  public void testDefaultsForUnsetFields () {

    ServletInstaller installer = new ServletInstaller();

    Assert.assertNull(installer.getDisplayName());
    Assert.assertNull(installer.getUrlPattern());
    Assert.assertNull(installer.getInitParameters());
    Assert.assertNull(installer.getLoadOnStartup());
    Assert.assertNull(installer.getAsyncSupported());
    Assert.assertNull(installer.getContextPath());
  }

  public void testAccessorRoundTrip () {

    ServletInstaller installer = new ServletInstaller();
    Map<String, String> initParameters = new HashMap<>();

    initParameters.put("key", "value");

    installer.setDisplayName("servlet-name");
    installer.setUrlPattern("/path/*");
    installer.setInitParameters(initParameters);
    installer.setLoadOnStartup(3);
    installer.setAsyncSupported(Boolean.FALSE);
    installer.setContextPath("/context");

    Assert.assertEquals(installer.getDisplayName(), "servlet-name");
    Assert.assertEquals(installer.getUrlPattern(), "/path/*");
    Assert.assertSame(installer.getInitParameters(), initParameters);
    Assert.assertEquals(installer.getLoadOnStartup(), Integer.valueOf(3));
    Assert.assertEquals(installer.getAsyncSupported(), Boolean.FALSE);
    Assert.assertEquals(installer.getContextPath(), "/context");
  }

  public void testGetServletReturnsConfiguredInstance ()
    throws InstantiationException, IllegalAccessException {

    ServletInstaller installer = new ServletInstaller();
    NoOpServlet servlet = new NoOpServlet();

    installer.setServlet(servlet);

    Assert.assertSame(installer.getServlet(), servlet);
  }

  public void testGetServletInstantiatesFromClassWhenNoInstance ()
    throws InstantiationException, IllegalAccessException {

    ServletInstaller installer = new ServletInstaller();

    installer.setServletClass(NoOpServlet.class);

    Servlet servlet = installer.getServlet();

    Assert.assertNotNull(servlet);
    Assert.assertTrue(servlet instanceof NoOpServlet);
  }

  public void testGetServletPrefersInstanceOverClass ()
    throws InstantiationException, IllegalAccessException {

    ServletInstaller installer = new ServletInstaller();
    NoOpServlet servlet = new NoOpServlet();

    installer.setServlet(servlet);
    installer.setServletClass(NoOpServlet.class);

    Assert.assertSame(installer.getServlet(), servlet);
  }
}
