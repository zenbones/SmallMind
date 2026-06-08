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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.PersistenceCache;
import org.smallmind.persistence.cache.VectorArtifact;
import org.smallmind.persistence.cache.VectorIndex;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.praxis.ByReferenceSingularVector;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ByReferenceIntrinsicCacheDao}, exercised over hand-rolled in-memory
 * {@link CacheDomain} and {@link PersistenceCache} fakes. The reference-based model keeps every
 * cached durable and vector in a process-local map, so no external infrastructure is required.
 *
 * <p>Note that this DAO's {@code persist} always uses put-if-absent and ignores the {@link UpdateMode}
 * argument (the in-process by-reference model never overwrites a live instance), so HARD and SOFT
 * behave identically here; the tests assert that documented behaviour rather than the overwrite
 * semantics of the out-of-process extrinsic DAO.
 */
@Test(groups = "unit")
public class ByReferenceIntrinsicCacheDaoTest {

  private InMemoryCacheDomain<Long, Gadget> domain () {

    return new InMemoryCacheDomain<>("source");
  }

  private ByReferenceIntrinsicCacheDao<Long, Gadget> dao (CacheDomain<Long, Gadget> cacheDomain) {

    return new ByReferenceIntrinsicCacheDao<>(cacheDomain);
  }

  private VectorKey<Gadget> vectorKey (String namespace) {

    return new VectorKey<>(new VectorArtifact(namespace, new VectorIndex[] {new VectorIndex("k", "v", "")}), Gadget.class);
  }

  public void testPersistInsertsWhenAbsentAndReturnsProvidedInstance () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    Gadget gadget = new Gadget(1L, "alpha");

