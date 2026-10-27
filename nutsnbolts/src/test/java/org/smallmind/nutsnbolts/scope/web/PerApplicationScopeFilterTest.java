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
package org.smallmind.nutsnbolts.scope.web;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.ServletException;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PerApplicationScopeFilterTest {

  private static PerApplicationScope scopeHolding (String value) {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, value));

    return scope;
  }

  private static PerApplicationScopeFilter initialized (PerApplicationScopeFilter filter) {

    filter.init(new MockFilterConfig(new MockServletContext()));

    return filter;
  }

  private static void assertUnbound () {

    try {
      PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      Assert.fail("The binding should not survive doFilter");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  public void testExplicitScopeIsBoundDuringChain ()
    throws IOException, ServletException {

    AtomicReference<String> seen = new AtomicReference<>();

    initialized(new PerApplicationScopeFilter(scopeHolding("value"))).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));

    Assert.assertEquals(seen.get(), "value");
  }

  public void testBindingEndsWhenDoFilterReturns ()
    throws IOException, ServletException {

    initialized(new PerApplicationScopeFilter(scopeHolding("value"))).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
    });

    assertUnbound();
  }

  public void testBindingEndsWhenChainThrows ()
    throws IOException {

    try {
      initialized(new PerApplicationScopeFilter(scopeHolding("value"))).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        throw new ServletException("boom");
      });
      Assert.fail("The chain's exception should propagate");
    } catch (ServletException servletException) {
      // expected
    }

    assertUnbound();
  }

  public void testServletExceptionPropagatesAsSameInstance ()
    throws IOException {

    ServletException thrown = new ServletException("boom");

    try {
      initialized(new PerApplicationScopeFilter(scopeHolding("value"))).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        throw thrown;
      });
      Assert.fail("The chain's exception should propagate");
    } catch (ServletException servletException) {
      Assert.assertSame(servletException, thrown);
    }
  }

  public void testIOExceptionPropagatesWhenNotSuppressed ()
    throws ServletException {

    IOException thrown = new IOException("Connection is closed");

    try {
      initialized(new PerApplicationScopeFilter(scopeHolding("value"))).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        throw thrown;
      });
      Assert.fail("The chain's exception should propagate");
    } catch (IOException ioException) {
      Assert.assertSame(ioException, thrown);
    }
  }

  public void testConnectionClosedIsSuppressedWhenEnabled ()
    throws IOException, ServletException {

    initialized(new PerApplicationScopeFilter(scopeHolding("value"), true)).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
      throw new IOException("Connection is closed");
    });

    assertUnbound();
  }

  public void testOtherIOExceptionIsNotSuppressedWhenEnabled ()
    throws ServletException {

    IOException thrown = new IOException("Broken pipe");

    try {
      initialized(new PerApplicationScopeFilter(scopeHolding("value"), true)).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        throw thrown;
      });
      Assert.fail("Only the connection-closed message is suppressed");
    } catch (IOException ioException) {
      Assert.assertSame(ioException, thrown);
    }
  }

  public void testRuntimeExceptionPropagatesAsSameInstance ()
    throws IOException, ServletException {

    IllegalStateException thrown = new IllegalStateException("boom");

    try {
      initialized(new PerApplicationScopeFilter(scopeHolding("value"))).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        throw thrown;
      });
      Assert.fail("The chain's exception should propagate");
    } catch (IllegalStateException illegalStateException) {
      Assert.assertSame(illegalStateException, thrown);
    }
  }

  public void testDataStoredInsideOneRequestIsVisibleToTheNext ()
    throws IOException, ServletException {

    PerApplicationScopeFilter filter = initialized(new PerApplicationScopeFilter(new PerApplicationScope()));
    AtomicReference<String> seen = new AtomicReference<>();

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> PerApplicationScope.setPerApplicationData(SampleManager.class, "first-request"));
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));

    Assert.assertEquals(seen.get(), "first-request");
  }

  public void testNoArgConstructorResolvesServletContextAttribute ()
    throws IOException, ServletException {

    MockServletContext servletContext = new MockServletContext();
    PerApplicationScopeFilter filter = new PerApplicationScopeFilter();
    AtomicReference<String> seen = new AtomicReference<>();

    servletContext.setAttribute(PerApplicationScope.class.getName(), scopeHolding("published"));
    filter.init(new MockFilterConfig(servletContext));
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));

    Assert.assertEquals(seen.get(), "published");
  }

  public void testNoArgConstructorFailsFastWithoutAttribute () {

    MockServletContext servletContext = new MockServletContext();

    try {
      new PerApplicationScopeFilter().init(new MockFilterConfig(servletContext));
      Assert.fail("A filter without a scope should fail in init");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }

    Assert.assertNull(servletContext.getAttribute(PerApplicationScope.class.getName()), "The filter must not create a scope of its own");
  }

  public void testNoArgConstructorAdoptsTheBoundScopeWithoutAttribute ()
    throws IOException, ServletException {

    MockServletContext servletContext = new MockServletContext();
    PerApplicationScope enclosing = scopeHolding("bound");
    PerApplicationScopeFilter filter = new PerApplicationScopeFilter();
    AtomicReference<String> seen = new AtomicReference<>();

    enclosing.run(() -> filter.init(new MockFilterConfig(servletContext)));
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));

    Assert.assertEquals(seen.get(), "bound");
    Assert.assertNull(servletContext.getAttribute(PerApplicationScope.class.getName()), "The filter must not publish the scope it adopted");
  }

  public void testNoArgConstructorRejectsAPublishedScopeThatIsNotTheBoundScope () {

    MockServletContext servletContext = new MockServletContext();

    servletContext.setAttribute(PerApplicationScope.class.getName(), scopeHolding("published"));
    scopeHolding("bound").run(() -> {
      try {
        new PerApplicationScopeFilter().init(new MockFilterConfig(servletContext));
        Assert.fail("A published scope that is not the one bound on the initializing thread is contradictory wiring");
      } catch (ScopeException scopeException) {
        // expected
      }
    });
  }

  public void testExplicitScopeThatIsNotTheBoundScopeFailsFast () {

    PerApplicationScopeFilter filter = new PerApplicationScopeFilter(scopeHolding("explicit"));

    scopeHolding("bound").run(() -> {
      try {
        filter.init(new MockFilterConfig(new MockServletContext()));
        Assert.fail("An explicit scope that is not the one bound on the initializing thread is contradictory wiring");
      } catch (ScopeException scopeException) {
        // expected
      }
    });
  }

  public void testExplicitScopeMatchingTheBoundScopeIsUsed ()
    throws IOException, ServletException {

    PerApplicationScope enclosing = scopeHolding("explicit");
    PerApplicationScopeFilter filter = new PerApplicationScopeFilter(enclosing);
    AtomicReference<String> seen = new AtomicReference<>();

    enclosing.run(() -> filter.init(new MockFilterConfig(new MockServletContext())));
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));

    Assert.assertEquals(seen.get(), "explicit");
  }

  public void testExplicitScopeIgnoresServletContextAttribute ()
    throws IOException, ServletException {

    MockServletContext servletContext = new MockServletContext();
    PerApplicationScopeFilter filter = new PerApplicationScopeFilter(scopeHolding("explicit"));
    AtomicReference<String> seen = new AtomicReference<>();

    servletContext.setAttribute(PerApplicationScope.class.getName(), scopeHolding("published"));
    filter.init(new MockFilterConfig(servletContext));
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));

    Assert.assertEquals(seen.get(), "explicit");
  }

  public void testSetSuppressConnectionClosedExceptionReturnsThis () {

    PerApplicationScopeFilter filter = new PerApplicationScopeFilter(new PerApplicationScope());

    Assert.assertSame(filter.setSuppressConnectionClosedException(true), filter);
  }

  public static class SampleManager implements PerApplicationDataManager {

  }
}
