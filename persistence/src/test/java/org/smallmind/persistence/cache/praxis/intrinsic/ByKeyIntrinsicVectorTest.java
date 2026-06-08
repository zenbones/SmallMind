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
package org.smallmind.persistence.cache.praxis.intrinsic;

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
 * Unit tests for {@link ByKeyIntrinsicVector}, which stores elements as keys in an {@link IntrinsicRoster}
 * and hydrates them through the registered {@link ORMDao} when read. A Mockito DAO mock is registered with
 * {@link OrmDaoManager} (which requires a {@link PerApplicationContext} bound to the thread) so the lazy list,
 * iterator, and {@code head()} access paths can resolve keys back to durables. The tests cover hydration,
 * {@code copy()} independence, and add/remove mutation.
 */
@Test(groups = "unit")
public class ByKeyIntrinsicVectorTest {

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void registerDao () {

    new PerApplicationContext();

    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    for (long id = 1L; id <= 5L; id++) {
      Mockito.when(ormDao.getIdFromString(Long.toString(id))).thenReturn(id);
      Mockito.when(ormDao.get(id)).thenReturn(new Cog(id));
    }

    OrmDaoManager.register(Cog.class, ormDao);
  }

  private static ByKeyIntrinsicVector<Long, Cog> vectorFor (long... ids) {

    List<Cog> cogs = new ArrayList<>();

    for (long id : ids) {
      cogs.add(new Cog(id));
    }

    return new ByKeyIntrinsicVector<>(Cog.class, cogs, null, 0, 0, false);
  }

  private static List<Long> idsOf (List<Cog> cogs) {

    List<Long> ids = new ArrayList<>();

    for (Cog cog : cogs) {
      ids.add(cog.getId());
    }

    return ids;
  }

  public void testLazyListHydratesKeysThroughDao () {

    ByKeyIntrinsicVector<Long, Cog> vector = vectorFor(1L, 2L);

    Assert.assertEquals(idsOf(vector.asBestEffortLazyList()), List.of(1L, 2L), "the lazy list should hydrate every keyed element in order");
  }

  public void testIteratorHydratesKeysThroughDao () {

    ByKeyIntrinsicVector<Long, Cog> vector = vectorFor(1L, 2L, 3L);

    int count = 0;
    for (java.util.Iterator<Cog> iterator = vector.iterator(); iterator.hasNext(); iterator.next()) {
      count++;
    }

    Assert.assertEquals(count, 3, "the iterator should traverse every keyed element");
  }

  public void testCopyIsIndependentOfOriginal () {

    ByKeyIntrinsicVector<Long, Cog> vector = vectorFor(1L, 2L);
    DurableVector<Long, Cog> copy = vector.copy();

    vector.add(new Cog(3L));

    Assert.assertEquals(vector.asBestEffortLazyList().size(), 3, "the mutated original should reflect the added element");
    Assert.assertEquals(copy.asBestEffortLazyList().size(), 2, "the copy should be unaffected by mutation of the original");
  }

  public void testAddAndRemoveMutateTheVector () {

    ByKeyIntrinsicVector<Long, Cog> vector = vectorFor(1L, 2L);

    vector.add(new Cog(3L));
    Assert.assertEquals(idsOf(vector.asBestEffortLazyList()), List.of(3L, 1L, 2L), "an unordered add prepends the new element to the vector");

    vector.remove(new Cog(1L));
    Assert.assertEquals(idsOf(vector.asBestEffortLazyList()), List.of(3L, 2L), "removing a durable should drop its key from the vector");
  }

  public void testHeadReturnsFirstHydratedDurable () {

    Assert.assertEquals(vectorFor(2L, 4L).head().getId(), Long.valueOf(2L), "head should hydrate and return the first keyed element");
  }

  public static class Cog extends AbstractDurable<Long, Cog> {

    private static final long serialVersionUID = 1L;

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
