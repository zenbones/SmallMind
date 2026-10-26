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
package org.smallmind.nutsnbolts.context.web;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises how {@link PerApplicationContextFilter} resolves its context: an explicit instance wins, otherwise the
 * servlet-context attribute, and it never creates a context of its own. Each case runs on a thread that inherits no
 * {@link PerApplicationContext}, so the assertions are not confused by the map shared on the calling thread.
 */
@Test(groups = "unit")
public class PerApplicationContextFilterTest {

  public void testNoArgConstructorResolvesServletContextAttribute ()
    throws Exception {

    onIsolatedThread(() -> {

      PerApplicationContext published = new PerApplicationContext();
      MockServletContext servletContext = new MockServletContext();
      PerApplicationContextFilter filter = new PerApplicationContextFilter();
      AtomicReference<String> seen = new AtomicReference<>();

      PerApplicationContext.setPerApplicationData(SampleManager.class, "published");
      servletContext.setAttribute(PerApplicationContext.class.getName(), published);
      filter.init(new MockFilterConfig(servletContext));

      Thread.ofPlatform().name("isolated-filter-chain").daemon(false).inheritInheritableThreadLocals(false).start(() -> {
        try {
          filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationContext.getPerApplicationData(SampleManager.class, String.class)));
        } catch (Exception exception) {
          seen.set(exception.toString());
        }
      }).join();

      Assert.assertEquals(seen.get(), "published");

      return null;
    });
  }

  public void testNoArgConstructorFailsFastWithoutAnAttribute ()
    throws Exception {

    onIsolatedThread(() -> {

      MockServletContext servletContext = new MockServletContext();

      try {
        new PerApplicationContextFilter().init(new MockFilterConfig(servletContext));
        Assert.fail("The filter must not create a context of its own");
      } catch (MissingPerApplicationContextException missingPerApplicationContextException) {
        // expected
      }

      Assert.assertNull(servletContext.getAttribute(PerApplicationContext.class.getName()), "The filter must not publish a context of its own");
      Assert.assertFalse(PerApplicationContext.isBound(), "The filter must not attach a context to the init thread");

      return null;
    });
  }

  public void testExplicitContextIgnoresServletContextAttribute ()
    throws Exception {

    onIsolatedThread(() -> {

      PerApplicationContext explicit = new PerApplicationContext();
      MockServletContext servletContext = new MockServletContext();
      PerApplicationContextFilter filter = new PerApplicationContextFilter(explicit, false);
      AtomicReference<String> seen = new AtomicReference<>();

      PerApplicationContext.setPerApplicationData(SampleManager.class, "explicit");
      servletContext.setAttribute(PerApplicationContext.class.getName(), new Object());
      filter.init(new MockFilterConfig(servletContext));

      Thread.ofPlatform().name("isolated-filter-chain").daemon(false).inheritInheritableThreadLocals(false).start(() -> {
        try {
          filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationContext.getPerApplicationData(SampleManager.class, String.class)));
        } catch (Exception exception) {
          seen.set(exception.toString());
        }
      }).join();

      Assert.assertEquals(seen.get(), "explicit");

      return null;
    });
  }

  public void testSuppressionConstructorResolvesServletContextAttributeAndSwallowsConnectionClosed ()
    throws Exception {

    onIsolatedThread(() -> {

      PerApplicationContext published = new PerApplicationContext();
      MockServletContext servletContext = new MockServletContext();
      PerApplicationContextFilter filter = new PerApplicationContextFilter(true);
      AtomicReference<String> seen = new AtomicReference<>();

      PerApplicationContext.setPerApplicationData(SampleManager.class, "published");
      servletContext.setAttribute(PerApplicationContext.class.getName(), published);
      filter.init(new MockFilterConfig(servletContext));

      Thread.ofPlatform().name("isolated-filter-chain").daemon(false).inheritInheritableThreadLocals(false).start(() -> {
        try {
          filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            seen.set(PerApplicationContext.getPerApplicationData(SampleManager.class, String.class));
            throw new IOException("Connection is closed");
          });
        } catch (Exception exception) {
          seen.set(exception.toString());
        }
      }).join();

      Assert.assertEquals(seen.get(), "published");

      return null;
    });
  }

  public void testConnectionClosedIsSuppressedWhenEnabled ()
    throws Exception {

    onIsolatedThread(() -> {

      PerApplicationContextFilter filter = new PerApplicationContextFilter(new PerApplicationContext(), true);

      filter.init(new MockFilterConfig(new MockServletContext()));
      filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        throw new IOException("Connection is closed");
      });

      return null;
    });
  }

  public void testConnectionClosedPropagatesAsSameInstanceWhenNotSuppressed ()
    throws Exception {

    onIsolatedThread(() -> {

      PerApplicationContextFilter filter = new PerApplicationContextFilter(new PerApplicationContext());
      IOException thrown = new IOException("Connection is closed");

      filter.init(new MockFilterConfig(new MockServletContext()));
      try {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
          throw thrown;
        });
        Assert.fail("The chain's exception should propagate");
      } catch (IOException ioException) {
        Assert.assertSame(ioException, thrown);
      }

      return null;
    });
  }

  public void testOtherIOExceptionIsNotSuppressedWhenEnabled ()
    throws Exception {

    onIsolatedThread(() -> {

      PerApplicationContextFilter filter = new PerApplicationContextFilter(new PerApplicationContext(), true);
      IOException thrown = new IOException("Broken pipe");

      filter.init(new MockFilterConfig(new MockServletContext()));
      try {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
          throw thrown;
        });
        Assert.fail("Only the connection-closed message is suppressed");
      } catch (IOException ioException) {
        Assert.assertSame(ioException, thrown);
      }

      return null;
    });
  }

  public void testSetSuppressConnectionClosedExceptionReturnsThis ()
    throws Exception {

    onIsolatedThread(() -> {

      PerApplicationContextFilter filter = new PerApplicationContextFilter(new PerApplicationContext());

      Assert.assertSame(filter.setSuppressConnectionClosedException(true), filter);

      return null;
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
}
