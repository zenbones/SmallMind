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
package org.smallmind.memcached.utility;

import java.util.Arrays;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class InMemoryMemcachedClientTest {

  public void testEntryWithZeroExpirationIsNeverEvicted ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 0, "value");
    Assert.assertEquals(client.<String>get("k"), "value");
  }

  public void testEntryWithNonZeroExpirationIsEvictedLazilyAfterTtlElapses ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 1, "value");
    Assert.assertEquals(client.<String>get("k"), "value");

    Thread.sleep(1100);

    Assert.assertNull(client.<String>get("k"));
    Assert.assertNull(client.<InMemoryCASResponse<String>>casGet("k"));
  }

  public void testCasSetWithZeroTokenSucceedsWhenEntryIsAbsent ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    Assert.assertTrue(client.casSet("k", 0, "value", 0));
    Assert.assertEquals(client.<String>get("k"), "value");
  }

  public void testCasSetWithZeroTokenFailsWhenEntryIsAlive ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 0, "first");

    Assert.assertFalse(client.casSet("k", 0, "second", 0));
    Assert.assertEquals(client.<String>get("k"), "first");
  }

  public void testCasSetWithZeroTokenSucceedsAfterEntryHasExpired ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 1, "first");
    Thread.sleep(1100);

    Assert.assertTrue(client.casSet("k", 0, "second", 0));
    Assert.assertEquals(client.<String>get("k"), "second");
  }

  public void testCasSetWithMatchingTokenStoresValueAndAdvancesToken ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 0, "first");
    ProxyCASResponse<String> initial = client.casGet("k");

    Assert.assertTrue(client.casSet("k", 0, "second", initial.getCas()));

    ProxyCASResponse<String> updated = client.casGet("k");

    Assert.assertEquals(updated.getValue(), "second");
    Assert.assertNotEquals(updated.getCas(), initial.getCas());
  }

  public void testCasSetWithMismatchedTokenLeavesEntryUntouched ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 0, "first");
    ProxyCASResponse<String> reading = client.casGet("k");

    Assert.assertFalse(client.casSet("k", 0, "second", reading.getCas() + 1));
    Assert.assertEquals(client.<String>get("k"), "first");
  }

  public void testCasDeleteOfAbsentKeyIsIdempotentSuccess ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    Assert.assertTrue(client.casDelete("missing", 999L));
  }

  public void testCasDeleteWithMismatchedTokenLeavesEntry ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 0, "value");
    ProxyCASResponse<String> reading = client.casGet("k");

    Assert.assertFalse(client.casDelete("k", reading.getCas() + 1));
    Assert.assertEquals(client.<String>get("k"), "value");
  }

  public void testCasDeleteWithMatchingTokenRemovesEntry ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 0, "value");
    ProxyCASResponse<String> reading = client.casGet("k");

    Assert.assertTrue(client.casDelete("k", reading.getCas()));
    Assert.assertNull(client.<String>get("k"));
  }

  public void testTouchRefreshesExpirationWindowOfLiveEntry ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 1, "value");
    Thread.sleep(700);

    Assert.assertTrue(client.touch("k", 5));

    Thread.sleep(700);
    Assert.assertEquals(client.<String>get("k"), "value");
  }

  public void testTouchReturnsFalseWhenEntryIsAbsentOrExpired ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    Assert.assertFalse(client.touch("missing", 60));

    client.set("k", 1, "value");
    Thread.sleep(1100);
    Assert.assertFalse(client.touch("k", 60));
  }

  public void testGetAndTouchReturnsValueAndRefreshesExpiration ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("k", 1, "value");
    Thread.sleep(700);

    Assert.assertEquals(client.<String>getAndTouch("k", 5), "value");

    Thread.sleep(700);
    Assert.assertEquals(client.<String>get("k"), "value");
  }

  public void testBulkGetOmitsAbsentAndExpiredEntries ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("alive", 0, "A");
    client.set("dying", 1, "B");
    Thread.sleep(1100);

    Map<String, String> bulk = client.get(Arrays.asList("alive", "dying", "missing"));

    Assert.assertEquals(bulk.size(), 1);
    Assert.assertEquals(bulk.get("alive"), "A");
  }

  public void testClearEmptiesAllEntries ()
    throws Exception {

    InMemoryMemcachedClient client = new InMemoryMemcachedClient();

    client.set("a", 0, "1");
    client.set("b", 0, "2");

    client.clear();

    Assert.assertNull(client.<String>get("a"));
    Assert.assertNull(client.<String>get("b"));
  }
}
