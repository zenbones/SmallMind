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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpHandler;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PerApplicationScopeHttpFilterTest {

  public void testDoFilterRunsTheRestOfTheChainInsideTheScope ()
    throws IOException {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    AtomicReference<String> seen = new AtomicReference<>();
    HttpHandler terminal = (exchange) -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    new PerApplicationScopeHttpFilter(perApplicationScope).doFilter(null, new Filter.Chain(List.of(), terminal));

    Assert.assertEquals(seen.get(), "scoped");
    assertUnbound();
  }

  public void testDoFilterRethrowsAnIOExceptionUnchanged () {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    IOException failure = new IOException("downstream failed");
    HttpHandler terminal = (exchange) -> {
      throw failure;
    };

    try {
      new PerApplicationScopeHttpFilter(perApplicationScope).doFilter(null, new Filter.Chain(List.of(), terminal));
      Assert.fail("The downstream exception should propagate");
    } catch (IOException ioException) {
      Assert.assertSame(ioException, failure);
    }

    assertUnbound();
  }

  public void testDescriptionNamesTheFilter () {

    Assert.assertEquals(new PerApplicationScopeHttpFilter(new PerApplicationScope()).description(), "per-application-scope");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testScopeIsRequired () {

    new PerApplicationScopeHttpFilter(null);
  }

  private static void assertUnbound () {

    try {
      PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      Assert.fail("The binding should not survive doFilter()");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  public static class SampleManager implements PerApplicationDataManager {

  }
}
