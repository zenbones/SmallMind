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
package org.smallmind.persistence.cache.praxis;

import java.util.Iterator;
import java.util.List;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link ByKeySingularVector}. Its {@code head}/{@code add} paths resolve the referenced durable
 * lazily through an {@link ORMDao}, so those cases register a Mockito mock via {@link OrmDaoManager} on a
 * thread that has a {@link PerApplicationContext} attached. The DAO-free branches ({@code isSingular},
 * {@code copy}, {@code remove}) are tested directly.
 */
@Test(groups = "unit")
public class ByKeySingularVectorTest {

  @BeforeMethod
  public void attachApplicationContext () {

    // OrmDaoManager.get(...) reads PerApplicationContext, which throws unless a context map is bound to this thread.
    new PerApplicationContext();
  }

  @SuppressWarnings("unchecked")
  private static ORMDao<Long, Sprocket, ?, ?> registerMockDao () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    OrmDaoManager.register(Sprocket.class, ormDao);

    return ormDao;
  }

  public void testIsSingularIsAlwaysTrue () {

    ByKeySingularVector<Long, Sprocket> vector = new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0);

    Assert.assertTrue(vector.isSingular());
  }

  public void testHeadResolvesDurableThroughDao () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = registerMockDao();
    Sprocket resolved = new Sprocket(1L);

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(resolved);

    ByKeySingularVector<Long, Sprocket> vector = new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0);

    Assert.assertSame(vector.head(), resolved);
  }

  public void testAddReplacesKeyWhenDurableDiffersById () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = registerMockDao();

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(new Sprocket(1L));
    Mockito.when(ormDao.getIdFromString("2")).thenReturn(2L);
    Mockito.when(ormDao.get(2L)).thenReturn(new Sprocket(2L));

    ByKeySingularVector<Long, Sprocket> vector = new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0);

    Assert.assertTrue(vector.add(new Sprocket(2L)));
    // After the swap the vector now references id 2 and resolves to that durable.
    Assert.assertEquals(vector.head().getId(), Long.valueOf(2L));
  }

  public void testAddReturnsFalseWhenDurableHasSameId () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = registerMockDao();

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(new Sprocket(1L));

    ByKeySingularVector<Long, Sprocket> vector = new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0);

    Assert.assertFalse(vector.add(new Sprocket(1L)));
  }

  public void testCopyPreservesKeyAndTimeToLive () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = registerMockDao();

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(new Sprocket(1L));

    ByKeySingularVector<Long, Sprocket> vector = new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 42);
    DurableVector<Long, Sprocket> copy = vector.copy();

    Assert.assertTrue(copy instanceof ByKeySingularVector);
    Assert.assertEquals(copy.getTimeToLiveSeconds(), 42);
    Assert.assertTrue(copy.isSingular());
    Assert.assertEquals(copy.head().getId(), Long.valueOf(1L));
  }

  public void testLazyListHoldsTheSingleResolvedDurable () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = registerMockDao();
    Sprocket resolved = new Sprocket(1L);

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(resolved);

    ByKeySingularVector<Long, Sprocket> vector = new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0);
    List<Sprocket> list = vector.asBestEffortLazyList();

    Assert.assertEquals(list.size(), 1, "a singular vector should expose exactly one element");
    Assert.assertSame(list.get(0), resolved, "the lazy list should hold the durable resolved through the DAO");
  }

  public void testIteratorYieldsTheSingleResolvedDurableOnce () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = registerMockDao();
    Sprocket resolved = new Sprocket(1L);

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(resolved);

    ByKeySingularVector<Long, Sprocket> vector = new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0);
    Iterator<Sprocket> iterator = vector.iterator();

    Assert.assertTrue(iterator.hasNext());
    Assert.assertSame(iterator.next(), resolved, "the iterator should yield the durable resolved through the DAO");
    Assert.assertFalse(iterator.hasNext(), "a singular iterator should yield exactly one element");
  }

  @Test(groups = "unit", expectedExceptions = CacheOperationException.class)
  public void testHeadFailsWhenNoDaoIsRegistered () {

    // No DAO is registered for Sprocket, so getORMDao() cannot locate one and reports a cache operation failure.
    new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0).head();
  }

  @Test(groups = "unit", expectedExceptions = CacheOperationException.class)
  public void testHeadFailsWhenDurableCannotBeResolved () {

    ORMDao<Long, Sprocket, ?, ?> ormDao = registerMockDao();

    // The DAO resolves the id but returns no durable, so getDurable() reports the missing instance.
    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(null);

    new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0).head();
  }

  @Test(groups = "unit", expectedExceptions = UnsupportedOperationException.class)
  public void testRemoveIsUnsupported () {

    new ByKeySingularVector<>(new DurableKey<>(Sprocket.class, 1L), 0).remove(new Sprocket(1L));
  }

  public static class Sprocket extends AbstractDurable<Long, Sprocket> {

    private Long id;

    public Sprocket () {

    }

    public Sprocket (Long id) {

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
}
