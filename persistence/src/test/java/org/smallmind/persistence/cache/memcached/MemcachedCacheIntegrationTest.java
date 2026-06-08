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
package org.smallmind.persistence.cache.memcached;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.CubbyMemcachedClient;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.EntitySource;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.PersistenceCache;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test for {@link MemcachedCache} against a real memcached container started through the docker
 * testbench. Unlike the unit test, which runs against an in-memory fake, this exercises the genuine memcached
 * protocol: real value serialization, real server-issued CAS tokens, real bulk gets, and real TTL expiration.
 *
 * <p>Requires a running Docker daemon; the {@code memcached:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class MemcachedCacheIntegrationTest extends AbstractGroundwaterTest {

  private static final String DISCRIMINATOR = "cacheprim";

  private CubbyMemcachedClient client;

  public MemcachedCacheIntegrationTest () {

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

  private MemcachedCache<String> cache (int timeToLiveSeconds) {

    return new MemcachedCache<>(client, DISCRIMINATOR, String.class, timeToLiveSeconds);
  }

  public void testSetThenGetRoundTripsThroughServer () {

    MemcachedCache<String> cache = cache(60);

    cache.set("set-get", "value", 0);

    Assert.assertEquals(cache.get("set-get"), "value");
    Assert.assertNull(cache.get("set-get-missing"));
  }

  public void testBulkGetReturnsValuesKeyedByDiscriminatedKey () {

    MemcachedCache<String> cache = cache(60);

    cache.set("bulk-a", "A", 0);
    cache.set("bulk-b", "B", 0);

    Map<String, String> result = cache.get(new String[] {"bulk-a", "bulk-b", "bulk-missing"});

    // The bulk result is keyed by the discriminated key. The real memcached client (unlike the in-memory
    // fake) may include an absent key with a null value, so assert on the values rather than the map size.
    Assert.assertEquals(result.get(DISCRIMINATOR + "[bulk-a]"), "A");
    Assert.assertEquals(result.get(DISCRIMINATOR + "[bulk-b]"), "B");
    Assert.assertNull(result.get(DISCRIMINATOR + "[bulk-missing]"));
  }

  public void testPutIfAbsentInsertsThenReturnsExisting () {

    MemcachedCache<String> cache = cache(60);

    Assert.assertNull(cache.putIfAbsent("pia", "first", 0));
    Assert.assertEquals(cache.putIfAbsent("pia", "second", 0), "first");
    Assert.assertEquals(cache.get("pia"), "first");
  }

  public void testGetViaCasReturnsNullInstanceForMissingKey () {

    CASValue<String> reading = cache(60).getViaCas("cas-missing");

    Assert.assertNotNull(reading);
    Assert.assertNull(reading.getValue());
  }

  public void testPutViaCasFailsOnStaleVersionAndSucceedsOnCurrentVersion () {

    MemcachedCache<String> cache = cache(60);

    cache.set("cas-rw", "first", 0);

    CASValue<String> reading = cache.getViaCas("cas-rw");

    Assert.assertEquals(reading.getValue(), "first");

    Assert.assertFalse(cache.putViaCas("cas-rw", reading.getValue(), "stale", reading.getVersion() + 1, 60));
    Assert.assertEquals(cache.get("cas-rw"), "first");

    Assert.assertTrue(cache.putViaCas("cas-rw", reading.getValue(), "second", reading.getVersion(), 60));
    Assert.assertEquals(cache.get("cas-rw"), "second");
  }

  public void testRemoveDeletesKey () {

    MemcachedCache<String> cache = cache(60);

    cache.set("rm", "value", 0);
    cache.remove("rm");

    Assert.assertNull(cache.get("rm"));
  }

  public void testSetWithDefaultTtlExpiresEntry ()
    throws InterruptedException {

    MemcachedCache<String> cache = cache(1);

    cache.set("ttl", "value", 0);

    Assert.assertEquals(cache.get("ttl"), "value");

    Thread.sleep(1500);

    Assert.assertNull(cache.get("ttl"));
  }

  public void testSetWithExplicitTtlOverridesDefault ()
    throws InterruptedException {

    // The cache default TTL is long-lived, but the per-call override should be honored, taking the
    // false branch of the (timeToLiveSeconds <= 0) ternary in set().
    MemcachedCache<String> cache = cache(600);

    cache.set("ttl-override", "value", 1);

    Assert.assertEquals(cache.get("ttl-override"), "value");

    Thread.sleep(1500);

    Assert.assertNull(cache.get("ttl-override"));
  }

  public void testPutIfAbsentWithExplicitTtlExpiresEntry ()
    throws InterruptedException {

    MemcachedCache<String> cache = cache(600);

    Assert.assertNull(cache.putIfAbsent("pia-ttl", "value", 1));
    Assert.assertEquals(cache.get("pia-ttl"), "value");

    Thread.sleep(1500);

    Assert.assertNull(cache.get("pia-ttl"));
  }

  public void testPutViaCasWithDefaultTtlExpiresEntry ()
    throws InterruptedException {

    // A non-positive TTL passed to putViaCas should fall back to the cache default, taking the true
    // branch of the (timeToLiveSeconds <= 0) ternary in putViaCas().
    MemcachedCache<String> cache = cache(1);

    cache.set("cas-default-ttl", "first", 600);

    CASValue<String> reading = cache.getViaCas("cas-default-ttl");

    Assert.assertTrue(cache.putViaCas("cas-default-ttl", reading.getValue(), "second", reading.getVersion(), 0));
    Assert.assertEquals(cache.get("cas-default-ttl"), "second");

    Thread.sleep(1500);

    Assert.assertNull(cache.get("cas-default-ttl"));
  }

  public void testGetRoundTripsSerializableDurable () {

    MemcachedCache<Widget> cache = new MemcachedCache<>(client, DISCRIMINATOR, Widget.class, 60);

    cache.set("durable", new Widget(42L, "alpha"), 0);

    Widget retrieved = cache.get("durable");

    Assert.assertNotNull(retrieved);
    Assert.assertEquals(retrieved.getId(), Long.valueOf(42L));
    Assert.assertEquals(retrieved.getName(), "alpha");
  }

  public void testDomainInstanceCacheRoundTripsThroughServer () {

    MemcachedCacheDomain<Long, Widget> domain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 60);

    PersistenceCache<String, Widget> instanceCache = domain.getInstanceCache(Widget.class);

    Assert.assertEquals(instanceCache.getDefaultTimeToLiveSeconds(), 60);

    instanceCache.set("domain-instance", new Widget(7L, "beta"), 0);

    Widget retrieved = instanceCache.get("domain-instance");

    Assert.assertNotNull(retrieved);
    Assert.assertEquals(retrieved.getId(), Long.valueOf(7L));
    Assert.assertEquals(retrieved.getName(), "beta");
  }

  public void testDomainCachesAreCreatedLazilyAndReused () {

    MemcachedCacheDomain<Long, Widget> domain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 60);

    // First access creates the cache; the second access must return the same instance, exercising
    // the populated-map branch of the double-checked lazy initializers.
    Assert.assertSame(domain.getInstanceCache(Widget.class), domain.getInstanceCache(Widget.class));
    Assert.assertSame(domain.getWideInstanceCache(Widget.class), domain.getWideInstanceCache(Widget.class));
    Assert.assertSame(domain.getVectorCache(Widget.class), domain.getVectorCache(Widget.class));
  }

  public void testDomainWideInstanceCacheRoundTripsListThroughServer () {

    MemcachedCacheDomain<Long, Widget> domain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 60);

    PersistenceCache<String, List<Widget>> wideInstanceCache = domain.getWideInstanceCache(Widget.class);

    wideInstanceCache.set("domain-wide", List.of(new Widget(1L, "a"), new Widget(2L, "b")), 0);

    List<Widget> retrieved = wideInstanceCache.get("domain-wide");

    Assert.assertNotNull(retrieved);
    Assert.assertEquals(retrieved.size(), 2);
    Assert.assertEquals(retrieved.get(0).getName(), "a");
    Assert.assertEquals(retrieved.get(1).getName(), "b");
  }

  public void testDomainMetricSourceMatchesMemcachedEntitySource () {

    MemcachedCacheDomain<Long, Widget> domain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 60);

    Assert.assertEquals(domain.getMetricSource(), EntitySource.MEMCACHED.getDisplay());
  }

  public void testDomainDefaultTtlAppliesWhenNoOverrideMapIsPresent () {

    MemcachedCacheDomain<Long, Widget> domain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 45);

    Assert.assertEquals(domain.getInstanceCache(Widget.class).getDefaultTimeToLiveSeconds(), 45);
  }

  public void testDomainOverrideMapSuppliesPerClassTtl () {

    Map<Class<Widget>, Integer> overrideMap = new HashMap<>();

    overrideMap.put(Widget.class, 99);

    MemcachedCacheDomain<Long, Widget> domain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 45, overrideMap);

    // The override map carries an entry for Widget, so its TTL wins over the domain default.
    Assert.assertEquals(domain.getInstanceCache(Widget.class).getDefaultTimeToLiveSeconds(), 99);
  }

  public void testDomainDefaultTtlAppliesWhenOverrideMapLacksClass () {

    Map<Class<Widget>, Integer> overrideMap = new HashMap<>();

    MemcachedCacheDomain<Long, Widget> domain = new MemcachedCacheDomain<>(client, DISCRIMINATOR, 45, overrideMap);

    // The override map is non-null but has no entry for Widget, so the domain default applies.
    Assert.assertEquals(domain.getInstanceCache(Widget.class).getDefaultTimeToLiveSeconds(), 45);
  }

  public static class Widget extends AbstractDurable<Long, Widget> {

    private Long id;
    private String name;

    public Widget () {

    }

    public Widget (Long id, String name) {

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
