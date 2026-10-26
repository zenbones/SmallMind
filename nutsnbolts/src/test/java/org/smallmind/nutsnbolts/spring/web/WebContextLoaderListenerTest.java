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
import jakarta.servlet.ServletContextEvent;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises how {@link WebContextLoaderListener} binds the refresh thread: with nothing published it constructs and
 * publishes a {@link PerApplicationContext}, and with one already published it attaches that instance's map to the
 * refresh thread; on destruction it attaches the published map to the destroying thread. Every case runs on a thread
 * that inherits no {@link PerApplicationContext}, so the map shared on the calling thread can not stand in for the
 * listener's work.
 */
@Test(groups = "unit")
public class WebContextLoaderListenerTest {

  private static final AtomicReference<Boolean> BOUND_ON_REFRESH = new AtomicReference<>();
  private static final AtomicReference<String> SEEN_ON_REFRESH = new AtomicReference<>();
  private static final AtomicReference<Boolean> BOUND_ON_DESTROY = new AtomicReference<>();
  private static final AtomicReference<String> SEEN_ON_DESTROY = new AtomicReference<>();


  @BeforeMethod
  public void setUp () {

    BOUND_ON_REFRESH.set(null);
    SEEN_ON_REFRESH.set(null);
    BOUND_ON_DESTROY.set(null);
    SEEN_ON_DESTROY.set(null);
  }

  public void testRefreshPublishesAContextAndBindsTheRefreshThread ()
    throws Exception {

    onIsolatedThread(() -> {

      MockServletContext servletContext = servletContext();
      WebContextLoaderListener listener = new WebContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);

      Assert.assertFalse(PerApplicationContext.isBound(), "the refresh thread must start out unbound for this case to mean anything");

      listener.contextInitialized(event);
      try {

        Object published = servletContext.getAttribute(PerApplicationContext.class.getName());

        Assert.assertNotNull(published);
        Assert.assertTrue(published instanceof PerApplicationContext);
        Assert.assertEquals(BOUND_ON_REFRESH.get(), Boolean.TRUE);
        Assert.assertNull(SEEN_ON_REFRESH.get(), "a freshly constructed context starts empty");
        Assert.assertEquals(registeredValue((PerApplicationContext)published), "registered-during-refresh");
        Assert.assertNotNull(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE));
      } finally {
        listener.contextDestroyed(event);
      }

      return null;
    });
  }

  public void testPublishedContextIsAttachedToTheRefreshThread ()
    throws Exception {

    MockServletContext servletContext = servletContext();
    PerApplicationContext prePublished = onIsolatedThread(() -> {

      PerApplicationContext perApplicationContext = new PerApplicationContext();

      PerApplicationContext.setPerApplicationData(SampleManager.class, "pre-published");

      return perApplicationContext;
    });

    servletContext.setAttribute(PerApplicationContext.class.getName(), prePublished);

    onIsolatedThread(() -> {

      WebContextLoaderListener listener = new WebContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);

      listener.contextInitialized(event);
      try {
        Assert.assertSame(servletContext.getAttribute(PerApplicationContext.class.getName()), prePublished);
        Assert.assertEquals(BOUND_ON_REFRESH.get(), Boolean.TRUE);
        Assert.assertEquals(SEEN_ON_REFRESH.get(), "pre-published");
        Assert.assertEquals(registeredValue(prePublished), "registered-during-refresh");
      } finally {
        listener.contextDestroyed(event);
      }

      return null;
    });
  }

  // The refresh thread already carries a map of its own, yet the published instance still wins: the bean reads and
  // writes through the published map during the refresh, and the map the thread arrived with is left untouched.
  public void testPublishedContextWinsOverTheMapAlreadyAttachedToTheRefreshThread ()
    throws Exception {

    MockServletContext servletContext = servletContext();
    PerApplicationContext prePublished = onIsolatedThread(() -> {

      PerApplicationContext perApplicationContext = new PerApplicationContext();

      PerApplicationContext.setPerApplicationData(SampleManager.class, "pre-published");

      return perApplicationContext;
    });

    servletContext.setAttribute(PerApplicationContext.class.getName(), prePublished);

    PerApplicationContext alreadyAttached = onIsolatedThread(() -> {

      PerApplicationContext perApplicationContext = new PerApplicationContext();
      WebContextLoaderListener listener = new WebContextLoaderListener();
      ServletContextEvent event = new ServletContextEvent(servletContext);

      PerApplicationContext.setPerApplicationData(SampleManager.class, "already-attached");

      listener.contextInitialized(event);
      try {
        Assert.assertSame(servletContext.getAttribute(PerApplicationContext.class.getName()), prePublished);
        Assert.assertEquals(BOUND_ON_REFRESH.get(), Boolean.TRUE);
        Assert.assertEquals(SEEN_ON_REFRESH.get(), "pre-published", "the bean must read through the published map, not the one the thread arrived with");
        Assert.assertEquals(PerApplicationContext.getPerApplicationData(SampleManager.class, String.class), "registered-during-refresh", "the refresh thread stays attached to the published map");
      } finally {
        listener.contextDestroyed(event);
      }

      return perApplicationContext;
    });

    Assert.assertEquals(registeredValue(prePublished), "registered-during-refresh");
    Assert.assertEquals(registeredValue(alreadyAttached), "already-attached", "the map the refresh thread arrived with must not receive the registration");
  }

  // The container may destroy on a thread other than the one that refreshed; contextDestroyed() attaches the
  // published map to whichever thread it runs on, so a bean's destroy callback reads the registrations the refresh
  // made even though the destroying thread arrived with nothing attached.
  public void testContextDestroyedAttachesThePublishedContextToTheDestroyingThread ()
    throws Exception {

    MockServletContext servletContext = servletContext();
    WebContextLoaderListener listener = new WebContextLoaderListener();
    ServletContextEvent event = new ServletContextEvent(servletContext);

    onIsolatedThread(() -> {
      listener.contextInitialized(event);

      return null;
    });

    Assert.assertEquals(BOUND_ON_REFRESH.get(), Boolean.TRUE);

    onIsolatedThread(() -> {
      Assert.assertFalse(PerApplicationContext.isBound(), "the destroying thread must start out unbound for this case to mean anything");
      listener.contextDestroyed(event);

      return null;
    });

    Assert.assertEquals(BOUND_ON_DESTROY.get(), Boolean.TRUE, "destroy must run with the published context attached to the destroying thread");
    Assert.assertEquals(SEEN_ON_DESTROY.get(), "registered-during-refresh", "the destroying thread must carry the published map, not an empty one of its own");
  }

  private static MockServletContext servletContext () {

    MockServletContext servletContext = new MockServletContext();

    servletContext.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "classpath:org/smallmind/nutsnbolts/spring/web/per-application-context-listener.xml");

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

  public static class ContextRegisteringBean implements InitializingBean {

    @Override
    public void afterPropertiesSet () {

      BOUND_ON_REFRESH.set(PerApplicationContext.isBound());
      SEEN_ON_REFRESH.set(PerApplicationContext.getPerApplicationData(SampleManager.class, String.class));
      PerApplicationContext.setPerApplicationData(SampleManager.class, "registered-during-refresh");
    }
  }

  public static class ContextRecordingDisposableBean implements DisposableBean {

    @Override
    public void destroy () {

      BOUND_ON_DESTROY.set(PerApplicationContext.isBound());
      SEEN_ON_DESTROY.set(PerApplicationContext.isBound() ? PerApplicationContext.getPerApplicationData(SampleManager.class, String.class) : null);
    }
  }
}
