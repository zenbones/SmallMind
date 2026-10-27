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
package org.smallmind.web.grizzly;

import java.util.concurrent.atomic.AtomicReference;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PerApplicationScopeHttpHandlerTest {

  private static void assertUnbound () {

    try {
      PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      Assert.fail("The binding should not survive service()");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  public void testServiceRunsTheDelegateInsideTheScope ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    RecordingHandler delegate = new RecordingHandler(null);

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    new PerApplicationScopeHttpHandler(delegate, perApplicationScope).service(null, null);

    Assert.assertEquals(delegate.seen.get(), "scoped");
    assertUnbound();
  }

  public void testServiceRethrowsTheDelegatesExceptionUnchanged () {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    IllegalStateException failure = new IllegalStateException("delegate failed");

    try {
      new PerApplicationScopeHttpHandler(new RecordingHandler(failure), perApplicationScope).service(null, null);
      Assert.fail("The delegate's exception should propagate");
    } catch (Exception exception) {
      Assert.assertSame(exception, failure);
    }

    assertUnbound();
  }

  public void testStartAndDestroyAreForwarded () {

    RecordingHandler delegate = new RecordingHandler(null);
    PerApplicationScopeHttpHandler handler = new PerApplicationScopeHttpHandler(delegate, new PerApplicationScope());

    handler.start();
    handler.destroy();

    Assert.assertTrue(delegate.started);
    Assert.assertTrue(delegate.destroyed);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testDelegateIsRequired () {

    new PerApplicationScopeHttpHandler(null, new PerApplicationScope());
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testScopeIsRequired () {

    new PerApplicationScopeHttpHandler(new RecordingHandler(null), null);
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  private static class RecordingHandler extends HttpHandler {

    private final AtomicReference<String> seen = new AtomicReference<>();
    private final Exception failure;
    private boolean started;
    private boolean destroyed;

    private RecordingHandler (Exception failure) {

      this.failure = failure;
    }

    @Override
    public void start () {

      started = true;
    }

    @Override
    public void destroy () {

      destroyed = true;
    }

    @Override
    public void service (Request request, Response response)
      throws Exception {

      if (failure != null) {
        throw failure;
      }

      seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
    }
  }
}
