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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContextEvent;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.context.web.PerApplicationContextFilter;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.web.PerApplicationScopeFilter;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pairs each context loader listener with the no-arg filter of the same flavor on one {@link MockServletContext}, the
 * way a web.xml would, and checks that a request thread which inherits nothing sees the registration made during the
 * refresh. The cross-flavor pairings show that each filter resolves only its own attribute and fails at {@code init}.
 * Every case runs on a thread that inherits no {@link PerApplicationContext}, so the map shared on the calling thread
 * can not stand in for the listener's work.
 */
@Test(groups = "unit")
public class PerApplicationListenerFilterPairingTest {

  private static MockServletContext servletContext (String configLocation) {

    MockServletContext servletContext = new MockServletContext();

    servletContext.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, configLocation);

    return servletContext;
  }

  private static MockServletContext contextListenerServletContext () {

    return servletContext("classpath:org/smallmind/nutsnbolts/spring/web/per-application-context-listener.xml");
  }

  private static MockServletContext scopeListenerServletContext () {

    return servletContext("classpath:org/smallmind/nutsnbolts/spring/web/per-application-scope-listener.xml");
  }

  // Runs one request through the filter on a thread that inherits nothing, so only the binding the filter makes can
  // let the chain read anything, and checks that the thread was unbound before the filter ran.
  private static String filteredRead (Filter filter, BooleanSupplier bound, Supplier<String> read)
    throws Exception {

    return onIsolatedThread(() -> {

      AtomicReference<String> seen = new AtomicReference<>();

      Assert.assertFalse(bound.getAsBoolean(), "the request thread must start out unbound for the binding the filter makes to be what the chain sees");

      filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(read.get()));

      return seen.get();
    });
  }

  private static <T> T onIsolatedThread (Callable<T> body)
    throws Exception {

    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        result.set(body.call());
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }

    return result.get();
  }

  public void testContextListenerThenNoArgContextFilterBindsRequestThreads ()
    throws Exception {

    onIsolatedThread(() -> {

      MockServletContext servletContext = contextListenerServletContext();
      WebContextLoaderListener listener = new WebContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);
      PerApplicationContextFilter filter = new PerApplicationContextFilter();

      listener.contextInitialized(event);
      try {
        filter.init(new MockFilterConfig(servletContext));

        Assert.assertEquals(filteredRead(filter, PerApplicationContext::isBound, () -> PerApplicationContext.getPerApplicationData(WebContextLoaderListenerTest.SampleManager.class, String.class)), "registered-during-refresh");
      } finally {
        filter.destroy();
        listener.contextDestroyed(event);
      }

      return null;
    });
  }

  public void testScopeListenerThenNoArgScopeFilterBindsRequestThreads ()
    throws Exception {

    onIsolatedThread(() -> {

      MockServletContext servletContext = scopeListenerServletContext();
      PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);
      PerApplicationScopeFilter filter = new PerApplicationScopeFilter();

      listener.contextInitialized(event);
      try {
        filter.init(new MockFilterConfig(servletContext));

        Assert.assertEquals(filteredRead(filter, PerApplicationScope::isBound, () -> PerApplicationScope.getPerApplicationData(PerApplicationScopeContextLoaderListenerTest.SampleManager.class, String.class)), "registered-during-refresh");
      } finally {
        filter.destroy();
        listener.contextDestroyed(event);
      }

      return null;
    });
  }

  public void testContextListenerThenNoArgScopeFilterFailsFastAtInit ()
    throws Exception {

    onIsolatedThread(() -> {

      MockServletContext servletContext = contextListenerServletContext();
      WebContextLoaderListener listener = new WebContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);

      listener.contextInitialized(event);
      try {
        Assert.assertNotNull(servletContext.getAttribute(PerApplicationContext.class.getName()));
        Assert.assertNull(servletContext.getAttribute(PerApplicationScope.class.getName()), "the context listener publishes no scope");

        try {
          new PerApplicationScopeFilter().init(new MockFilterConfig(servletContext));
          Assert.fail("A scope filter must not accept a context attribute in place of a scope");
        } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
          // expected
        }
      } finally {
        listener.contextDestroyed(event);
      }

      return null;
    });
  }

  public void testScopeListenerThenNoArgContextFilterFailsFastAtInit ()
    throws Exception {

    onIsolatedThread(() -> {

      MockServletContext servletContext = scopeListenerServletContext();
      PerApplicationScopeContextLoaderListener listener = new PerApplicationScopeContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);

      listener.contextInitialized(event);
      try {
        Assert.assertNotNull(servletContext.getAttribute(PerApplicationScope.class.getName()));
        Assert.assertNull(servletContext.getAttribute(PerApplicationContext.class.getName()), "the scope listener publishes no context");

        try {
          new PerApplicationContextFilter().init(new MockFilterConfig(servletContext));
          Assert.fail("A context filter must not accept a scope attribute in place of a context");
        } catch (MissingPerApplicationContextException missingPerApplicationContextException) {
          // expected
        }
      } finally {
        listener.contextDestroyed(event);
      }

      return null;
    });
  }
}
