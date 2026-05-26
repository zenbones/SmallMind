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
package org.smallmind.claxon.registry.aggregate;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BoundedTest {

  public void testEmptyWindowMinimumIsSentinel () {

    Assert.assertEquals(new Bounded().getMinimum(), Long.MAX_VALUE);
  }

  public void testEmptyWindowMaximumIsSentinel () {

    Assert.assertEquals(new Bounded().getMaximum(), Long.MIN_VALUE);
  }

  public void testSingleUpdateReportsValueAsBothBounds () {

    Bounded bounded = new Bounded();

    bounded.update(7);

    Assert.assertEquals(bounded.getMinimum(), 7L);
    Assert.assertEquals(bounded.getMaximum(), 7L);
  }

  public void testTracksRunningMinAndMax () {

    Bounded bounded = new Bounded();

    bounded.update(5);
    bounded.update(-3);
    bounded.update(11);
    bounded.update(2);

    Assert.assertEquals(bounded.getMinimum(), -3L);
    Assert.assertEquals(bounded.getMaximum(), 11L);
  }

  public void testReadResetsWindow () {

    Bounded bounded = new Bounded();

    bounded.update(10);
    bounded.update(20);

    Assert.assertEquals(bounded.getMaximum(), 20L);
    Assert.assertEquals(bounded.getMinimum(), 10L);

    Assert.assertEquals(bounded.getMaximum(), Long.MIN_VALUE);
    Assert.assertEquals(bounded.getMinimum(), Long.MAX_VALUE);
  }

  public void testNewValuesAfterResetTrackedIndependently () {

    Bounded bounded = new Bounded();

    bounded.update(100);
    bounded.getMaximum();
    bounded.getMinimum();

    bounded.update(4);
    bounded.update(8);

    Assert.assertEquals(bounded.getMinimum(), 4L);
    Assert.assertEquals(bounded.getMaximum(), 8L);
  }
}
