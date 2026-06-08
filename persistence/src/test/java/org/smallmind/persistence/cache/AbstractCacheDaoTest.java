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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.praxis.ByReferenceSingularVector;
import org.smallmind.persistence.cache.praxis.intrinsic.ByReferenceIntrinsicVector;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the shared logic in {@link AbstractCacheDao}, exercised through a minimal concrete
 * subclass over hand-rolled in-memory {@link CacheDomain} and {@link PersistenceCache} fakes. No
 * external cache infrastructure is involved.
 */
@Test(groups = "unit")
public class AbstractCacheDaoTest {

  private InMemoryCacheDomain<Long, Gadget> domain () {

    return new InMemoryCacheDomain<>("source");
  }

  private ConcreteCacheDao<Long, Gadget> dao (CacheDomain<Long, Gadget> cacheDomain) {

    return new ConcreteCacheDao<>(cacheDomain);
  }

  private VectorKey<Gadget> vectorKey (String namespace) {

    return new VectorKey<>(new VectorArtifact(namespace, new VectorIndex[] {new VectorIndex("k", "v", "")}), Gadget.class);
  }

  public void testGetByIdReturnsCachedDurable () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);

    cacheDomain.getInstanceCache(Gadget.class).set(new DurableKey<>(Gadget.class, 1L).getKey(), new Gadget(1L, "alpha"), 0);

    Assert.assertEquals(cacheDao.get(Gadget.class, 1L).getName(), "alpha");
  }

  public void testGetByIdReturnsNullWhenAbsent () {

    Assert.assertNull(dao(domain()).get(Gadget.class, 999L));
  }

  public void testMultiKeyGetRemapsKeysToDurables () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);

    PersistenceCache<String, Gadget> instanceCache = cacheDomain.getInstanceCache(Gadget.class);
    DurableKey<Long, Gadget> keyA = new DurableKey<>(Gadget.class, 1L);
    DurableKey<Long, Gadget> keyB = new DurableKey<>(Gadget.class, 2L);

    instanceCache.set(keyA.getKey(), new Gadget(1L, "alpha"), 0);
    instanceCache.set(keyB.getKey(), new Gadget(2L, "beta"), 0);

    List<DurableKey<Long, Gadget>> durableKeys = new ArrayList<>();
    durableKeys.add(keyA);
    durableKeys.add(keyB);

    Map<DurableKey<Long, Gadget>, Gadget> result = cacheDao.get(Gadget.class, durableKeys);

    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get(keyA).getName(), "alpha");
    Assert.assertEquals(result.get(keyB).getName(), "beta");
  }

  public void testMultiKeyGetOmitsKeysAbsentFromCache () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);

    DurableKey<Long, Gadget> present = new DurableKey<>(Gadget.class, 1L);
    DurableKey<Long, Gadget> missing = new DurableKey<>(Gadget.class, 2L);

    cacheDomain.getInstanceCache(Gadget.class).set(present.getKey(), new Gadget(1L, "alpha"), 0);

    List<DurableKey<Long, Gadget>> durableKeys = new ArrayList<>();
    durableKeys.add(present);
    durableKeys.add(missing);

    Map<DurableKey<Long, Gadget>, Gadget> result = cacheDao.get(Gadget.class, durableKeys);

    Assert.assertEquals(result.size(), 1);
    Assert.assertTrue(result.containsKey(present));
    Assert.assertFalse(result.containsKey(missing));
  }

  public void testMultiKeyGetShortCircuitsOnNullInput () {

    Map<DurableKey<Long, Gadget>, Gadget> result = dao(domain()).get(Gadget.class, (List<DurableKey<Long, Gadget>>)null);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  public void testMultiKeyGetShortCircuitsOnEmptyInput () {

    Map<DurableKey<Long, Gadget>, Gadget> result = dao(domain()).get(Gadget.class, new ArrayList<DurableKey<Long, Gadget>>());

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  public void testDeleteRemovesDurableFromInstanceCache () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);

    cacheDomain.getInstanceCache(Gadget.class).set(new DurableKey<>(Gadget.class, 1L).getKey(), new Gadget(1L, "alpha"), 0);

    cacheDao.delete(Gadget.class, new Gadget(1L, "alpha"));

    Assert.assertNull(cacheDao.get(Gadget.class, 1L));
  }

  public void testDeleteWithNullDurableIsNoOp () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);

    cacheDomain.getInstanceCache(Gadget.class).set(new DurableKey<>(Gadget.class, 1L).getKey(), new Gadget(1L, "alpha"), 0);

    cacheDao.delete(Gadget.class, null);

    Assert.assertEquals(cacheDao.get(Gadget.class, 1L).getName(), "alpha");
  }

  public void testPersistVectorStoresWhenAbsentAndReturnsProvidedVector () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    DurableVector<Long, Gadget> provided = new ByReferenceSingularVector<>(new Gadget(1L, "alpha"), 0);
    DurableVector<Long, Gadget> result = cacheDao.persistVector(vectorKey, provided);

    Assert.assertSame(result, provided);
    Assert.assertNotNull(cacheDao.getVector(vectorKey));
    Assert.assertTrue(cacheDao.getVector(vectorKey).isSingular());
  }

  public void testPersistVectorReturnsExistingVectorWhenAlreadyPresent () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    DurableVector<Long, Gadget> first = new ByReferenceSingularVector<>(new Gadget(1L, "alpha"), 0);
    DurableVector<Long, Gadget> second = new ByReferenceSingularVector<>(new Gadget(2L, "beta"), 0);

    cacheDao.persistVector(vectorKey, first);

    DurableVector<Long, Gadget> result = cacheDao.persistVector(vectorKey, second);

    Assert.assertNotSame(result, second);
    Assert.assertEquals(result.head().getName(), "alpha");
    Assert.assertEquals(cacheDao.getVector(vectorKey).head().getName(), "alpha");
  }

  public void testDeleteVectorRemovesVector () {

    InMemoryCacheDomain<Long, Gadget> cacheDomain = domain();
    ConcreteCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    cacheDao.persistVector(vectorKey, new ByReferenceSingularVector<>(new Gadget(1L, "alpha"), 0));
    cacheDao.deleteVector(vectorKey);

    Assert.assertNull(cacheDao.getVector(vectorKey));
  }

  public void testGetMetricSourceComesFromDomain () {

    Assert.assertEquals(dao(new InMemoryCacheDomain<>("tagged")).getMetricSource(), "tagged");
  }

  /**
   * Minimal concrete {@link AbstractCacheDao} that adds nothing beyond the shared base behaviour
   * under test, requiring a concrete {@link #migrateVector} only because the abstract surface of the
   * persistence cache contract is exercised through {@code persistVector}.
   */
  private static class ConcreteCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

    public ConcreteCacheDao (CacheDomain<I, D> cacheDomain) {

      super(cacheDomain);
    }

    public D persist (Class<D> durableClass, D durable, UpdateMode mode) {

      return durable;
    }

    public void updateInVector (VectorKey<D> vectorKey, D durable) {

    }

    public void removeFromVector (VectorKey<D> vectorKey, D durable) {

    }

    public DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector) {

      return vector;
    }

    public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds) {

      return new ByReferenceSingularVector<>(durable, timeToLiveSeconds);
    }

    public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

      IntrinsicRoster<D> roster = new IntrinsicRoster<>();

      for (D durable : elementIter) {
        roster.add(durable);
      }

      return new ByReferenceIntrinsicVector<>(roster, comparator, maxSize, timeToLiveSeconds, ordered);
    }
  }

  /**
   * In-memory {@link CacheDomain} that hands out a stable per-class instance and vector cache. The
   * caches are real, process-local maps with put-if-absent and remove semantics.
   */
  private static class InMemoryCacheDomain<I extends Serializable & Comparable<I>, D extends Durable<I>> implements CacheDomain<I, D> {

    private final InMemoryPersistenceCache<D> instanceCache = new InMemoryPersistenceCache<>();
    private final InMemoryPersistenceCache<List<D>> wideInstanceCache = new InMemoryPersistenceCache<>();
    private final InMemoryPersistenceCache<DurableVector<I, D>> vectorCache = new InMemoryPersistenceCache<>();
    private final String metricSource;

    public InMemoryCacheDomain (String metricSource) {

      this.metricSource = metricSource;
    }

    public String getMetricSource () {

      return metricSource;
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
   * Process-local {@link PersistenceCache} backed by a {@link HashMap}, implementing real get,
   * bulk get, set, put-if-absent, and remove semantics with omission of absent keys.
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

  public static class Gadget extends AbstractDurable<Long, Gadget> {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;

    public Gadget () {

    }

    public Gadget (Long id, String name) {

      this.id = id;
      this.name = name;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }
  }
}
