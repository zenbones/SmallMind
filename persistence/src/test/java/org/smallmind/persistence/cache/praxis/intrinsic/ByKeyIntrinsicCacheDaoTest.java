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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.PersistenceCache;
import org.smallmind.persistence.cache.VectorArtifact;
import org.smallmind.persistence.cache.VectorIndex;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.praxis.ByKeySingularVector;
import org.smallmind.persistence.cache.praxis.ByReferenceSingularVector;
import org.smallmind.persistence.cache.praxis.Roster;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ByKeyIntrinsicCacheDao}, the key-based intrinsic (in-process) cache DAO. Unlike the
 * extrinsic twin it performs vector mutations directly on the shared in-memory vector (no compare-and-swap),
 * so the cache domain is backed by plain {@link java.util.HashMap}-based fakes. Because a {@link ByKeyIntrinsicVector}
 * stores {@link org.smallmind.persistence.cache.DurableKey} references and hydrates them through {@link OrmDaoManager}
 * on add/iterate, the fixture binds a {@link PerApplicationContext} and registers a Mockito {@link ORMDao} stub.
 * Coverage spans the (mode-ignoring) put-if-absent persist, vector insert/remove (including the singular delete
 * short circuit and the absent/null no-ops), vector migration to and from the key-based forms, and the singular
 * and multi vector factories.
 */
@Test(groups = "unit")
public class ByKeyIntrinsicCacheDaoTest {

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void registerDao () {

    // ByKeyIntrinsicVector.add iterates the key roster (for uniqueness), which hydrates keys through
    // OrmDaoManager; that lookup requires a PerApplicationContext bound to the thread plus a registered DAO.
    // Re-registering Widget.class every method overwrites any registration leaked from a prior test on this thread.
    new PerApplicationContext();

    ORMDao<Long, Widget, ?, ?> ormDao = Mockito.mock(ORMDao.class);

    for (long id = 1L; id <= 3L; id++) {
      Mockito.when(ormDao.getIdFromString(Long.toString(id))).thenReturn(id);
      Mockito.when(ormDao.get(id)).thenReturn(new Widget(id));
    }

    OrmDaoManager.register(Widget.class, ormDao);
  }

  private InMemoryCacheDomain<Long, Widget> domain () {

    return new InMemoryCacheDomain<>();
  }

  private ByKeyIntrinsicCacheDao<Long, Widget> dao (CacheDomain<Long, Widget> cacheDomain) {

    return new ByKeyIntrinsicCacheDao<>(cacheDomain);
  }

  private VectorKey<Widget> vectorKey () {

    return new VectorKey<>(new VectorArtifact("widgets", new VectorIndex[] {new VectorIndex("k", "v", "")}), Widget.class);
  }

  private List<Widget> widgets (long... ids) {

    List<Widget> list = new ArrayList<>();

    for (long id : ids) {
      list.add(new Widget(id));
    }

    return list;
  }

  private int rosterSize (DurableVector<Long, Widget> vector) {

    Roster<Widget> roster = ((ByKeyIntrinsicVector<Long, Widget>)vector).getRoster();

    return roster.size();
  }

  public void testPersistKeepsTheFirstCachedInstanceRegardlessOfMode () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    Widget first = new Widget(1L);