    Assert.assertSame(cacheDao.persist(Gadget.class, gadget, UpdateMode.HARD), gadget);
    Assert.assertEquals(cacheDao.get(Gadget.class, 1L).getName(), "alpha");
  }

  public void testPersistWithNullDurableReturnsNull () {

    Assert.assertNull(dao(domain()).persist(Gadget.class, null, UpdateMode.HARD));
  }

  public void testPersistHardDoesNotOverwriteExistingInstance () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());

    Gadget existing = new Gadget(1L, "alpha");
    cacheDao.persist(Gadget.class, existing, UpdateMode.HARD);

    Gadget challenger = new Gadget(1L, "beta");

    Assert.assertSame(cacheDao.persist(Gadget.class, challenger, UpdateMode.HARD), existing);
    Assert.assertEquals(cacheDao.get(Gadget.class, 1L).getName(), "alpha");
  }

  public void testPersistSoftReturnsExistingInstance () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());

    Gadget existing = new Gadget(1L, "alpha");
    cacheDao.persist(Gadget.class, existing, UpdateMode.SOFT);

    Assert.assertSame(cacheDao.persist(Gadget.class, new Gadget(1L, "beta"), UpdateMode.SOFT), existing);
    Assert.assertEquals(cacheDao.get(Gadget.class, 1L).getName(), "alpha");
  }

  public void testUpdateInVectorIsNoOpWhenVectorAbsent () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    cacheDao.updateInVector(vectorKey, new Gadget(1L, "alpha"));

    Assert.assertNull(cacheDao.getVector(vectorKey));
  }

  public void testUpdateInVectorAddsToExistingVector () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    DurableVector<Long, Gadget> vector = cacheDao.createVector(vectorKey, List.of(new Gadget(1L, "alpha")), null, 0, 0, false);
    cacheDao.persistVector(vectorKey, vector);

    cacheDao.updateInVector(vectorKey, new Gadget(2L, "beta"));

    Assert.assertEquals(cacheDao.getVector(vectorKey).asBestEffortLazyList().size(), 2);
  }

  public void testUpdateInVectorWithNullDurableIsNoOp () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    DurableVector<Long, Gadget> vector = cacheDao.createVector(vectorKey, List.of(new Gadget(1L, "alpha")), null, 0, 0, false);
    cacheDao.persistVector(vectorKey, vector);

    cacheDao.updateInVector(vectorKey, null);

    Assert.assertEquals(cacheDao.getVector(vectorKey).asBestEffortLazyList().size(), 1);
  }

  public void testRemoveFromVectorIsNoOpWhenVectorAbsent () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    cacheDao.removeFromVector(vectorKey, new Gadget(1L, "alpha"));

    Assert.assertNull(cacheDao.getVector(vectorKey));
  }

  public void testRemoveFromVectorRemovesElementFromMultiElementVector () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    DurableVector<Long, Gadget> vector = cacheDao.createVector(vectorKey, List.of(new Gadget(1L, "alpha"), new Gadget(2L, "beta")), null, 0, 0, false);
    cacheDao.persistVector(vectorKey, vector);

    cacheDao.removeFromVector(vectorKey, new Gadget(1L, "alpha"));

    DurableVector<Long, Gadget> fetched = cacheDao.getVector(vectorKey);

    Assert.assertNotNull(fetched);
    Assert.assertEquals(fetched.asBestEffortLazyList().size(), 1);
    Assert.assertEquals(fetched.head().getName(), "beta");
  }

  public void testRemoveFromVectorDeletesEntireSingularVector () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("singular");
    Gadget gadget = new Gadget(1L, "solo");

    cacheDao.persistVector(vectorKey, cacheDao.createSingularVector(vectorKey, gadget, 0));

    Assert.assertTrue(cacheDao.getVector(vectorKey).isSingular());

    cacheDao.removeFromVector(vectorKey, gadget);

    Assert.assertNull(cacheDao.getVector(vectorKey));
  }

  public void testMigrateVectorReturnsSameSingularWhenAlreadyByReference () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());

    DurableVector<Long, Gadget> singular = new ByReferenceSingularVector<>(new Gadget(1L, "alpha"), 0);

    Assert.assertSame(cacheDao.migrateVector(Gadget.class, singular), singular);
  }

  public void testMigrateVectorReturnsSameIntrinsicWhenAlreadyByReference () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());

    DurableVector<Long, Gadget> intrinsic = new ByReferenceIntrinsicVector<>(new IntrinsicRoster<>(List.of(new Gadget(1L, "alpha"))), null, 0, 0, false);

    Assert.assertSame(cacheDao.migrateVector(Gadget.class, intrinsic), intrinsic);
  }

  public void testMigrateVectorRebuildsForeignSingularVector () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());

    DurableVector<Long, Gadget> foreign = new ForeignSingularVector<>(new Gadget(1L, "alpha"), 0);

    DurableVector<Long, Gadget> migrated = cacheDao.migrateVector(Gadget.class, foreign);

    Assert.assertNotSame(migrated, foreign);
    Assert.assertTrue(migrated instanceof ByReferenceSingularVector);
    Assert.assertTrue(migrated.isSingular());
    Assert.assertEquals(migrated.head().getName(), "alpha");
  }

  public void testMigrateVectorRebuildsForeignMultiElementVector () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());

    DurableVector<Long, Gadget> foreign = new ForeignMultiVector<>(List.of(new Gadget(1L, "alpha"), new Gadget(2L, "beta")), 0);

    DurableVector<Long, Gadget> migrated = cacheDao.migrateVector(Gadget.class, foreign);

    Assert.assertNotSame(migrated, foreign);
    Assert.assertTrue(migrated instanceof ByReferenceIntrinsicVector);
    Assert.assertFalse(migrated.isSingular());
    Assert.assertEquals(migrated.asBestEffortLazyList().size(), 2);
  }

  public void testCreateSingularVectorNormalizesToCanonicalCachedInstance () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("singular");

    Gadget canonical = new Gadget(1L, "alpha");
    cacheDao.persist(Gadget.class, canonical, UpdateMode.HARD);

    DurableVector<Long, Gadget> vector = cacheDao.createSingularVector(vectorKey, new Gadget(1L, "beta"), 0);

    Assert.assertTrue(vector.isSingular());
    Assert.assertSame(vector.head(), canonical);
    Assert.assertEquals(vector.head().getName(), "alpha");
  }

  public void testCreateSingularVectorCachesDurableWhenAbsent () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("singular");
    Gadget gadget = new Gadget(1L, "alpha");

    DurableVector<Long, Gadget> vector = cacheDao.createSingularVector(vectorKey, gadget, 0);

    Assert.assertSame(vector.head(), gadget);
    Assert.assertSame(cacheDao.get(Gadget.class, 1L), gadget);
  }

  public void testCreateVectorNormalizesEachElementToCanonicalInstance () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    Gadget canonical = new Gadget(1L, "alpha");
    cacheDao.persist(Gadget.class, canonical, UpdateMode.HARD);

    DurableVector<Long, Gadget> vector = cacheDao.createVector(vectorKey, List.of(new Gadget(1L, "beta"), new Gadget(2L, "gamma")), null, 0, 0, false);

    List<Gadget> elements = vector.asBestEffortLazyList();

    Assert.assertEquals(elements.size(), 2);
    Assert.assertSame(elements.get(0), canonical);
    Assert.assertEquals(elements.get(0).getName(), "alpha");
    Assert.assertSame(cacheDao.get(Gadget.class, 1L), canonical);
    Assert.assertNotNull(cacheDao.get(Gadget.class, 2L));
  }

  public void testCreateVectorSkipsNullElements () {

    ByReferenceIntrinsicCacheDao<Long, Gadget> cacheDao = dao(domain());
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    java.util.ArrayList<Gadget> elements = new java.util.ArrayList<>();
    elements.add(new Gadget(1L, "alpha"));
    elements.add(null);
    elements.add(new Gadget(2L, "beta"));

    DurableVector<Long, Gadget> vector = cacheDao.createVector(vectorKey, elements, null, 0, 0, false);

    Assert.assertEquals(vector.asBestEffortLazyList().size(), 2);
  }

  /**
   * Foreign singular vector type used to drive the rebuild branch of {@code migrateVector}; it is a
   * {@link DurableVector} that is singular but not a {@link ByReferenceSingularVector}.
   */
  private static class ForeignSingularVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

    private final D durable;

    public ForeignSingularVector (D durable, int timeToLiveSeconds) {

      super(null, 1, timeToLiveSeconds, false);

      this.durable = durable;
    }

    public DurableVector<I, D> copy () {

      return new ForeignSingularVector<>(durable, getTimeToLiveSeconds());
    }

    public boolean isSingular () {

      return true;
    }

    public boolean add (D durable) {

      return false;
    }

    public boolean remove (D durable) {

      return false;
    }

    public D head () {

      return durable;
    }

    public List<D> asBestEffortLazyList () {

      return List.of(durable);
    }

    public java.util.Iterator<D> iterator () {

      return List.of(durable).iterator();
    }
  }

  /**
   * Foreign multi-element vector type used to drive the rebuild branch of {@code migrateVector}; it is
   * a {@link DurableVector} that is not singular and not a {@link ByReferenceIntrinsicVector}.
   */
  private static class ForeignMultiVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

    private final List<D> durables;

    public ForeignMultiVector (List<D> durables, int timeToLiveSeconds) {

      super(null, 0, timeToLiveSeconds, false);

      this.durables = durables;
    }

    public DurableVector<I, D> copy () {

      return new ForeignMultiVector<>(durables, getTimeToLiveSeconds());
    }

    public boolean isSingular () {

      return false;
    }

    public boolean add (D durable) {

      return false;
    }

    public boolean remove (D durable) {

      return false;
    }

    public D head () {

      return durables.isEmpty() ? null : durables.get(0);
    }

    public List<D> asBestEffortLazyList () {

      return durables;
    }

    public java.util.Iterator<D> iterator () {

      return durables.iterator();
    }
  }

  /**
   * In-memory {@link CacheDomain} handing out stable per-class instance and vector caches.
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
   * Process-local {@link PersistenceCache} backed by a {@link HashMap} with real put-if-absent,
   * get, set, and remove semantics. Put-if-absent preserves object identity, which the by-reference
   * canonicalization assertions depend on.
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
