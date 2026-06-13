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
package org.smallmind.web.jetty.installer;

import java.io.IOException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit coverage for the installer descriptors: type identity, accessor round-trips, the instance-versus-class
 * resolution branches, and the shared {@link JettyInstaller} context-path contract.
 */
@Test(groups = "unit")
public class InstallerTest {

  public static class NoopFilter implements Filter {

    @Override
    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

      chain.doFilter(request, response);
    }
  }

  public static class SampleListener implements EventListener {

  }

  public void testInstallerTypeValues () {

    Assert.assertEquals(JettyInstallerType.valueOf("FILTER"), JettyInstallerType.FILTER);
    Assert.assertEquals(JettyInstallerType.valueOf("LISTENER"), JettyInstallerType.LISTENER);
    Assert.assertEquals(JettyInstallerType.valueOf("SERVLET"), JettyInstallerType.SERVLET);
    Assert.assertEquals(JettyInstallerType.values().length, 3);
  }

  public void testFilterInstallerType () {

    Assert.assertEquals(new FilterInstaller().getOptionType(), JettyInstallerType.FILTER);
  }

  public void testFilterInstallerAccessors () {

    FilterInstaller installer = new FilterInstaller();
    Map<String, String> initParameters = new HashMap<>();

    initParameters.put("a", "b");

    installer.setDisplayName("display");
    installer.setUrlPattern("/x/*");
    installer.setInitParameters(initParameters);
    installer.setAsyncSupported(Boolean.TRUE);
    installer.setMatchAfter(true);
    installer.setContextPath("/ctx");

    Assert.assertEquals(installer.getDisplayName(), "display");
    Assert.assertEquals(installer.getUrlPattern(), "/x/*");
    Assert.assertSame(installer.getInitParameters(), initParameters);
    Assert.assertEquals(installer.getAsyncSupported(), Boolean.TRUE);
    Assert.assertTrue(installer.isMatchAfter());
    Assert.assertEquals(installer.getContextPath(), "/ctx");
  }

  public void testFilterInstallerDefaults () {

    FilterInstaller installer = new FilterInstaller();

    Assert.assertNull(installer.getDisplayName());
    Assert.assertNull(installer.getUrlPattern());
    Assert.assertNull(installer.getInitParameters());
    Assert.assertNull(installer.getAsyncSupported());
    Assert.assertNull(installer.getContextPath());
    Assert.assertFalse(installer.isMatchAfter());
  }

  public void testFilterInstallerReturnsSuppliedInstance ()
    throws InstantiationException, IllegalAccessException {

    FilterInstaller installer = new FilterInstaller();
    NoopFilter filter = new NoopFilter();

    installer.setFilter(filter);

    Assert.assertSame(installer.getFilter(), filter);
  }

  public void testFilterInstallerInstantiatesClass ()
    throws InstantiationException, IllegalAccessException {

    FilterInstaller installer = new FilterInstaller();

    installer.setFilterClass(NoopFilter.class);

    Assert.assertTrue(installer.getFilter() instanceof NoopFilter);
  }

  public void testServletInstallerType () {

    Assert.assertEquals(new ServletInstaller().getOptionType(), JettyInstallerType.SERVLET);
  }

  public void testServletInstallerAccessors () {

    ServletInstaller installer = new ServletInstaller();
    Map<String, String> initParameters = new HashMap<>();

    initParameters.put("k", "v");

    installer.setDisplayName("name");
    installer.setUrlPattern("/svc/*");
    installer.setInitParameters(initParameters);
    installer.setLoadOnStartup(3);
    installer.setAsyncSupported(Boolean.FALSE);
    installer.setContextPath("/app");

    Assert.assertEquals(installer.getDisplayName(), "name");
    Assert.assertEquals(installer.getUrlPattern(), "/svc/*");
    Assert.assertSame(installer.getInitParameters(), initParameters);
    Assert.assertEquals(installer.getLoadOnStartup(), Integer.valueOf(3));
    Assert.assertEquals(installer.getAsyncSupported(), Boolean.FALSE);
    Assert.assertEquals(installer.getContextPath(), "/app");
  }

  public void testServletInstallerDefaults () {

    ServletInstaller installer = new ServletInstaller();

    Assert.assertNull(installer.getDisplayName());
    Assert.assertNull(installer.getUrlPattern());
    Assert.assertNull(installer.getInitParameters());
    Assert.assertNull(installer.getLoadOnStartup());
    Assert.assertNull(installer.getAsyncSupported());
  }

  public void testListenerInstallerType () {

    Assert.assertEquals(new ListenerInstaller().getOptionType(), JettyInstallerType.LISTENER);
  }

  public void testListenerInstallerAccessors () {

    ListenerInstaller installer = new ListenerInstaller();
    Map<String, String> contextParameters = new HashMap<>();

    contextParameters.put("c", "d");

    installer.setContextParameters(contextParameters);
    installer.setContextPath("/listen");

    Assert.assertSame(installer.getContextParameters(), contextParameters);
    Assert.assertEquals(installer.getContextPath(), "/listen");
  }

  public void testListenerInstallerReturnsSuppliedInstance ()
    throws NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {

    ListenerInstaller installer = new ListenerInstaller();
    SampleListener listener = new SampleListener();

    installer.setEventListener(listener);

    Assert.assertSame(installer.getListener(), listener);
  }

  public void testListenerInstallerInstantiatesClass ()
    throws NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {

    ListenerInstaller installer = new ListenerInstaller();

    installer.setListenerClass(SampleListener.class);

    Assert.assertTrue(installer.getListener() instanceof SampleListener);
  }

  public void testWebServiceInstallerConstructorAndAccessors () {

    Object service = new Object();
    WebServiceInstaller installer = new WebServiceInstaller("/path", service);

    Assert.assertEquals(installer.getPath(), "/path");
    Assert.assertSame(installer.getService(), service);
    Assert.assertNull(installer.getAsyncSupported());

    Object replacement = new Object();

    installer.setPath("/other");
    installer.setService(replacement);
    installer.setAsyncSupported(Boolean.TRUE);

    Assert.assertEquals(installer.getPath(), "/other");
    Assert.assertSame(installer.getService(), replacement);
    Assert.assertEquals(installer.getAsyncSupported(), Boolean.TRUE);
  }
}
