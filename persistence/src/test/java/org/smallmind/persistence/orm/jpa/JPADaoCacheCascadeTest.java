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
package org.smallmind.persistence.orm.jpa;

import jakarta.persistence.EntityManager;
import org.mockito.Mockito;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for the cache-cascade ({@code vectoredDao != null}) branches of {@link JPADao}'s
 * {@code get}/{@code persist}/{@code delete} methods. The {@link JPAProxySession} and its native
 * {@link EntityManager} are Mockito mocks, so no database is involved; the assertions verify the
 * read-through (cache hit short-circuits the source; cache miss reads the source then SOFT-populates
 * the cache), write-through ({@code UpdateMode.HARD}), and eviction interactions with the
 * {@link VectoredDao} delegate.
 */
@Test(groups = "unit")
public class JPADaoCacheCascadeTest {

  private EntityManager entityManager;
  private JPAProxySession proxySession;
  private VectoredDao<Long, Widget> vectoredDao;
  private WidgetDao dao;

  @SuppressWarnings("unchecked")
  @BeforeMethod
  public void setUp () {

    entityManager = Mockito.mock(EntityManager.class);
    proxySession = Mockito.mock(JPAProxySession.class);
    vectoredDao = Mockito.mock(VectoredDao.class);

    Mockito.when(proxySession.getNativeSession()).thenReturn(entityManager);
    Mockito.when(proxySession.isCacheEnabled()).thenReturn(true);

    dao = new WidgetDao(proxySession, vectoredDao);
  }

  public void testGetReturnsCacheHitWithoutTouchingTheSource () {

    Widget cached = new Widget(1L);

    Mockito.when(vectoredDao.get(Widget.class, 1L)).thenReturn(cached);

    Assert.assertSame(dao.get(1L), cached, "a cache hit should be returned directly");
    Mockito.verify(entityManager, Mockito.never()).find(Mockito.any(), Mockito.any());
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testGetOnCacheMissReadsSourceThenSoftPopulatesCache () {

    Widget fromSource = new Widget(2L);

    Mockito.when(vectoredDao.get(Widget.class, 2L)).thenReturn(null);
    Mockito.when(entityManager.find(Widget.class, 2L)).thenReturn(fromSource);
    Mockito.when(vectoredDao.persist(Widget.class, fromSource, UpdateMode.SOFT)).thenReturn(fromSource);

    Assert.assertSame(dao.get(2L), fromSource, "a cache miss should fall through to the source result");
    Mockito.verify(vectoredDao).persist(Widget.class, fromSource, UpdateMode.SOFT);
  }

  public void testGetReturnsNullWhenAbsentFromBothCacheAndSource () {

    Mockito.when(vectoredDao.get(Widget.class, 3L)).thenReturn(null);
    Mockito.when(entityManager.find(Widget.class, 3L)).thenReturn(null);

    Assert.assertNull(dao.get(3L), "a value absent from cache and source should resolve to null");
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testPersistMergesAndWritesThroughHard () {

    Widget durable = new Widget(4L);

    Mockito.when(entityManager.contains(durable)).thenReturn(false);
    Mockito.when(entityManager.merge(durable)).thenReturn(durable);
    Mockito.when(vectoredDao.persist(Widget.class, durable, UpdateMode.HARD)).thenReturn(durable);

    Assert.assertSame(dao.persist(durable), durable);
    Mockito.verify(entityManager).merge(durable);
    Mockito.verify(proxySession).flush();
    Mockito.verify(vectoredDao).persist(Widget.class, durable, UpdateMode.HARD);
  }

  public void testPersistOfManagedDurableSkipsMergeButStillWritesThrough () {

    Widget durable = new Widget(5L);

    Mockito.when(entityManager.contains(durable)).thenReturn(true);
    Mockito.when(vectoredDao.persist(Widget.class, durable, UpdateMode.HARD)).thenReturn(durable);

    dao.persist(durable);

    Mockito.verify(entityManager, Mockito.never()).merge(Mockito.any());
    Mockito.verify(proxySession, Mockito.never()).flush();
    Mockito.verify(vectoredDao).persist(Widget.class, durable, UpdateMode.HARD);
  }

  public void testDeleteOfManagedDurableRemovesAndEvicts () {

    Widget durable = new Widget(6L);

    Mockito.when(entityManager.contains(durable)).thenReturn(true);

    dao.delete(durable);

    Mockito.verify(entityManager).remove(durable);
    Mockito.verify(vectoredDao).delete(Widget.class, durable);
  }

  public void testDeleteOfDetachedDurableFindsThenRemovesAndEvicts () {

    Widget durable = new Widget(7L);

    Mockito.when(entityManager.contains(durable)).thenReturn(false);
    Mockito.when(entityManager.find(Widget.class, 7L)).thenReturn(durable);

    dao.delete(durable);

    Mockito.verify(entityManager).remove(durable);
    Mockito.verify(vectoredDao).delete(Widget.class, durable);
  }

  public static class Widget extends AbstractDurable<Long, Widget> {

    private static final long serialVersionUID = 1L;

    private Long id;

    public Widget () {

    }

    public Widget (Long id) {

      this.id = id;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  private static class WidgetDao extends JPADao<Long, Widget> {

    private WidgetDao (JPAProxySession proxySession, VectoredDao<Long, Widget> vectoredDao) {

      super(proxySession, vectoredDao);
    }
  }
}