    // The intrinsic DAO always uses put-if-absent and ignores UpdateMode, so HARD behaves like SOFT.
    Assert.assertSame(cacheDao.persist(Widget.class, first, UpdateMode.HARD), first);
    Assert.assertSame(cacheDao.persist(Widget.class, new Widget(1L), UpdateMode.HARD), first, "persist should keep the existing cached instance");
    Assert.assertSame(cacheDao.persist(Widget.class, new Widget(1L), UpdateMode.SOFT), first, "SOFT persist should also keep the existing cached instance");
  }

  public void testPersistWithNullDurableReturnsNull () {

    Assert.assertNull(dao(domain()).persist(Widget.class, null, UpdateMode.HARD));
  }

  public void testUpdateInVectorAddsDurable () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.persistVector(vectorKey, cacheDao.createVector(vectorKey, widgets(1L), null, 0, 0, false));
    cacheDao.updateInVector(vectorKey, new Widget(2L));

    Assert.assertEquals(rosterSize(cacheDao.getVector(vectorKey)), 2, "the update should add the durable's key to the vector");
  }

  public void testUpdateInVectorIsNoOpWhenVectorAbsent () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.updateInVector(vectorKey, new Widget(1L));

    Assert.assertNull(cacheDao.getVector(vectorKey), "updating an absent vector must not create one");
  }

  public void testUpdateInVectorWithNullDurableIsNoOp () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.persistVector(vectorKey, cacheDao.createVector(vectorKey, widgets(1L), null, 0, 0, false));
    cacheDao.updateInVector(vectorKey, null);

    Assert.assertEquals(rosterSize(cacheDao.getVector(vectorKey)), 1, "a null durable must leave the vector unchanged");
  }

  public void testRemoveFromVectorRemovesDurable () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.persistVector(vectorKey, cacheDao.createVector(vectorKey, widgets(1L, 2L), null, 0, 0, false));
    cacheDao.removeFromVector(vectorKey, new Widget(1L));

    Assert.assertEquals(rosterSize(cacheDao.getVector(vectorKey)), 1, "the remove should drop the durable's key from the vector");
  }

  public void testRemoveFromVectorIsNoOpWhenVectorAbsent () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.removeFromVector(vectorKey, new Widget(1L));

    Assert.assertNull(cacheDao.getVector(vectorKey));
  }

  public void testRemoveFromSingularVectorDeletesTheVector () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.persistVector(vectorKey, cacheDao.createSingularVector(vectorKey, new Widget(1L), 0));
    cacheDao.removeFromVector(vectorKey, new Widget(1L));

    Assert.assertNull(cacheDao.getVector(vectorKey), "removing from a singular vector should delete the entire vector");
  }

  public void testMigrateSingularVectorYieldsByKeySingularVector () {

    DurableVector<Long, Widget> migrated = dao(domain()).migrateVector(Widget.class, new ByReferenceSingularVector<>(new Widget(1L), 0));

    Assert.assertTrue(migrated instanceof ByKeySingularVector, "a non-key singular vector should migrate to a ByKeySingularVector");
    Assert.assertTrue(migrated.isSingular());
  }

  public void testMigrateMultiVectorYieldsByKeyIntrinsicVector () {

    IntrinsicRoster<Widget> roster = new IntrinsicRoster<>();
    roster.add(new Widget(1L));
    roster.add(new Widget(2L));

    DurableVector<Long, Widget> migrated = dao(domain()).migrateVector(Widget.class, new ByReferenceIntrinsicVector<>(roster, null, 0, 0, false));

    Assert.assertTrue(migrated instanceof ByKeyIntrinsicVector, "a non-key multi vector should migrate to a ByKeyIntrinsicVector");
    Assert.assertFalse(migrated.isSingular());
    Assert.assertEquals(rosterSize(migrated), 2);
  }

  public void testMigrateReturnsSameByKeySingularVector () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    DurableVector<Long, Widget> singular = cacheDao.createSingularVector(vectorKey(), new Widget(1L), 0);

    Assert.assertSame(cacheDao.migrateVector(Widget.class, singular), singular, "an already key-based singular vector should be returned unchanged");
  }

  public void testMigrateReturnsSameByKeyIntrinsicVector () {

    ByKeyIntrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    DurableVector<Long, Widget> multi = cacheDao.createVector(vectorKey(), widgets(1L, 2L), null, 0, 0, false);

    Assert.assertSame(cacheDao.migrateVector(Widget.class, multi), multi, "an already key-based multi vector should be returned unchanged");
  }

  public void testCreateSingularVectorIsSingular () {

    DurableVector<Long, Widget> vector = dao(domain()).createSingularVector(vectorKey(), new Widget(1L), 0);

    Assert.assertTrue(vector instanceof ByKeySingularVector);
    Assert.assertTrue(vector.isSingular());
  }

  public void testCreateVectorHoldsEveryElement () {

    DurableVector<Long, Widget> vector = dao(domain()).createVector(vectorKey(), widgets(1L, 2L, 3L), null, 0, 0, false);

    Assert.assertTrue(vector instanceof ByKeyIntrinsicVector);
    Assert.assertFalse(vector.isSingular());
    Assert.assertEquals(rosterSize(vector), 3);
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

  /**
   * In-memory {@link CacheDomain} handing out stable per-class instance and vector caches. The vector cache is a
   * plain (non-CAS) map because the intrinsic DAO mutates the shared vector directly rather than via compare-and-swap.
   */
  private static class InMemoryCacheDomain<I extends Serializable & Comparable<I>, D extends Durable<I>> implements CacheDomain<I, D> {

    private final InMemoryPersistenceCache<D> instanceCache = new InMemoryPersistenceCache<>();
    private final InMemoryPersistenceCache<List<D>> wideInstanceCache = new InMemoryPersistenceCache<>();
    private final InMemoryPersistenceCache<DurableVector<I, D>> vectorCache = new InMemoryPersistenceCache<>();

    public String getMetricSource () {

      return "source";
    }

    public PersistenceCache<String, D> getInstanceCache (Class<D> managedClass) {

      return instanceCache;
    }

    public PersistenceCache<String, List<D>> getWideInstanceCache (Class<D> managedClass) {

      return wideInstanceCache;
    }

    public PersistenceCache<String, DurableVector<I, D>> getVectorCache (Class<D> managedClass) {

      return vectorCache;
    }
  }

  /**
   * Process-local {@link PersistenceCache} backed by a {@link HashMap} with real put-if-absent, get, set, and
   * remove semantics. Put-if-absent preserves object identity, which the persist assertions depend on.
   */
  private static class InMemoryPersistenceCache<V> implements PersistenceCache<String, V> {

    private final HashMap<String, V> map = new HashMap<>();

    public int getDefaultTimeToLiveSeconds () {

      return 0;
    }

    public synchronized V get (String key) {

      return map.get(key);
    }

    public synchronized Map<String, V> get (String[] keys) {

      Map<String, V> resultMap = new HashMap<>();

      for (String key : keys) {
        if (map.containsKey(key)) {
          resultMap.put(key, map.get(key));
        }
      }

      return resultMap;
    }

    public synchronized void set (String key, V value, int timeToLiveSeconds) {

      map.put(key, value);
    }

    public synchronized V putIfAbsent (String key, V value, int timeToLiveSeconds) {

      V existing = map.get(key);

      if (existing != null) {

        return existing;
      }

      map.put(key, value);

      return null;
    }

    public synchronized void remove (String key) {

      map.remove(key);
    }
  }
}
