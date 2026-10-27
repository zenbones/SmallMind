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
package org.smallmind.nutsnbolts.spring.web;

import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.ServletContextEvent;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PerApplicationScopeContextLoaderListenerTest {

  private static final AtomicReference<String> SEEN_ON_DESTROY = new AtomicReference<>();

  private static MockServletContext servletContext () {

    MockServletContext servletContext = new MockServletContext();

    servletContext.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:org/smallmind/nutsnbolts/spring/web/per-application-scope-listener.xml");

    return servletContext;
  }

  private static String registeredValue (PerApplicationScope scope) {

    return scope.call(() -> PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
  }

  private static void assertUnbound () {

    try {
      PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      Assert.fail("The binding should not survive contextInitialized");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  @BeforeMethod
  public void setUp () {

    SEEN_ON_DESTROY.set(null);
  }

  public void testRefreshRunsInsideScopeAndPublishesAttribute () {

    MockServletContext servletContext = servletContext();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener();
    ServletContextEvent event = new ServletContextEvent(servletContext);

    listener.contextInitialized(event);
    try {

      Object published = servletContext.getAttribute(PerApplicationScope.class.getName());

      Assert.assertNotNull(published);
      Assert.assertTrue(published instanceof PerApplicationScope);
      Assert.assertEquals(registeredValue((PerApplicationScope)published), "registered-during-refresh");
      Assert.assertNotNull(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE));
    } finally {
      listener.contextDestroyed(event);
    }

    Assert.assertEquals(SEEN_ON_DESTROY.get(), "registered-during-refresh");
  }

  public void testCallingThreadIsUnboundAfterInitialized () {

    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener();
    ServletContextEvent event = new ServletContextEvent(servletContext());

    listener.contextInitialized(event);
    try {
      assertUnbound();
    } finally {
      listener.contextDestroyed(event);
    }
  }

  public void testExistingAttributeIsReused () {

    MockServletContext servletContext = servletContext();
    PerApplicationScope prePublished = new PerApplicationScope();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener();
    ServletContextEvent event = new ServletContextEvent(servletContext);

    servletContext.setAttribute(PerApplicationScope.class.getName(), prePublished);
    listener.contextInitialized(event);
    try {
      Assert.assertSame(servletContext.getAttribute(PerApplicationScope.class.getName()), prePublished);
      Assert.assertEquals(registeredValue(prePublished), "registered-during-refresh");
    } finally {
      listener.contextDestroyed(event);
    }
  }

  public void testInjectedScopeIsPublishedAndUsed () {

    MockServletContext servletContext = servletContext();
    PerApplicationScope injected = new PerApplicationScope();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener(injected);
    ServletContextEvent event = new ServletContextEvent(servletContext);

    listener.contextInitialized(event);
    try {
      Assert.assertSame(servletContext.getAttribute(PerApplicationScope.class.getName()), injected);
      Assert.assertEquals(registeredValue(injected), "registered-during-refresh");
    } finally {
      listener.contextDestroyed(event);
    }
  }

  public void testInjectedScopeConflictingWithPublishedScopeFailsFast () {

    MockServletContext servletContext = servletContext();
    PerApplicationScope prePublished = new PerApplicationScope();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener(new PerApplicationScope());

    servletContext.setAttribute(PerApplicationScope.class.getName(), prePublished);
    try {
      listener.contextInitialized(new ServletContextEvent(servletContext));
      Assert.fail("Two different scopes for one servlet context is contradictory wiring");
    } catch (ScopeException scopeException) {
      // expected
    }

    Assert.assertSame(servletContext.getAttribute(PerApplicationScope.class.getName()), prePublished);
    Assert.assertNull(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE), "Spring must not have been refreshed");
  }

  public void testBoundScopeIsAdoptedWhenNothingIsHandedOrPublished () {

    MockServletContext servletContext = servletContext();
    PerApplicationScope enclosing = new PerApplicationScope();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener();
    ServletContextEvent event = new ServletContextEvent(servletContext);

    enclosing.run(() -> listener.contextInitialized(event));
    try {
      Assert.assertSame(servletContext.getAttribute(PerApplicationScope.class.getName()), enclosing);
      Assert.assertEquals(registeredValue(enclosing), "registered-during-refresh");
    } finally {
      listener.contextDestroyed(event);
    }
  }

  public void testInjectedScopeMatchingTheBoundScopeIsUsed () {

    MockServletContext servletContext = servletContext();
    PerApplicationScope enclosing = new PerApplicationScope();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener(enclosing);
    ServletContextEvent event = new ServletContextEvent(servletContext);

    enclosing.run(() -> listener.contextInitialized(event));
    try {
      Assert.assertSame(servletContext.getAttribute(PerApplicationScope.class.getName()), enclosing);
      Assert.assertEquals(registeredValue(enclosing), "registered-during-refresh");
    } finally {
      listener.contextDestroyed(event);
    }
  }

  public void testInjectedScopeConflictingWithBoundScopeFailsFast () {

    MockServletContext servletContext = servletContext();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener(new PerApplicationScope());

    new PerApplicationScope().run(() -> {
      try {
        listener.contextInitialized(new ServletContextEvent(servletContext));
        Assert.fail("A handed scope that is not the one bound on the initializing thread is contradictory wiring");
      } catch (ScopeException scopeException) {
        // expected
      }
    });

    Assert.assertNull(servletContext.getAttribute(PerApplicationScope.class.getName()), "Nothing must have been published");
    Assert.assertNull(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE), "Spring must not have been refreshed");
  }

  public void testPublishedScopeConflictingWithBoundScopeFailsFast () {

    MockServletContext servletContext = servletContext();
    PerApplicationScope prePublished = new PerApplicationScope();
    PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener();

    servletContext.setAttribute(PerApplicationScope.class.getName(), prePublished);
    new PerApplicationScope().run(() -> {
      try {
        listener.contextInitialized(new ServletContextEvent(servletContext));
        Assert.fail("A published scope that is not the one bound on the initializing thread is contradictory wiring");
      } catch (ScopeException scopeException) {
        // expected
      }
    });

    Assert.assertSame(servletContext.getAttribute(PerApplicationScope.class.getName()), prePublished);
    Assert.assertNull(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE), "Spring must not have been refreshed");
  }

  public void testContextDestroyedWithoutInitializedDelegates () {

    new PerApplicationScopeContextLoaderListener().contextDestroyed(new ServletContextEvent(new MockServletContext()));
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  public static class ScopeRegisteringBean implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet () {

      PerApplicationScope.setPerApplicationData(SampleManager.class, "registered-during-refresh");
    }

    @Override
    public void destroy () {

      try {
        SEEN_ON_DESTROY.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
      } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
        SEEN_ON_DESTROY.set("unbound");
      }
    }
  }
}
