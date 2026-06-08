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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link ByKeyRoster}. The type-guard, containment, and structural-delegation branches are
 * tested without a DAO because they only touch the backing key roster. The DAO-hydration path
 * ({@link ByKeyRoster#prefetch()}) is tested by registering a Mockito {@link ORMDao} mock through
 * {@link OrmDaoManager}, which requires a {@link PerApplicationContext} to be attached to the test
 * thread first.
 */
@Test(groups = "unit")
public class ByKeyRosterTest {

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void attachApplicationContext () {

    // OrmDaoManager.get(...) reads PerApplicationContext, which throws unless a context map is bound to this thread.
    // The map is a static InheritableThreadLocal that is reused (not recreated) across methods, so registrations leak
    // between tests on the same Surefire thread; install a fresh registry every method to keep the missing-DAO branch
    // observable and to stop a foreign DAO bleeding into hydration here.
    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(OrmDaoManager.class, new ConcurrentHashMap<>());

    // ByKeyRoster hydrates each key through OrmDaoManager on get/iterate/remove, so register a DAO that turns the
    // key's id string back into a Cog. Tests that need different hydration (prefetch) re-register their own mock.
    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    Mockito.when(ormDao.getManagedClass()).thenReturn(Cog.class);
    for (long id = 1L; id <= 9L; id++) {
      Mockito.when(ormDao.getIdFromString(Long.toString(id))).thenReturn(id);
      Mockito.when(ormDao.get(id)).thenReturn(new Cog(id));
    }

    OrmDaoManager.register(Cog.class, ormDao);
  }

  private static IntrinsicRoster<DurableKey<Long, Cog>> keyRosterFor (long... ids) {

    IntrinsicRoster<DurableKey<Long, Cog>> keyRoster = new IntrinsicRoster<DurableKey<Long, Cog>>();

    for (long id : ids) {
      keyRoster.add(new DurableKey<>(Cog.class, id));
    }

    return keyRoster;
  }

  private static ByKeyRoster<Long, Cog> rosterFor (long... ids) {

    return new ByKeyRoster<>(Cog.class, keyRosterFor(ids));
  }

  private static List<Cog> widgets (long... ids) {

    List<Cog> list = new ArrayList<>();

    for (long id : ids) {
      list.add(new Cog(id));
    }

    return list;
  }

  public void testContainsRejectsNonDurableClassElementWithoutDao () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertFalse(roster.contains("not-a-widget"));
  }

  public void testContainsMatchesByKeyForDurableClassElement () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertTrue(roster.contains(new Cog(1L)));
    Assert.assertFalse(roster.contains(new Cog(9L)));
  }

  public void testRemoveRejectsNonDurableClassElementWithoutDao () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertFalse(roster.remove("not-a-widget"));
    Assert.assertEquals(roster.size(), 2);
  }

  public void testRemoveByDurableRemovesMatchingKey () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertTrue(roster.remove(new Cog(1L)));
    Assert.assertEquals(roster.size(), 1);
    Assert.assertFalse(roster.remove(new Cog(1L)));
  }

  public void testIndexOfRejectsNonDurableClassElement () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertEquals(roster.indexOf("not-a-widget"), -1);
  }

  public void testIndexOfAndLastIndexOfLocateByKey () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 1L);

    Assert.assertEquals(roster.indexOf(new Cog(1L)), 0);
    Assert.assertEquals(roster.lastIndexOf(new Cog(1L)), 2);
    Assert.assertEquals(roster.indexOf(new Cog(9L)), -1);
  }

  public void testLastIndexOfRejectsNonDurableClassElement () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertEquals(roster.lastIndexOf("not-a-widget"), -1);
  }

  // Regression: containsAll must check whether every requested key is present in the roster, not whether the
  // roster's own keys are all contained (the previous keySet.containsAll(keySet) form was always true).
  public void testContainsAllTrueWhenAllKeysPresent () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertTrue(roster.containsAll(widgets(1L, 2L)));
  }

  public void testContainsAllFalseWhenAKeyIsMissing () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Assert.assertFalse(roster.containsAll(widgets(1L, 3L)));
  }

  public void testContainsAllFalseWhenElementIsNotOfDurableClass () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    List<Object> mixed = new ArrayList<>();
    mixed.add(new Cog(1L));
    mixed.add("not-a-widget");

    Assert.assertFalse(roster.containsAll(mixed));
  }

  public void testAddAllAddsKeysForDurableClassElementsToBackingRoster () {

    ByKeyRoster<Long, Cog> roster = rosterFor();

    Assert.assertTrue(roster.addAll(widgets(1L, 2L)));
    Assert.assertEquals(roster.size(), 2);
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 1L)));
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 2L)));
  }

  public void testRemoveAllRemovesMatchingKeysFromBackingRoster () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Assert.assertTrue(roster.removeAll(widgets(1L, 3L)));
    Assert.assertEquals(roster.size(), 1);
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 2L)));
  }

  public void testRetainAllRetainsOnlyMatchingKeysInBackingRoster () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Assert.assertTrue(roster.retainAll(widgets(2L)));
    Assert.assertEquals(roster.size(), 1);
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 2L)));
  }

  public void testClearEmptiesBackingRoster () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    roster.clear();

    Assert.assertTrue(roster.isEmpty());
    Assert.assertEquals(roster.size(), 0);
  }

  public void testSizeAndIsEmptyReflectBackingRoster () {

    Assert.assertTrue(rosterFor().isEmpty());
    Assert.assertEquals(rosterFor().size(), 0);
    Assert.assertFalse(rosterFor(1L).isEmpty());
    Assert.assertEquals(rosterFor(1L, 2L, 3L).size(), 3);
  }

  @SuppressWarnings("unchecked")
  public void testPrefetchUsesVectorCacheHitsAndAcquireFallback () {

    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);
    VectoredDao<Long, Cog> vectoredDao = Mockito.mock(VectoredDao.class);

    Cog cached = new Cog(1L);
    Cog acquired = new Cog(2L);

    Map<DurableKey<Long, Cog>, Cog> hits = new HashMap<>();
    hits.put(new DurableKey<>(Cog.class, 1L), cached);

    Mockito.when(ormDao.getVectoredDao()).thenReturn(vectoredDao);
    Mockito.when(vectoredDao.get(Mockito.eq(Cog.class), Mockito.anyList())).thenReturn(hits);
    Mockito.when(ormDao.getIdFromString("2")).thenReturn(2L);
    Mockito.when(ormDao.acquire(Cog.class, 2L)).thenReturn(acquired);

    OrmDaoManager.register(Cog.class, ormDao);

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);
    List<Cog> prefetched = roster.prefetch();

    Assert.assertEquals(prefetched.size(), 2);
    Assert.assertSame(prefetched.get(0), cached);
    Assert.assertSame(prefetched.get(1), acquired);
  }

  @SuppressWarnings("unchecked")
  public void testPrefetchSkipsKeysThatResolveToNeitherCacheNorAcquire () {

    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);
    VectoredDao<Long, Cog> vectoredDao = Mockito.mock(VectoredDao.class);

    Cog cached = new Cog(1L);

    Map<DurableKey<Long, Cog>, Cog> hits = new HashMap<>();
    hits.put(new DurableKey<>(Cog.class, 1L), cached);

    Mockito.when(ormDao.getVectoredDao()).thenReturn(vectoredDao);
    Mockito.when(vectoredDao.get(Mockito.eq(Cog.class), Mockito.anyList())).thenReturn(hits);
    Mockito.when(ormDao.getIdFromString("2")).thenReturn(2L);
    Mockito.when(ormDao.acquire(Cog.class, 2L)).thenReturn(null);

    OrmDaoManager.register(Cog.class, ormDao);

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);
    List<Cog> prefetched = roster.prefetch();

    Assert.assertEquals(prefetched.size(), 1);
    Assert.assertSame(prefetched.get(0), cached);
  }

  @SuppressWarnings("unchecked")
  public void testPrefetchFallsBackToSelfCopyWhenNoVectoredDao () {

    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    Mockito.when(ormDao.getVectoredDao()).thenReturn(null);

    OrmDaoManager.register(Cog.class, ormDao);

    // With no vectored DAO the method returns new LinkedList<>(this), which copies via the roster's
    // iterator; that iterator hydrates each key through the same ORM DAO's get(...).
    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(new Cog(1L));

    ByKeyRoster<Long, Cog> roster = rosterFor(1L);
    List<Cog> prefetched = roster.prefetch();

    Assert.assertEquals(prefetched.size(), 1);
    Assert.assertEquals(prefetched.get(0).getId(), Long.valueOf(1L));
  }

  public void testGetHydratesDurableAtIndex () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Assert.assertEquals(roster.get(0).getId(), Long.valueOf(1L));
    Assert.assertEquals(roster.get(2).getId(), Long.valueOf(3L));
  }

  public void testSetReplacesKeyAndReturnsPreviousDurable () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Cog previous = roster.set(1, new Cog(9L));

    Assert.assertEquals(previous.getId(), Long.valueOf(2L));
    Assert.assertEquals(roster.get(1).getId(), Long.valueOf(9L));
    Assert.assertEquals(roster.size(), 3);
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 9L)));
    Assert.assertFalse(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 2L)));
  }

  public void testAddAppendsKeyToBackingRoster () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L);

    Assert.assertTrue(roster.add(new Cog(2L)));
    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(roster.get(1).getId(), Long.valueOf(2L));
  }

  public void testAddFirstPrependsKeyToBackingRoster () {

    ByKeyRoster<Long, Cog> roster = rosterFor(2L, 3L);

    roster.addFirst(new Cog(1L));

    Assert.assertEquals(roster.size(), 3);
    Assert.assertEquals(roster.get(0).getId(), Long.valueOf(1L));
  }

  public void testAddAtIndexInsertsKey () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 3L);

    roster.add(1, new Cog(2L));

    Assert.assertEquals(roster.size(), 3);
    Assert.assertEquals(roster.get(0).getId(), Long.valueOf(1L));
    Assert.assertEquals(roster.get(1).getId(), Long.valueOf(2L));
    Assert.assertEquals(roster.get(2).getId(), Long.valueOf(3L));
  }

  public void testAddAtIndexZeroInsertsAtHead () {

    ByKeyRoster<Long, Cog> roster = rosterFor(2L, 3L);

    roster.add(0, new Cog(1L));

    Assert.assertEquals(roster.get(0).getId(), Long.valueOf(1L));
  }

  public void testRemoveAtIndexReturnsHydratedDurable () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Cog removed = roster.remove(1);

    Assert.assertEquals(removed.getId(), Long.valueOf(2L));
    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(roster.get(0).getId(), Long.valueOf(1L));
    Assert.assertEquals(roster.get(1).getId(), Long.valueOf(3L));
  }

  public void testRemoveLastReturnsHydratedDurable () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Cog removed = roster.removeLast();

    Assert.assertEquals(removed.getId(), Long.valueOf(3L));
    Assert.assertEquals(roster.size(), 2);
  }

  public void testAddAllAtIndexAddsKeysForDurableClassElements () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 4L);

    Assert.assertTrue(roster.addAll(1, widgets(2L, 3L)));
    Assert.assertEquals(roster.size(), 4);
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 2L)));
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 3L)));
    // The first and last elements outside the insertion point keep their positions.
    Assert.assertEquals(roster.get(0).getId(), Long.valueOf(1L));
    Assert.assertEquals(roster.get(3).getId(), Long.valueOf(4L));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testAddAllAtIndexSkipsNonDurableClassElements () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L);

    List<Object> mixed = new ArrayList<>();
    mixed.add(new Cog(2L));
    mixed.add("not-a-widget");

    Assert.assertTrue(roster.addAll(1, (List)mixed));
    Assert.assertEquals(roster.size(), 2);
    Assert.assertTrue(roster.getInternalRoster().contains(new DurableKey<>(Cog.class, 2L)));
  }

  public void testToArrayWithNullAllocatesDurableArray () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Object[] array = roster.toArray();

    Assert.assertEquals(array.length, 3);
    Assert.assertEquals(((Cog)array[0]).getId(), Long.valueOf(1L));
    Assert.assertEquals(((Cog)array[2]).getId(), Long.valueOf(3L));
  }

  public void testToArrayWithTypedArrayHydratesDurables () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    Cog[] array = roster.toArray(new Cog[2]);

    Assert.assertEquals(array.length, 2);
    Assert.assertEquals(array[0].getId(), Long.valueOf(1L));
    Assert.assertEquals(array[1].getId(), Long.valueOf(2L));
  }

  public void testToArrayWithUndersizedArrayAllocatesNewArray () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    Cog[] array = roster.toArray(new Cog[1]);

    Assert.assertEquals(array.length, 3);
    Assert.assertEquals(array[2].getId(), Long.valueOf(3L));
  }

  public void testSubListReturnsByKeyRosterOverRange () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L, 4L);

    List<Cog> sub = roster.subList(1, 3);

    Assert.assertTrue(sub instanceof ByKeyRoster);
    Assert.assertEquals(sub.size(), 2);
    Assert.assertEquals(sub.get(0).getId(), Long.valueOf(2L));
    Assert.assertEquals(sub.get(1).getId(), Long.valueOf(3L));
  }

  public void testListIteratorHydratesDurablesInOrder () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    ListIterator<Cog> iterator = roster.listIterator();

    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(iterator.next().getId(), Long.valueOf(1L));
    Assert.assertEquals(iterator.next().getId(), Long.valueOf(2L));
    Assert.assertEquals(iterator.next().getId(), Long.valueOf(3L));
    Assert.assertFalse(iterator.hasNext());
  }

  public void testListIteratorAtIndexStartsAtPosition () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L, 3L);

    ListIterator<Cog> iterator = roster.listIterator(1);

    Assert.assertEquals(iterator.nextIndex(), 1);
    Assert.assertEquals(iterator.next().getId(), Long.valueOf(2L));
  }

  public void testIteratorHydratesDurablesInOrder () {

    ByKeyRoster<Long, Cog> roster = rosterFor(1L, 2L);

    List<Cog> copied = new ArrayList<>();

    for (Cog cog : roster) {
      copied.add(cog);
    }

    Assert.assertEquals(copied.size(), 2);
    Assert.assertEquals(copied.get(0).getId(), Long.valueOf(1L));
    Assert.assertEquals(copied.get(1).getId(), Long.valueOf(2L));
  }

  public void testGetORMDaoThrowsWhenNoDaoRegistered () {

    // Sprocket is never registered, so the lazy getORMDao lookup must fail.
    ByKeyRoster<Long, Sprocket> roster = new ByKeyRoster<>(Sprocket.class, new IntrinsicRoster<DurableKey<Long, Sprocket>>());

    Assert.assertThrows(CacheOperationException.class, roster::listIterator);
  }

  @SuppressWarnings("unchecked")
  public void testGetThrowsWhenDurableCannotBeHydrated () {

    ORMDao<Long, Cog, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    Mockito.when(ormDao.getIdFromString("1")).thenReturn(1L);
    Mockito.when(ormDao.get(1L)).thenReturn(null);

    OrmDaoManager.register(Cog.class, ormDao);

    ByKeyRoster<Long, Cog> roster = rosterFor(1L);

    Assert.assertThrows(CacheOperationException.class, () -> roster.get(0));
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

  /**
   * A second durable type that is deliberately never registered with {@link OrmDaoManager}, used to exercise the
   * missing-DAO failure branch of {@link ByKeyRoster#getORMDao()}.
   */
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
