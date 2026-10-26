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
import org.smallmind.persistence.orm.ORMInitializationException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link DataSourceContextManager}: registration, the throwing and non-throwing lookups, and the failure
 * on a thread with no context attached.
 */
@Test(groups = "unit")
public class DataSourceContextManagerTest {

  @BeforeMethod
  public void attachApplicationContext () {

    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(DataSourceContextManager.class, new ConcurrentHashMap<>());
  }

  public void testRegisterAndGetDataSource () {

    DataSource dataSource = Mockito.mock(DataSource.class);

    DataSourceContextManager.register("primary", dataSource);

    Assert.assertSame(DataSourceContextManager.getDataSource("primary"), dataSource);
    Assert.assertSame(DataSourceContextManager.findDataSource("primary"), dataSource);
  }

  public void testFindReturnsNullForAnUnregisteredKey () {

    Assert.assertNull(DataSourceContextManager.findDataSource("missing"));
  }

  @Test(expectedExceptions = ORMInitializationException.class)
  public void testUnknownKeyThrows () {

    DataSourceContextManager.getDataSource("missing");
  }

  @Test(expectedExceptions = MissingPerApplicationContextException.class)
  public void testGetDataSourceWithoutContextThrows ()
    throws Exception {

    onIsolatedThread(() -> DataSourceContextManager.findDataSource("primary"));
  }

  private static <T> T onIsolatedThread (Callable<T> body)
    throws Exception {

    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().inheritInheritableThreadLocals(false).unstarted(() -> {
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
