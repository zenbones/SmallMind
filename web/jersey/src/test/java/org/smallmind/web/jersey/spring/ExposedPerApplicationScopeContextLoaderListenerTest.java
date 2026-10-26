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
package org.smallmind.web.jersey.spring;

import jakarta.servlet.ServletContextEvent;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ExposedPerApplicationScopeContextLoaderListenerTest {

  public void testRefreshRunsInsideTheScopeAndExposesTheContext () {

    MockServletContext servletContext = servletContext();
    PerApplicationScope perApplicationScope = new PerApplicationScope();
    ExposedPerApplicationScopeContextLoaderListener listener = new ExposedPerApplicationScopeContextLoaderListener(perApplicationScope);
    ServletContextEvent event = new ServletContextEvent(servletContext);

    listener.contextInitialized(event);
    try {

      WebApplicationContext webApplicationContext = (WebApplicationContext)servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

      Assert.assertNotNull(webApplicationContext);
      Assert.assertSame(servletContext.getAttribute(PerApplicationScope.class.getName()), perApplicationScope);
      Assert.assertEquals(registeredValue(perApplicationScope), "registered-during-refresh");
      Assert.assertSame(ExposedApplicationContext.getApplicationContext(), webApplicationContext);
    } finally {
      listener.contextDestroyed(event);
    }

    try {
      PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      Assert.fail("The binding should not survive contextInitialized");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  public void testNoArgListenerCreatesAndPublishesAScope () {

    MockServletContext servletContext = servletContext();
    ExposedPerApplicationScopeContextLoaderListener listener = new ExposedPerApplicationScopeContextLoaderListener();
    ServletContextEvent event = new ServletContextEvent(servletContext);

    listener.contextInitialized(event);
    try {

      Object published = servletContext.getAttribute(PerApplicationScope.class.getName());

      Assert.assertTrue(published instanceof PerApplicationScope);
      Assert.assertEquals(registeredValue((PerApplicationScope)published), "registered-during-refresh");
      Assert.assertNotNull(ExposedApplicationContext.getApplicationContext());
    } finally {
      listener.contextDestroyed(event);
    }
  }

  private static MockServletContext servletContext () {

    MockServletContext servletContext = new MockServletContext();

    servletContext.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:org/smallmind/web/jersey/spring/exposed-per-application-scope-listener.xml");

    return servletContext;
  }

  private static String registeredValue (PerApplicationScope scope) {

    return scope.call(() -> PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  public static class RegisteringBean implements InitializingBean {

    @Override
    public void afterPropertiesSet () {

      PerApplicationScope.setPerApplicationData(SampleManager.class, "registered-during-refresh");
    }
  }
}
