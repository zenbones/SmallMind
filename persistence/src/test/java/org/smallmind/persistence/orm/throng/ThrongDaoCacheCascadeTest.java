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
package org.smallmind.persistence.orm.throng;

import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.InsertOneOptions;
import org.mockito.Mockito;
import org.smallmind.mongodb.throng.ThrongClient;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Query;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for the cache-cascade ({@code vectoredDao != null}) branches of {@link ThrongDao}'s
 * {@code get}/{@code persist}/{@code delete} methods. The {@link ThrongProxySession} and its native
 * {@link ThrongClient} are Mockito mocks, so no MongoDB is involved; the assertions verify read-through
 * (cache hit short-circuits the source; cache miss reads then SOFT-populates), write-through
 * ({@code UpdateMode.HARD}), and eviction interactions with the {@link VectoredDao} delegate.
 */
@Test(groups = "unit")
public class ThrongDaoCacheCascadeTest {

  private ThrongClient throngClient;
  private ThrongProxySession proxySession;
  private VectoredDao<Long, Widget> vectoredDao;
  private WidgetDao dao;

  @SuppressWarnings("unchecked")
  @BeforeMethod
  public void setUp () {

    throngClient = Mockito.mock(ThrongClient.class);
    proxySession = Mockito.mock(ThrongProxySession.class);
    vectoredDao = Mockito.mock(VectoredDao.class);

    Mockito.when(proxySession.getNativeSession()).thenReturn(throngClient);
    Mockito.when(proxySession.isCacheEnabled()).thenReturn(true);

    dao = new WidgetDao(proxySession, vectoredDao);
  }

  public void testGetReturnsCacheHitWithoutTouchingTheSource () {

    Widget cached = new Widget(1L);

    Mockito.when(vectoredDao.get(Widget.class, 1L)).thenReturn(cached);

    Assert.assertSame(dao.get(1L), cached, "a cache hit should be returned directly");
    Mockito.verify(throngClient, Mockito.never()).findOne(Mockito.any(), Mockito.any(Query.class));
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testGetOnCacheMissReadsSourceThenSoftPopulatesCache () {

    Widget fromSource = new Widget(2L);

    Mockito.when(vectoredDao.get(Widget.class, 2L)).thenReturn(null);
    Mockito.when(throngClient.findOne(Mockito.eq(Widget.class), Mockito.any(Query.class))).thenReturn(fromSource);
    Mockito.when(vectoredDao.persist(Widget.class, fromSource, UpdateMode.SOFT)).thenReturn(fromSource);

    Assert.assertSame(dao.get(2L), fromSource, "a cache miss should fall through to the source result");
    Mockito.verify(vectoredDao).persist(Widget.class, fromSource, UpdateMode.SOFT);
  }

  public void testGetReturnsNullWhenAbsentFromBothCacheAndSource () {

    Mockito.when(vectoredDao.get(Widget.class, 3L)).thenReturn(null);
    Mockito.when(throngClient.findOne(Mockito.eq(Widget.class), Mockito.any(Query.class))).thenReturn(null);

    Assert.assertNull(dao.get(3L), "a value absent from cache and source should resolve to null");
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testPersistInsertsAndWritesThroughHard () {

    Widget durable = new Widget(4L);

    Mockito.when(vectoredDao.persist(Widget.class, durable, UpdateMode.HARD)).thenReturn(durable);

    Assert.assertSame(dao.persist(durable), durable);
    Mockito.verify(throngClient).insert(Mockito.eq(durable), Mockito.any(InsertOneOptions.class));
    Mockito.verify(vectoredDao).persist(Widget.class, durable, UpdateMode.HARD);
  }

  public void testDeleteRemovesFromSourceAndEvicts () {

    Widget durable = new Widget(5L);

    dao.delete(durable);

    Mockito.verify(throngClient).delete(Mockito.eq(Widget.class), Mockito.any(Filter.class), Mockito.any(DeleteOptions.class));
    Mockito.verify(vectoredDao).delete(Widget.class, durable);
  }

  public static class Widget extends ThrongDurable<Long, Widget> {

    public Widget () {

    }

    public Widget (Long id) {

      setId(id);
    }
  }

  private static class WidgetDao extends ThrongDao<Long, Widget> {

    private WidgetDao (ThrongProxySession proxySession, VectoredDao<Long, Widget> vectoredDao) {

      super(proxySession, vectoredDao);
    }
  }
}
