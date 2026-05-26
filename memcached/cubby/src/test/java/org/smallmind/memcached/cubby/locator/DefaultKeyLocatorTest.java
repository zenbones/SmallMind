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

import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DefaultKeyLocatorTest {

  public void testFindUsesSortedNamesIndexedByPositiveModuloOfHashCode ()
    throws Exception {

    MemcachedHost alpha = new MemcachedHost("alpha", "host-a", 11211);
    MemcachedHost bravo = new MemcachedHost("bravo", "host-b", 11211);
    MemcachedHost charlie = new MemcachedHost("charlie", "host-c", 11211);
    ServerPool pool = new ServerPool(charlie, alpha, bravo);

    DefaultKeyLocator locator = new DefaultKeyLocator();
    locator.installRouting(pool);

    String[] sorted = {"alpha", "bravo", "charlie"};
    String[] probes = {"key-0", "key-1", "key-2", "key-7", "kappa"};

    for (String key : probes) {

      int index = key.hashCode() % sorted.length;

      if (index >= 0) {
        Assert.assertEquals(locator.find(pool, key).getName(), sorted[index], key);
      }
    }
  }

  public void testFindIsStableForSameKeyAcrossCalls ()
    throws Exception {

    ServerPool pool = new ServerPool(
                                      new MemcachedHost("a", "ha", 11211),
                                      new MemcachedHost("b", "hb", 11211),
                                      new MemcachedHost("c", "hc", 11211));

    DefaultKeyLocator locator = new DefaultKeyLocator();
    locator.installRouting(pool);

    String first = locator.find(pool, "kappa").getName();

    for (int loop = 0; loop < 5; loop++) {
      Assert.assertEquals(locator.find(pool, "kappa").getName(), first);
    }
  }

  public void testEmptyPoolThrowsNoAvailableHostException () {

    DefaultKeyLocator locator = new DefaultKeyLocator();
    ServerPool empty = new ServerPool();

    locator.installRouting(empty);

    Assert.assertThrows(NoAvailableHostException.class, () -> locator.find(empty, "k"));
  }

  public void testAllHostsInactivePoolThrowsNoAvailableHostException () {

    MemcachedHost alpha = new MemcachedHost("alpha", "ha", 11211);
    ServerPool pool = new ServerPool(alpha);

    pool.get("alpha").setActive(false);

    DefaultKeyLocator locator = new DefaultKeyLocator();
    locator.installRouting(pool);

    Assert.assertThrows(NoAvailableHostException.class, () -> locator.find(pool, "k"));
  }

  public void testInactiveHostIsExcludedFromRouting ()
    throws Exception {

    MemcachedHost alpha = new MemcachedHost("alpha", "ha", 11211);
    MemcachedHost bravo = new MemcachedHost("bravo", "hb", 11211);
    ServerPool pool = new ServerPool(alpha, bravo);

    DefaultKeyLocator locator = new DefaultKeyLocator();
    locator.installRouting(pool);

    pool.get("alpha").setActive(false);
    locator.updateRouting(pool);

    for (int loop = 0; loop < 25; loop++) {
      Assert.assertEquals(locator.find(pool, "key-" + loop).getName(), "bravo");
    }
  }
}
