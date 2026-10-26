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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.ServletContextEvent;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link ExposedWebContextLoaderListener} on a thread that inherits no {@link PerApplicationContext}: the
 * listener must publish a context under the servlet context attribute, bind the refresh thread to it, and expose the
 * refreshed web application context through {@link ExposedApplicationContext}.
 */
@Test(groups = "unit")
public class ExposedWebContextLoaderListenerTest {

  private static final AtomicReference<Boolean> BOUND_ON_REFRESH = new AtomicReference<>();


  @BeforeMethod
  public void setUp () {

    BOUND_ON_REFRESH.set(null);
  }

  public void testRefreshPublishesAContextBindsTheRefreshThreadAndExposesTheContext ()
    throws Exception {

    onIsolatedThread(() -> {

      MockServletContext servletContext = servletContext();
      ExposedWebContextLoaderListener listener = new ExposedWebContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);

      Assert.assertFalse(PerApplicationContext.isBound(), "the refresh thread must start out unbound for this case to mean anything");

      listener.contextInitialized(event);
      try {

        WebApplicationContext webApplicationContext = (WebApplicationContext)servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        Object published = servletContext.getAttribute(PerApplicationContext.class.getName());

        Assert.assertNotNull(webApplicationContext);
        Assert.assertNotNull(published);
        Assert.assertTrue(published instanceof PerApplicationContext);
        Assert.assertEquals(BOUND_ON_REFRESH.get(), Boolean.TRUE);
        Assert.assertTrue(PerApplicationContext.isBound(), "the refresh thread stays attached to the published map after initialization");
        Assert.assertEquals(registeredValue((PerApplicationContext)published), "registered-during-refresh");
        Assert.assertSame(ExposedApplicationContext.getApplicationContext(), webApplicationContext);
      } finally {
        listener.contextDestroyed(event);
        // the exposed context is a static; leave none behind for later tests in this JVM
        ExposedApplicationContext.register(null);
      }

      return null;
    });
  }

  private static MockServletContext servletContext () {

    MockServletContext servletContext = new MockServletContext();

    servletContext.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:org/smallmind/web/jersey/spring/exposed-per-application-context-listener.xml");

    return servletContext;
  }

  // Reads through the instance's map from a thread that has nothing else attached to it.
  private static String registeredValue (PerApplicationContext perApplicationContext)
    throws Exception {

    return onIsolatedThread(() -> {
      perApplicationContext.prepareThread();

      return PerApplicationContext.getPerApplicationData(SampleManager.class, String.class);
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

  public static class SampleManager implements PerApplicationDataManager {

  }

  public static class RegisteringBean implements InitializingBean {

    @Override
    public void afterPropertiesSet () {

      BOUND_ON_REFRESH.set(PerApplicationContext.isBound());
      PerApplicationContext.setPerApplicationData(SampleManager.class, "registered-during-refresh");
    }
  }
}
