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
package org.smallmind.persistence.sql.pool.context;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.PooledConnection;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ContextualPooledDataSourceTest {

  /**
   * Builds a Mockito-mocked {@link ComponentPool} reporting the supplied pool name.
   */
  private static ComponentPool mockComponentPool (String poolName) {

    ComponentPool componentPool = Mockito.mock(ComponentPool.class);

    Mockito.when(componentPool.getPoolName()).thenReturn(poolName);

    return componentPool;
  }

  public void testConstructorRoutesConnectionByCurrentContext ()
    throws ComponentPoolException, SQLException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');

    // Two pools whose names translate back to contexts "red" and "blue".
    ComponentPool redPool = mockComponentPool(translator.getPoolName("red"));
    ComponentPool bluePool = mockComponentPool(translator.getPoolName("blue"));

    PooledConnection redPooledConnection = Mockito.mock(PooledConnection.class);
    Connection redConnection = Mockito.mock(Connection.class);

    Mockito.when(redPool.getComponent()).thenReturn(redPooledConnection);
    Mockito.when(redPooledConnection.getConnection()).thenReturn(redConnection);

    ContextualPooledDataSource dataSource = new ContextualPooledDataSource(translator, redPool, bluePool);

    ContextFactory.pushContext(new PooledDataSourceContext("red"));
    try {
      Assert.assertSame(dataSource.getConnection(), redConnection);
    } finally {
      ContextFactory.popContext(PooledDataSourceContext.class);
    }
  }

  @Test(groups = "unit", expectedExceptions = SQLException.class)
  public void testGetConnectionWithNoMatchingContextThrowsWrappedSqlException ()
    throws ComponentPoolException, SQLException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    ComponentPool redPool = mockComponentPool(translator.getPoolName("red"));

    ContextualPooledDataSource dataSource = new ContextualPooledDataSource(translator, redPool);

    ContextFactory.pushContext(new PooledDataSourceContext("green"));
    try {
      dataSource.getConnection();
    } finally {
      ContextFactory.popContext(PooledDataSourceContext.class);
    }
  }

  public void testGetConnectionWithNoMatchingContextWrapsComponentPoolException ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    ComponentPool redPool = mockComponentPool(translator.getPoolName("red"));

    ContextualPooledDataSource dataSource = new ContextualPooledDataSource(translator, redPool);

    ContextFactory.pushContext(new PooledDataSourceContext("green"));
    try {
      dataSource.getConnection();
      Assert.fail("Expected a SQLException");
    } catch (SQLException sqlException) {
      // The contextual data source wraps the internal ComponentPoolException as the SQLException cause.
      Assert.assertTrue(sqlException.getCause() instanceof ComponentPoolException);
    } finally {
      ContextFactory.popContext(PooledDataSourceContext.class);
    }
  }

  @Test(groups = "unit", expectedExceptions = SQLException.class)
  public void testGetConnectionWithNullContextWhenNoNullKeyedPoolThrows ()
    throws ComponentPoolException, SQLException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    ComponentPool redPool = mockComponentPool(translator.getPoolName("red"));

    ContextualPooledDataSource dataSource = new ContextualPooledDataSource(translator, redPool);

    // No PooledDataSourceContext on the thread -> current contextual part resolves to null, which
    // matches no pool here.
    Assert.assertNull(ContextFactory.getContext(PooledDataSourceContext.class));
    dataSource.getConnection();
  }

  @Test(groups = "unit", expectedExceptions = ComponentPoolException.class)
  public void testConstructorRejectsPoolNameNotMatchingTranslatorBase ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    // A pool name that does not start with the translator base cannot be parsed.
    ComponentPool foreignPool = mockComponentPool("other:red");

    new ContextualPooledDataSource(translator, foreignPool);
  }

  public void testGetConnectionWithUsernamePasswordIsUnsupported ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    ComponentPool redPool = mockComponentPool(translator.getPoolName("red"));

    ContextualPooledDataSource dataSource = new ContextualPooledDataSource(translator, redPool);

    try {
      dataSource.getConnection("user", "password");
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
      // expected
    }
  }
}
