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

import java.sql.SQLException;
import javax.sql.XAConnection;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ContextualPooledXADataSource}, the XA counterpart of {@link ContextualPooledDataSource}.
 * Component pools are Mockito mocks reporting translator-derived pool names, so no real XA resource manager is
 * involved; the assertions verify context-based routing of {@code getXAConnection()}, the wrapping of a missing
 * context as a {@link SQLException}, the unsupported credentialed overload, and that {@code startup}/{@code shutdown}
 * fan out to every managed pool.
 */
@Test(groups = "unit")
public class ContextualPooledXADataSourceTest {

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static ComponentPool<XAConnection> mockComponentPool (String poolName) {

    ComponentPool componentPool = Mockito.mock(ComponentPool.class);

    Mockito.when(componentPool.getPoolName()).thenReturn(poolName);

    return componentPool;
  }

  public void testGetXAConnectionRoutesByCurrentContext ()
    throws ComponentPoolException, SQLException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');

    ComponentPool<XAConnection> redPool = mockComponentPool(translator.getPoolName("red"));
    ComponentPool<XAConnection> bluePool = mockComponentPool(translator.getPoolName("blue"));

    XAConnection redXAConnection = Mockito.mock(XAConnection.class);

    Mockito.when(redPool.getComponent()).thenReturn(redXAConnection);

    ContextualPooledXADataSource dataSource = new ContextualPooledXADataSource(translator, redPool, bluePool);

    ContextFactory.pushContext(new PooledDataSourceContext("red"));
    try {
      Assert.assertSame(dataSource.getXAConnection(), redXAConnection);
    } finally {
      ContextFactory.popContext(PooledDataSourceContext.class);
    }
  }

  public void testGetXAConnectionWithNoMatchingContextWrapsComponentPoolException ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    ComponentPool<XAConnection> redPool = mockComponentPool(translator.getPoolName("red"));

    ContextualPooledXADataSource dataSource = new ContextualPooledXADataSource(translator, redPool);

    ContextFactory.pushContext(new PooledDataSourceContext("green"));
    try {
      dataSource.getXAConnection();
      Assert.fail("Expected a SQLException");
    } catch (SQLException sqlException) {
      Assert.assertTrue(sqlException.getCause() instanceof ComponentPoolException, "the missing-pool failure should be wrapped as a SQLException cause");
    } finally {
      ContextFactory.popContext(PooledDataSourceContext.class);
    }
  }

  @Test(groups = "unit", expectedExceptions = UnsupportedOperationException.class)
  public void testGetXAConnectionWithUsernamePasswordIsUnsupported ()
    throws ComponentPoolException, SQLException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    ComponentPool<XAConnection> redPool = mockComponentPool(translator.getPoolName("red"));

    new ContextualPooledXADataSource(translator, redPool).getXAConnection("user", "password");
  }

  public void testStartupAndShutdownFanOutToEveryPool ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", ':');
    ComponentPool<XAConnection> redPool = mockComponentPool(translator.getPoolName("red"));
    ComponentPool<XAConnection> bluePool = mockComponentPool(translator.getPoolName("blue"));

    ContextualPooledXADataSource dataSource = new ContextualPooledXADataSource(translator, redPool, bluePool);

    dataSource.startup();
    dataSource.shutdown();

    Mockito.verify(redPool).startup();
    Mockito.verify(bluePool).startup();
    Mockito.verify(redPool).shutdown();
    Mockito.verify(bluePool).shutdown();
  }
}
