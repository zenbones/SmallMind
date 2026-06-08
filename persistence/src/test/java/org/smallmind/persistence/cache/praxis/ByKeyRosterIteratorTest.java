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

import org.mockito.Mockito;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.orm.ORMDao;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link ByKeyRosterIterator} directly. The iterator wraps a key-roster list iterator and hydrates each
 * {@link DurableKey} into a {@link Cog} via a supplied {@link ORMDao}. Because the DAO is handed to the constructor
 * explicitly (rather than resolved through {@code OrmDaoManager}), these tests need no {@code PerApplicationContext};
 * they simply register a Mockito {@link ORMDao} stub that round-trips ids and supplies the managed class used to
 * build replacement keys on {@code set}/{@code add}. Coverage spans forward and backward traversal, the raw
 * {@link ByKeyRosterIterator#nextKey()} accessor, index queries, structural mutation, and the not-found hydration
 * failure.
 */
@Test(groups = "unit")
public class ByKeyRosterIteratorTest {

  @SuppressWarnings("unchecked")
  private static ORMDao<Long, Cog, ?, ?> hydratingDao () {

    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    Mockito.when(ormDao.getManagedClass()).thenReturn(Cog.class);
    for (long id = 1L; id <= 9L; id++) {
      Mockito.when(ormDao.getIdFromString(Long.toString(id))).thenReturn(id);
      Mockito.when(ormDao.get(id)).thenReturn(new Cog(id));
    }

    return ormDao;
  }

  private static IntrinsicRoster<DurableKey<Long, Cog>> keyRosterFor (long... ids) {

    IntrinsicRoster<DurableKey<Long, Cog>> keyRoster = new IntrinsicRoster<DurableKey<Long, Cog>>();

    for (long id : ids) {
      keyRoster.add(new DurableKey<>(Cog.class, id));
    }

    return keyRoster;
  }

  private static ByKeyRosterIterator<Long, Cog> iteratorFor (ORMDao<Long, Cog, ?, ?> ormDao, long... ids) {

    return new ByKeyRosterIterator<>(ormDao, keyRosterFor(ids).listIterator());
  }

  public void testNextHydratesEachDurableInOrder () {

    ByKeyRosterIterator<Long, Cog> iterator = iteratorFor(hydratingDao(), 1L, 2L, 3L);

    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(iterator.next().getId(), Long.valueOf(1L));
    Assert.assertEquals(iterator.next().getId(), Long.valueOf(2L));
    Assert.assertEquals(iterator.next().getId(), Long.valueOf(3L));
    Assert.assertFalse(iterator.hasNext());
  }

  public void testNextKeyReturnsRawKeyWithoutHydration () {

    ByKeyRosterIterator<Long, Cog> iterator = iteratorFor(hydratingDao(), 1L, 2L);

    Assert.assertEquals(iterator.nextKey(), new DurableKey<>(Cog.class, 1L));
    Assert.assertEquals(iterator.nextKey(), new DurableKey<>(Cog.class, 2L));
  }

  public void testPreviousHydratesInReverseAfterForwardTraversal () {

    ByKeyRosterIterator<Long, Cog> iterator = iteratorFor(hydratingDao(), 1L, 2L, 3L);

    iterator.next();
    iterator.next();

    Assert.assertTrue(iterator.hasPrevious());
    Assert.assertEquals(iterator.previous().getId(), Long.valueOf(2L));
    Assert.assertEquals(iterator.previous().getId(), Long.valueOf(1L));
    Assert.assertFalse(iterator.hasPrevious());
  }

  public void testNextIndexAndPreviousIndexTrackPosition () {

    ByKeyRosterIterator<Long, Cog> iterator = iteratorFor(hydratingDao(), 1L, 2L, 3L);

    Assert.assertEquals(iterator.nextIndex(), 0);
    Assert.assertEquals(iterator.previousIndex(), -1);

    iterator.next();

    Assert.assertEquals(iterator.nextIndex(), 1);
    Assert.assertEquals(iterator.previousIndex(), 0);
  }

  public void testSetReplacesLastReturnedElement () {

    IntrinsicRoster<DurableKey<Long, Cog>> keyRoster = keyRosterFor(1L, 2L, 3L);
    ByKeyRosterIterator<Long, Cog> iterator = new ByKeyRosterIterator<>(hydratingDao(), keyRoster.listIterator());

    iterator.next();
    iterator.set(new Cog(9L));

    Assert.assertTrue(keyRoster.contains(new DurableKey<>(Cog.class, 9L)));
    Assert.assertFalse(keyRoster.contains(new DurableKey<>(Cog.class, 1L)));
    Assert.assertEquals(keyRoster.size(), 3);
  }

  public void testAddInsertsKeyBeforeLastReturnedElement () {

    IntrinsicRoster<DurableKey<Long, Cog>> keyRoster = keyRosterFor(1L, 3L);
    ByKeyRosterIterator<Long, Cog> iterator = new ByKeyRosterIterator<>(hydratingDao(), keyRoster.listIterator());

    iterator.next();
    iterator.add(new Cog(2L));

    // The underlying IntrinsicRosterIterator inserts immediately before the last-returned element (see IntrinsicRosterIteratorTest).
    Assert.assertEquals(keyRoster.size(), 3);
    Assert.assertEquals(keyRoster.get(0), new DurableKey<>(Cog.class, 2L));
    Assert.assertEquals(keyRoster.get(1), new DurableKey<>(Cog.class, 1L));
    Assert.assertEquals(keyRoster.get(2), new DurableKey<>(Cog.class, 3L));
  }

  public void testRemoveDropsLastReturnedElement () {

    IntrinsicRoster<DurableKey<Long, Cog>> keyRoster = keyRosterFor(1L, 2L, 3L);
    ByKeyRosterIterator<Long, Cog> iterator = new ByKeyRosterIterator<>(hydratingDao(), keyRoster.listIterator());

    iterator.next();
    iterator.next();
    iterator.remove();

    Assert.assertEquals(keyRoster.size(), 2);
    Assert.assertFalse(keyRoster.contains(new DurableKey<>(Cog.class, 2L)));
  }

  @SuppressWarnings("unchecked")
  public void testNextThrowsWhenDurableCannotBeHydrated () {

    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(null);

    ByKeyRosterIterator<Long, Cog> iterator = iteratorFor(ormDao, 1L);

    Assert.assertThrows(CacheOperationException.class, iterator::next);
  }

  public static class Cog extends AbstractDurable<Long, Cog> {

    private Long id;

    public Cog () {

    }

    public Cog (Long id) {

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
