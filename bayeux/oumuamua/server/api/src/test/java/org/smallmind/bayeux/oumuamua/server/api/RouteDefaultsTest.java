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
package org.smallmind.bayeux.oumuamua.server.api;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link Route#isDeliverable()}'s default implementation across the full
 * matrix of wild / deep-wild / meta / service predicate combinations.
 */
@Test(groups = "unit")
public class RouteDefaultsTest {

  public void testIsDeliverableTrueWhenNothingFlagged () {

    Assert.assertTrue(new StubRoute(false, false, false, false).isDeliverable());
  }

  public void testIsDeliverableFalseWhenWild () {

    Assert.assertFalse(new StubRoute(true, false, false, false).isDeliverable());
  }

  public void testIsDeliverableFalseWhenDeepWild () {

    Assert.assertFalse(new StubRoute(false, true, false, false).isDeliverable());
  }

  public void testIsDeliverableFalseWhenMeta () {

    Assert.assertFalse(new StubRoute(false, false, true, false).isDeliverable());
  }

  public void testIsDeliverableFalseWhenService () {

    Assert.assertFalse(new StubRoute(false, false, false, true).isDeliverable());
  }

  public void testIsDeliverableFalseWhenMultipleFlagsSet () {

    Assert.assertFalse(new StubRoute(true, false, true, false).isDeliverable());
  }

  private static class StubRoute implements Route {

    private final boolean wild;
    private final boolean deepWild;
    private final boolean meta;
    private final boolean service;

    private StubRoute (boolean wild, boolean deepWild, boolean meta, boolean service) {

      this.wild = wild;
      this.deepWild = deepWild;
      this.meta = meta;
      this.service = service;
    }

    @Override
    public String getPath () {

      return "/stub";
    }

    @Override
    public int size () {

      return 1;
    }

    @Override
    public int lastIndex () {

      return 0;
    }

    @Override
    public Segment getSegment (int index) {

      return null;
    }

    @Override
    public boolean isWild () {

      return wild;
    }

    @Override
    public boolean isDeepWild () {

      return deepWild;
    }

    @Override
    public boolean isMeta () {

      return meta;
    }

    @Override
    public boolean isService () {

      return service;
    }

    @Override
    public boolean matchesPrefix (String... segments) {

      return false;
    }
  }
}
