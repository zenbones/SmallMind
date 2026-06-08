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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;
import org.smallmind.persistence.cache.CASSupportingPersistenceCache;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.PersistenceCache;
import org.smallmind.persistence.cache.VectorArtifact;
import org.smallmind.persistence.cache.VectorIndex;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.praxis.ByKeySingularVector;
import org.smallmind.persistence.cache.praxis.ByReferenceSingularVector;
import org.smallmind.persistence.cache.praxis.Roster;
import org.smallmind.persistence.cache.praxis.intrinsic.ByReferenceIntrinsicVector;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ByKeyExtrinsicCacheDao}, the CAS-based extrinsic cache DAO. The cache domain is backed by
 * hand-rolled in-memory caches, including a {@link CASSupportingPersistenceCache} that implements optimistic
 * compare-and-swap, so the {@code updateInVector}/{@code removeFromVector} CAS loops run without external
 * infrastructure. Vector contents are asserted through the backing key-roster size, which does not require DAO
 * hydration. Coverage spans the SOFT/HARD instance-persist modes, CAS insert and remove (including the singular
 * delete short circuit), vector migration, and the singular/multi vector factories.
 */
@Test(groups = "unit")
public class ByKeyExtrinsicCacheDaoTest {

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void registerDao () {

    // ByKeyExtrinsicVector.add/remove iterate the key roster (for uniqueness), which hydrates keys through
    // OrmDaoManager; that lookup requires a PerApplicationContext bound to the thread plus a registered DAO.
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

  private ByKeyExtrinsicCacheDao<Long, Widget> dao (CacheDomain<Long, Widget> cacheDomain) {

    return new ByKeyExtrinsicCacheDao<>(cacheDomain);
  }

  private VectorKey<Widget> vectorKey () {

    return new VectorKey<>(new VectorArtifact("widgets", new VectorIndex[] {new VectorIndex("k", "v", "")}), Widget.class);
  }

  private List<Widget> widgets (long... ids) {

    java.util.List<Widget> list = new java.util.ArrayList<>();

    for (long id : ids) {
      list.add(new Widget(id));
    }

    return list;
  }

  private int rosterSize (DurableVector<Long, Widget> vector) {

    Roster<Widget> roster = ((ByKeyExtrinsicVector<Long, Widget>)vector).getRoster();

    return roster.size();
  }

  public void testPersistSoftKeepsTheFirstCachedInstance () {

    ByKeyExtrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    Widget first = new Widget(1L);

    Assert.assertSame(cacheDao.persist(Widget.class, first, UpdateMode.SOFT), first);
    Assert.assertSame(cacheDao.persist(Widget.class, new Widget(1L), UpdateMode.SOFT), first, "SOFT persist should keep the existing cached instance");
  }

  public void testPersistHardOverwritesTheCachedInstance () {

    ByKeyExtrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    Widget replacement = new Widget(1L);

    cacheDao.persist(Widget.class, new Widget(1L), UpdateMode.HARD);
    cacheDao.persist(Widget.class, replacement, UpdateMode.HARD);

    Assert.assertSame(cacheDao.get(Widget.class, 1L), replacement, "HARD persist should overwrite the cached instance");
  }

  public void testUpdateInVectorAddsDurableViaCas () {

    ByKeyExtrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.persistVector(vectorKey, cacheDao.createVector(vectorKey, widgets(1L), null, 0, 0, false));
    cacheDao.updateInVector(vectorKey, new Widget(2L));

    Assert.assertEquals(rosterSize(cacheDao.getVector(vectorKey)), 2, "the CAS update should add the durable's key to the vector");
  }

  public void testRemoveFromVectorRemovesDurableViaCas () {

    ByKeyExtrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
    VectorKey<Widget> vectorKey = vectorKey();

    cacheDao.persistVector(vectorKey, cacheDao.createVector(vectorKey, widgets(1L, 2L), null, 0, 0, false));
    cacheDao.removeFromVector(vectorKey, new Widget(1L));

    Assert.assertEquals(rosterSize(cacheDao.getVector(vectorKey)), 1, "the CAS remove should drop the durable's key from the vector");
  }

  public void testRemoveFromSingularVectorDeletesTheVector () {

    ByKeyExtrinsicCacheDao<Long, Widget> cacheDao = dao(domain());
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

  public void testMigrateMultiVectorYieldsByKeyExtrinsicVector () {

    IntrinsicRoster<Widget> roster = new IntrinsicRoster<>();
    roster.add(new Widget(1L));
    roster.add(new Widget(2L));

    DurableVector<Long, Widget> migrated = dao(domain()).migrateVector(Widget.class, new ByReferenceIntrinsicVector<>(roster, null, 0, 0, false));

    Assert.assertTrue(migrated instanceof ByKeyExtrinsicVector, "a non-key multi vector should migrate to a ByKeyExtrinsicVector");
    Assert.assertFalse(migrated.isSingular());
    Assert.assertEquals(rosterSize(migrated), 2);
  }

  public void testCreateSingularVectorIsSingular () {

    Assert.assertTrue(dao(domain()).createSingularVector(vectorKey(), new Widget(1L), 0).isSingular());
  }

  public void testCreateVectorHoldsEveryElement () {

    DurableVector<Long, Widget> vector = dao(domain()).createVector(vectorKey(), widgets(1L, 2L, 3L), null, 0, 0, false);

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
   * In-memory {@link CacheDomain} handing out a stable instance cache and a CAS-supporting vector cache.
   */
  private static class InMemoryCacheDomain<I extends Serializable & Comparable<I>, D extends Durable<I>> implements CacheDomain<I, D> {

    private final InMemoryPersistenceCache<D> instanceCache = new InMemoryPersistenceCache<>();
    private final InMemoryPersistenceCache<List<D>> wideInstanceCache = new InMemoryPersistenceCache<>();
    private final InMemoryCASCache<DurableVector<I, D>> vectorCache = new InMemoryCASCache<>();

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
   * Process-local {@link PersistenceCache} backed by a {@link HashMap}.
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

  /**
   * Process-local {@link CASSupportingPersistenceCache} with a monotonically increasing per-key version,
   * implementing real optimistic compare-and-swap over in-process state.
   */
  private static class InMemoryCASCache<V> implements CASSupportingPersistenceCache<String, V> {

    private final HashMap<String, V> map = new HashMap<>();
    private final HashMap<String, Long> versionMap = new HashMap<>();

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
      versionMap.put(key, versionMap.getOrDefault(key, 0L) + 1);
    }

    public synchronized V putIfAbsent (String key, V value, int timeToLiveSeconds) {

      V existing = map.get(key);

      if (existing != null) {

        return existing;
      }

      map.put(key, value);
      versionMap.put(key, 0L);

      return null;
    }

    public synchronized void remove (String key) {

      map.remove(key);
      versionMap.remove(key);
    }

    public boolean requiresCopyOnDistributedCASOperation () {

      return false;
    }

    public synchronized CASValue<V> getViaCas (String key) {

      return map.containsKey(key) ? new CASValue<>(map.get(key), versionMap.get(key)) : CASValue.nullInstance();
    }

    public synchronized boolean putViaCas (String key, V oldValue, V value, long version, int timeToLiveSeconds) {

      if (versionMap.getOrDefault(key, -1L) != version) {

        return false;
      }

      map.put(key, value);
      versionMap.put(key, version + 1);

      return true;
    }
  }
}
