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
package org.smallmind.memcached.cubby.locator;

import java.util.HashMap;
import java.util.HashSet;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MaglevKeyLocatorTest {

  private static ServerPool poolOf (String... names) {

    MemcachedHost[] hosts = new MemcachedHost[names.length];

    for (int index = 0; index < names.length; index++) {
      hosts[index] = new MemcachedHost(names[index], "host-" + names[index], 11211);
    }

    return new ServerPool(hosts);
  }

  public void testFindIsStableForSameKeyAcrossRepeatedCalls ()
    throws Exception {

    ServerPool pool = poolOf("a", "b", "c", "d");
    MaglevKeyLocator locator = new MaglevKeyLocator(50);

    locator.installRouting(pool);

    String first = locator.find(pool, "stable-key").getName();

    for (int loop = 0; loop < 25; loop++) {
      Assert.assertEquals(locator.find(pool, "stable-key").getName(), first);
    }
  }

  public void testKeysDistributeAcrossAllActiveHosts ()
    throws Exception {

    ServerPool pool = poolOf("a", "b", "c", "d");
    MaglevKeyLocator locator = new MaglevKeyLocator(50);

    locator.installRouting(pool);

    HashSet<String> hostsSeen = new HashSet<>();

    for (int loop = 0; loop < 500; loop++) {
      hostsSeen.add(locator.find(pool, "key-" + loop).getName());
    }

    Assert.assertEquals(hostsSeen.size(), 4);
  }

  public void testRemovingHostReassignsOnlyKeysThatOwnedThatHost ()
    throws Exception {

    ServerPool pool = poolOf("a", "b", "c", "d");
    MaglevKeyLocator locator = new MaglevKeyLocator(100);

    locator.installRouting(pool);

    int keyCount = 1000;
    HashMap<String, String> before = new HashMap<>();

    for (int loop = 0; loop < keyCount; loop++) {

      String key = "key-" + loop;
      before.put(key, locator.find(pool, key).getName());
    }

    pool.get("c").setActive(false);
    locator.updateRouting(pool);

    int reassignedKeysOwnedByRemovedHost = 0;
    int reassignedKeysOwnedByOtherHosts = 0;

    for (int loop = 0; loop < keyCount; loop++) {

      String key = "key-" + loop;
      String previousHost = before.get(key);
      String currentHost = locator.find(pool, key).getName();

      if (!previousHost.equals(currentHost)) {
        if ("c".equals(previousHost)) {
          reassignedKeysOwnedByRemovedHost++;
        } else {
          reassignedKeysOwnedByOtherHosts++;
        }
      }
    }

    Assert.assertTrue(reassignedKeysOwnedByRemovedHost > 0, "Keys previously routed to removed host must move");
    Assert.assertTrue(reassignedKeysOwnedByOtherHosts < reassignedKeysOwnedByRemovedHost,
                       "Maglev should not redistribute keys that did not previously own the removed host (other-host moves="
                         + reassignedKeysOwnedByOtherHosts + ", removed-host moves=" + reassignedKeysOwnedByRemovedHost + ")");
  }

  public void testEmptyPoolThrowsNoAvailableHostException () {

    MaglevKeyLocator locator = new MaglevKeyLocator(10);
    ServerPool empty = new ServerPool();

    Assert.assertThrows(NoAvailableHostException.class, () -> locator.find(empty, "k"));
  }

  public void testAllHostsInactivePoolThrowsNoAvailableHostException ()
    throws Exception {

    ServerPool pool = poolOf("a", "b");
    MaglevKeyLocator locator = new MaglevKeyLocator(10);

    locator.installRouting(pool);

    pool.get("a").setActive(false);
    pool.get("b").setActive(false);
    locator.updateRouting(pool);

    Assert.assertThrows(NoAvailableHostException.class, () -> locator.find(pool, "k"));
  }
}
