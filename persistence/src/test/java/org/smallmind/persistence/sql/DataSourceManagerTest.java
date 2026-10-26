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
package org.smallmind.persistence.sql;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.persistence.orm.ORMInitializationException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the bridge semantics of {@link DataSourceManager}.
 */
@Test(groups = "unit")
public class DataSourceManagerTest {

  @BeforeMethod
  public void attachApplicationContext () {

    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(DataSourceContextManager.class, new ConcurrentHashMap<>());
  }

  public void testRegisterWithBothBoundWritesBothRegistries () {

    PerApplicationScope scope = new PerApplicationScope();
    DataSource dataSource = Mockito.mock(DataSource.class);

    scope.run(() -> DataSourceManager.register("primary", dataSource));

    Assert.assertSame(scope.call(() -> DataSourceScopeManager.getDataSource("primary")), dataSource);
    Assert.assertSame(DataSourceContextManager.getDataSource("primary"), dataSource);
  }

  public void testLookupFallsBackToTheContextWhenTheScopeHoldsNothing () {

    PerApplicationScope scope = new PerApplicationScope();
    DataSource dataSource = Mockito.mock(DataSource.class);

    DataSourceContextManager.register("primary", dataSource);

    Assert.assertSame(scope.call(() -> DataSourceManager.getDataSource("primary")), dataSource);
  }

  public void testLookupPrefersTheScopeWhenBothHoldAValue () {

    PerApplicationScope scope = new PerApplicationScope();
    DataSource scoped = Mockito.mock(DataSource.class);
    DataSource context = Mockito.mock(DataSource.class);

    scope.run(() -> DataSourceScopeManager.register("primary", scoped));
    DataSourceContextManager.register("primary", context);

    Assert.assertSame(scope.call(() -> DataSourceManager.getDataSource("primary")), scoped);
    Assert.assertSame(DataSourceManager.getDataSource("primary"), context);
  }

  @Test(expectedExceptions = ORMInitializationException.class)
  public void testMissingInEveryBoundRegistryThrows () {

    new PerApplicationScope().run(() -> DataSourceManager.getDataSource("missing"));
  }

  public void testFindReturnsNullInsteadOfThrowing () {

    PerApplicationScope scope = new PerApplicationScope();

    Assert.assertNull(scope.call(() -> DataSourceManager.findDataSource("missing")));
    Assert.assertNull(DataSourceManager.findDataSource("missing"));
  }

  public void testScopeOnlyThreadResolvesWithoutAContext ()
    throws Exception {

    PerApplicationScope scope = new PerApplicationScope();
    DataSource dataSource = Mockito.mock(DataSource.class);

    Assert.assertSame(onIsolatedThread(() -> scope.call(() -> {
      DataSourceManager.register("primary", dataSource);

      return DataSourceManager.getDataSource("primary");
    })), dataSource);
    Assert.assertNull(onIsolatedThread(() -> scope.call(() -> DataSourceManager.findDataSource("missing"))), "an unregistered key on a scope-only thread is null, not an exception");
  }

  @Test(expectedExceptions = MissingPerApplicationContextException.class)
  public void testUnboundThreadFailsThroughTheContextFlavor ()
    throws Exception {

    onIsolatedThread(() -> DataSourceManager.findDataSource("primary"));
  }

  // The test thread carries an inherited PerApplicationContext shared with every other test on it, so the scope-only
  // and unbound cases can only be observed on a thread that does not inherit it.
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
}
