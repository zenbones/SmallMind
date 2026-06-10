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
package org.smallmind.quorum.pool;

import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies that {@link PoolManager} publishes and resolves a single {@link Pool} per application
 * context. Each test installs a fresh {@link PerApplicationContext} so the registry never leaks
 * across methods.
 */
@Test(groups = "unit")
public class PoolManagerTest {

  @BeforeMethod
  public void establishPerApplicationContext () {

    new PerApplicationContext();
  }

  public void testGetPoolReturnsNullWhenNothingRegistered () {

    Assert.assertNull(PoolManager.getPool(), "an application context with no registered pool should resolve to null");
  }

  public void testRegisterMakesThePoolRetrievable () {

    Pool pool = new StubPool();

    pool.register();

    Assert.assertSame(PoolManager.getPool(), pool, "a registered pool should be retrievable from the manager");
  }

  public void testRegisterReplacesThePreviouslyRegisteredPool () {

    Pool first = new StubPool();
    Pool second = new StubPool();

    first.register();
    second.register();

    Assert.assertSame(PoolManager.getPool(), second, "registering a second pool should replace the first");
  }

  private static class StubPool extends Pool {

  }
}
