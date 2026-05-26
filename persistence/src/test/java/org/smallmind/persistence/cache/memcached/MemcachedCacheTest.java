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

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.memcached.utility.InMemoryMemcachedClient;
import org.smallmind.persistence.cache.CASValue;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MemcachedCacheTest {

  public void testDiscriminatorNamespaceIsAppliedToStoredKeys ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    cache.set("a", "value", 0);

    Assert.assertEquals(client.<String>get("ns[a]"), "value");
    Assert.assertNull(client.<String>get("a"));
  }

  public void testGetReadsValueStoredUnderDiscriminatedKey ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    client.set("ns[a]", 0, "value");

    Assert.assertEquals(cache.get("a"), "value");
  }

  public void testBulkGetRehydratesEachKeyThroughDiscriminator ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    client.set("ns[a]", 0, "A");
    client.set("ns[b]", 0, "B");

    Map<String, String> result = cache.get(new String[] {"a", "b", "missing"});

    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get("ns[a]"), "A");
    Assert.assertEquals(result.get("ns[b]"), "B");
  }

  public void testPutIfAbsentInsertsWhenKeyIsMissingAndReturnsNull ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    Assert.assertNull(cache.putIfAbsent("a", "value", 0));
    Assert.assertEquals(cache.get("a"), "value");
  }

  public void testPutIfAbsentReturnsExistingValueWithoutOverwriting ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    client.set("ns[a]", 0, "existing");

    Assert.assertEquals(cache.putIfAbsent("a", "challenger", 0), "existing");
    Assert.assertEquals(cache.get("a"), "existing");
  }

  public void testPutIfAbsentReturnsConcurrentValueAfterCasMismatchLoop ()
    throws Exception {

    final AtomicBoolean firstAttempt = new AtomicBoolean(true);
    InMemoryMemcachedClient client = new InMemoryMemcachedClient() {

      @Override
      public synchronized <T> boolean casSet (String key, int expiration, T value, long cas) {

        if (firstAttempt.getAndSet(false)) {
          super.set(key, expiration, "raced");

          return false;
        }

        return super.casSet(key, expiration, value, cas);
      }
    };
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    Assert.assertEquals(cache.putIfAbsent("a", "loser", 0), "raced");
    Assert.assertEquals(cache.get("a"), "raced");
  }

  public void testSetWithNonPositiveTtlFallsBackToDefault ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 1);

    cache.set("a", "value", 0);
    Thread.sleep(1100);

    Assert.assertNull(cache.get("a"));
  }

  public void testPutViaCasFailsOnVersionMismatchAndPreservesValue ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    cache.set("a", "first", 0);
    CASValue<String> reading = cache.getViaCas("a");

    Assert.assertFalse(cache.putViaCas("a", reading.getValue(), "second", reading.getVersion() + 1, 60));
    Assert.assertEquals(cache.get("a"), "first");
  }

  public void testPutViaCasSucceedsOnMatchingVersion ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    cache.set("a", "first", 0);
    CASValue<String> reading = cache.getViaCas("a");

    Assert.assertTrue(cache.putViaCas("a", reading.getValue(), "second", reading.getVersion(), 60));
    Assert.assertEquals(cache.get("a"), "second");
  }

  public void testGetViaCasReturnsNullInstanceForMissingKey ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    CASValue<String> reading = cache.getViaCas("missing");

    Assert.assertNotNull(reading);
    Assert.assertNull(reading.getValue());
    Assert.assertEquals(reading.getVersion(), 0L);
  }

  public void testRemoveDeletesDiscriminatedKey ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();
    MemcachedCache<String> cache = new MemcachedCache<>(client, "ns", String.class, 60);

    cache.set("a", "value", 0);
    cache.remove("a");

    Assert.assertNull(cache.get("a"));
    Assert.assertNull(client.<String>get("ns[a]"));
  }
}
