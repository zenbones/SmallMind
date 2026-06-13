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
package org.smallmind.web.jetty;

import java.util.HashMap;
import java.util.Map;
import org.smallmind.web.jetty.installer.FilterInstaller;
import org.smallmind.web.jetty.installer.ListenerInstaller;
import org.smallmind.web.jetty.installer.ServletInstaller;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration coverage for {@link JettyPostProcessor}, exercising the routing of installer and
 * {@link ServicePath}-annotated beans into a {@link JettyWebAppStateLocator}, including the
 * before-locator queuing branch, the after-locator flush, and the ignore-unrelated-bean path.
 */
@Test(groups = "integration")
public class JettyPostProcessorTest {

  @ServicePath(value = "/quote", context = "/finance")
  public static class AnnotatedService {

  }

  private static class CapturingLocator implements JettyWebAppStateLocator {

    private final Map<String, JettyWebAppState> stateMap = new HashMap<>();

    @Override
    public JettyWebAppState webAppStateFor (String context) {

      return stateMap.computeIfAbsent(context, key -> new JettyWebAppState());
    }
  }

  public void testRoutesInstallersWhenLocatorPresentFirst () {

    JettyPostProcessor postProcessor = new JettyPostProcessor();
    CapturingLocator locator = new CapturingLocator();

    ListenerInstaller listenerInstaller = new ListenerInstaller();
    FilterInstaller filterInstaller = new FilterInstaller();
    ServletInstaller servletInstaller = new ServletInstaller();

    listenerInstaller.setContextPath("/app");
    filterInstaller.setContextPath("/app");
    servletInstaller.setContextPath("/app");

    Assert.assertSame(postProcessor.postProcessAfterInitialization(locator, "locator"), locator);
    postProcessor.postProcessAfterInitialization(listenerInstaller, "listener");
    postProcessor.postProcessAfterInitialization(filterInstaller, "filter");
    postProcessor.postProcessAfterInitialization(servletInstaller, "servlet");

    JettyWebAppState state = locator.webAppStateFor("/app");

    Assert.assertSame(state.getListenerInstallerList().getFirst(), listenerInstaller);
    Assert.assertSame(state.getFilterInstallerList().getFirst(), filterInstaller);
    Assert.assertSame(state.getServletInstallerList().getFirst(), servletInstaller);
  }

  public void testRoutesServicePathAnnotatedBean () {

    JettyPostProcessor postProcessor = new JettyPostProcessor();
    CapturingLocator locator = new CapturingLocator();
    AnnotatedService service = new AnnotatedService();

    postProcessor.postProcessAfterInitialization(locator, "locator");
    Assert.assertSame(postProcessor.postProcessAfterInitialization(service, "service"), service);

    JettyWebAppState state = locator.webAppStateFor("/finance");

    Assert.assertEquals(state.getWebServiceInstallerList().size(), 1);
    Assert.assertEquals(state.getWebServiceInstallerList().getFirst().getPath(), "/quote");
    Assert.assertSame(state.getWebServiceInstallerList().getFirst().getService(), service);
  }

  public void testQueuesBeansUntilLocatorArrives () {

    JettyPostProcessor postProcessor = new JettyPostProcessor();
    CapturingLocator locator = new CapturingLocator();

    ListenerInstaller earlyListener = new ListenerInstaller();
    ServletInstaller lateServlet = new ServletInstaller();

    earlyListener.setContextPath("/early");
    lateServlet.setContextPath("/late");

    // Arrives before the locator is known and must be held back.
    postProcessor.postProcessAfterInitialization(earlyListener, "early");
    Assert.assertTrue(locator.webAppStateFor("/early").getListenerInstallerList().isEmpty());

    // The locator now appears; the next non-locator bean triggers the flush plus its own processing.
    postProcessor.postProcessAfterInitialization(locator, "locator");
    postProcessor.postProcessAfterInitialization(lateServlet, "late");

    Assert.assertSame(locator.webAppStateFor("/early").getListenerInstallerList().getFirst(), earlyListener);
    Assert.assertSame(locator.webAppStateFor("/late").getServletInstallerList().getFirst(), lateServlet);
  }

  public void testIgnoresUnrelatedBeanAndReturnsIt () {

    JettyPostProcessor postProcessor = new JettyPostProcessor();
    CapturingLocator locator = new CapturingLocator();
    Object plainBean = new Object();

    postProcessor.postProcessAfterInitialization(locator, "locator");

    Assert.assertSame(postProcessor.postProcessAfterInitialization(plainBean, "plain"), plainBean);
  }

  public void testUnrelatedBeanBeforeLocatorIsNotQueued () {

    JettyPostProcessor postProcessor = new JettyPostProcessor();
    CapturingLocator locator = new CapturingLocator();
    ServletInstaller queuedServlet = new ServletInstaller();

    queuedServlet.setContextPath("/q");

    // An unrelated bean arriving before the locator is neither queued nor routed.
    postProcessor.postProcessAfterInitialization(new Object(), "plain");
    postProcessor.postProcessAfterInitialization(queuedServlet, "queued");
    postProcessor.postProcessAfterInitialization(locator, "locator");
    postProcessor.postProcessAfterInitialization(new ServletInstaller(), "trigger");

    Assert.assertSame(locator.webAppStateFor("/q").getServletInstallerList().getFirst(), queuedServlet);
  }
}
