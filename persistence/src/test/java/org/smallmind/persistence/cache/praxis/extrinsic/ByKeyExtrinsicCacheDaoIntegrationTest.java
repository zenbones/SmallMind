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

import java.util.List;
import java.util.Map;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.CubbyMemcachedClient;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorArtifact;
import org.smallmind.persistence.cache.VectorIndex;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.memcached.MemcachedCacheDomain;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test for the out-of-process caching layer — {@link MemcachedCacheDomain} fronted by a
 * {@link ByKeyExtrinsicCacheDao} — against a real memcached container. It verifies that durables and their
 * vectors genuinely round-trip through memcached (full serialization, not a process-local map), that update
 * modes and eviction behave as specified, and that the CAS-driven vector mutation path operates against real
 * server CAS tokens.
 *
 * <p>Vector assertions stay on the hydration-free surface (presence, singularity, deletion) so the test needs
 * no backing {@code ORMDao} to resolve keys into durables.
 *
 * <p>Requires a running Docker daemon; the {@code memcached:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class ByKeyExtrinsicCacheDaoIntegrationTest extends AbstractGroundwaterTest {

  private static final String DISCRIMINATOR = "cachedao";

  private CubbyMemcachedClient client;

  public ByKeyExtrinsicCacheDaoIntegrationTest () {

    super(DockerApplication.MEMCACHED);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();

    super.beforeClass();

    client = new CubbyMemcachedClient(CubbyConfiguration.OPTIMAL, new MemcachedHost("0", "localhost", 11211));
    client.start();
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    try {
      if (client != null) {
        client.stop();
      }
    } finally {
      super.afterClass();
    }
  }

  private MemcachedCacheDomain<Long, Gadget> domain (int timeToLiveSeconds) {

    return new MemcachedCacheDomain<>(client, DISCRIMINATOR, timeToLiveSeconds);
  }

  private ByKeyExtrinsicCacheDao<Long, Gadget> dao (MemcachedCacheDomain<Long, Gadget> cacheDomain) {

    return new ByKeyExtrinsicCacheDao<>(cacheDomain);
  }

  private VectorKey<Gadget> vectorKey (String namespace) {

    return new VectorKey<>(new VectorArtifact(namespace, new VectorIndex[] {new VectorIndex("k", "v", "")}), Gadget.class);
  }

  public void testPersistHardThenGetRoundTripsDurableThroughServer () {

    dao(domain(3600)).persist(Gadget.class, new Gadget(1L, "alpha"), UpdateMode.HARD);

    // A brand-new domain and DAO over the same client and discriminator: a hit here proves the durable
    // lives in memcached, not in any process-local structure, and that its fields survived serialization.
    Gadget fetched = dao(domain(3600)).get(Gadget.class, 1L);

    Assert.assertNotNull(fetched);
    Assert.assertEquals(fetched.getId(), Long.valueOf(1L));
    Assert.assertEquals(fetched.getName(), "alpha");
  }

  public void testPersistSoftReturnsExistingInsteadOfOverwriting () {

    ByKeyExtrinsicCacheDao<Long, Gadget> cacheDao = dao(domain(3600));

    Assert.assertEquals(cacheDao.persist(Gadget.class, new Gadget(10L, "alpha"), UpdateMode.SOFT).getName(), "alpha");
    Assert.assertEquals(cacheDao.persist(Gadget.class, new Gadget(10L, "beta"), UpdateMode.SOFT).getName(), "alpha");
    Assert.assertEquals(cacheDao.get(Gadget.class, 10L).getName(), "alpha");
  }

  public void testPersistHardOverwritesExistingValue () {

    ByKeyExtrinsicCacheDao<Long, Gadget> cacheDao = dao(domain(3600));

    cacheDao.persist(Gadget.class, new Gadget(15L, "alpha"), UpdateMode.HARD);
    cacheDao.persist(Gadget.class, new Gadget(15L, "beta"), UpdateMode.HARD);

    Assert.assertEquals(cacheDao.get(Gadget.class, 15L).getName(), "beta");
  }

  public void testDeleteEvictsDurable () {

    ByKeyExtrinsicCacheDao<Long, Gadget> cacheDao = dao(domain(3600));

    cacheDao.persist(Gadget.class, new Gadget(20L, "x"), UpdateMode.HARD);
    cacheDao.delete(Gadget.class, new Gadget(20L, "x"));

    Assert.assertNull(cacheDao.get(Gadget.class, 20L));
  }

  public void testPersistVectorRoundTripsAndDeletes () {

    ByKeyExtrinsicCacheDao<Long, Gadget> cacheDao = dao(domain(3600));
    VectorKey<Gadget> vectorKey = vectorKey("listed");

    DurableVector<Long, Gadget> vector = cacheDao.createVector(vectorKey, List.of(new Gadget(40L, "a"), new Gadget(41L, "b")), null, 0, 0, false);

    cacheDao.persistVector(vectorKey, vector);

    DurableVector<Long, Gadget> fetched = cacheDao.getVector(vectorKey);

    Assert.assertNotNull(fetched);
    Assert.assertFalse(fetched.isSingular());

    cacheDao.deleteVector(vectorKey);

    Assert.assertNull(cacheDao.getVector(vectorKey));
  }

  public void testSingularVectorRemovalUsesCasAndDeletesVector () {

    ByKeyExtrinsicCacheDao<Long, Gadget> cacheDao = dao(domain(3600));
    VectorKey<Gadget> vectorKey = vectorKey("singular");
    Gadget gadget = new Gadget(50L, "solo");

    cacheDao.persistVector(vectorKey, cacheDao.createSingularVector(vectorKey, gadget, 0));

    DurableVector<Long, Gadget> fetched = cacheDao.getVector(vectorKey);

    Assert.assertNotNull(fetched);
    Assert.assertTrue(fetched.isSingular());

    // Drives the real getViaCas/putViaCas path; a singular removal deletes the whole vector.
    cacheDao.removeFromVector(vectorKey, gadget);

    Assert.assertNull(cacheDao.getVector(vectorKey));
  }

  public void testPerClassTtlOverrideExpiresDurable ()
    throws InterruptedException {

    MemcachedCacheDomain<Long, Gadget> cacheDomain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 3600, Map.of(Gadget.class, 1));
    ByKeyExtrinsicCacheDao<Long, Gadget> cacheDao = dao(cacheDomain);

    cacheDao.persist(Gadget.class, new Gadget(60L, "fleeting"), UpdateMode.HARD);

    Assert.assertNotNull(cacheDao.get(Gadget.class, 60L));

    Thread.sleep(1500);

    Assert.assertNull(cacheDao.get(Gadget.class, 60L));
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
