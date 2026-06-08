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
package org.smallmind.persistence.cache.praxis.extrinsic;

import java.util.ArrayList;
import java.util.List;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ByKeyExtrinsicVector}, which stores elements as {@link org.smallmind.persistence.cache.DurableKey}
 * values inside an {@link ExtrinsicRoster} and hydrates them through the registered {@link ORMDao} when read. A Mockito DAO
 * mock is registered with {@link OrmDaoManager} (which requires a {@link PerApplicationContext} bound to the thread) so the
 * lazy list, prefetched list, and iterator access paths can resolve keys back to durables. The tests cover both
 * constructors, hydration ordering, {@code maxSize} truncation, {@code copy()} independence, and the prefetch path that
 * falls back to per-element ORM lookups when no {@code VectoredDao} is present.
 */
@Test(groups = "unit")
public class ByKeyExtrinsicVectorTest {

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void registerDao () {

    new PerApplicationContext();

    ORMDao<Long, Gadget, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    for (long id = 1L; id <= 5L; id++) {
      Mockito.when(ormDao.getIdFromString(Long.toString(id))).thenReturn(id);
      Mockito.when(ormDao.get(id)).thenReturn(new Gadget(id));
      Mockito.when(ormDao.acquire(Gadget.class, id)).thenReturn(new Gadget(id));
    }

    OrmDaoManager.register(Gadget.class, ormDao);
  }

  private static List<Gadget> gadgets (long... ids) {

    List<Gadget> gadgets = new ArrayList<>();

    for (long id : ids) {
      gadgets.add(new Gadget(id));
    }

    return gadgets;
  }

  private static ByKeyExtrinsicVector<Long, Gadget> vectorFor (long... ids) {

    return new ByKeyExtrinsicVector<>(Gadget.class, gadgets(ids), null, 0, 0, false);
  }

  private static List<Long> idsOf (List<Gadget> gadgets) {

    List<Long> ids = new ArrayList<>();

    for (Gadget gadget : gadgets) {
      ids.add(gadget.getId());
    }

    return ids;
  }

  public void testLazyListHydratesKeysInOrder () {

    ByKeyExtrinsicVector<Long, Gadget> vector = vectorFor(1L, 2L, 3L);

    Assert.assertEquals(idsOf(vector.asBestEffortLazyList()), List.of(1L, 2L, 3L), "the lazy list should hydrate every keyed element in order");
  }

  public void testPreFetchedListHydratesKeysInOrder () {

    ByKeyExtrinsicVector<Long, Gadget> vector = vectorFor(1L, 2L, 3L);

    // With no VectoredDao registered on the mock, prefetch() falls back to a straight key-by-key hydration of the roster.
    Assert.assertEquals(idsOf(vector.asBestEffortPreFetchedList()), List.of(1L, 2L, 3L), "the prefetched list should hydrate every keyed element in order");
  }

  public void testIteratorHydratesEveryKey () {

    ByKeyExtrinsicVector<Long, Gadget> vector = vectorFor(1L, 2L, 3L);

    int count = 0;
    for (java.util.Iterator<Gadget> iterator = vector.iterator(); iterator.hasNext(); iterator.next()) {
      count++;
    }

    Assert.assertEquals(count, 3, "the iterator should traverse every keyed element");
  }

  public void testConstructorTruncatesAtMaxSize () {

    ByKeyExtrinsicVector<Long, Gadget> vector = new ByKeyExtrinsicVector<>(Gadget.class, gadgets(1L, 2L, 3L, 4L), null, 2, 0, false);

    Assert.assertEquals(vector.getRoster().size(), 2, "a positive maxSize should cap the number of stored elements");
    Assert.assertEquals(idsOf(vector.asBestEffortLazyList()), List.of(1L, 2L), "truncation should retain the leading elements in order");
  }

  public void testCopyPreservesContentsAndOrdering () {

    ByKeyExtrinsicVector<Long, Gadget> vector = vectorFor(1L, 2L, 3L);
    DurableVector<Long, Gadget> copy = vector.copy();

    Assert.assertTrue(copy instanceof ByKeyExtrinsicVector, "copy should produce another ByKeyExtrinsicVector");
    Assert.assertEquals(idsOf(copy.asBestEffortLazyList()), List.of(1L, 2L, 3L), "the copy should preserve the contents and ordering of the original");
  }

  public void testCopyIsIndependentOfOriginal () {

    ByKeyExtrinsicVector<Long, Gadget> vector = vectorFor(1L, 2L);
    DurableVector<Long, Gadget> copy = vector.copy();

    vector.add(new Gadget(3L));

    Assert.assertEquals(vector.asBestEffortLazyList().size(), 3, "the mutated original should reflect the added element");
    Assert.assertEquals(copy.asBestEffortLazyList().size(), 2, "the copy should be unaffected by mutation of the original");
  }

  public void testCopyPreservesConfiguration () {

    ByKeyExtrinsicVector<Long, Gadget> vector = new ByKeyExtrinsicVector<>(Gadget.class, gadgets(1L, 2L), null, 7, 42, true);
    DurableVector<Long, Gadget> copy = vector.copy();

    Assert.assertEquals(copy.getMaxSize(), 7, "copy should preserve maxSize");
    Assert.assertEquals(copy.getTimeToLiveSeconds(), 42, "copy should preserve the time to live");
    Assert.assertTrue(copy.isOrdered(), "copy should preserve the ordered flag");
  }

  public static class Gadget extends AbstractDurable<Long, Gadget> {

    private static final long serialVersionUID = 1L;

    private Long id;

    public Gadget () {

    }

    public Gadget (Long id) {

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
