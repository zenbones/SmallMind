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
package org.smallmind.nutsnbolts.util;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DotNotationComparatorTest {

  public void testEqualPathsCompareEqual () {

    Assert.assertEquals(new DotNotationComparator().compare("a.b.c", "a.b.c"), 0);
  }

  public void testFirstDifferentSegmentDetermisOrder () {

    Assert.assertTrue(new DotNotationComparator().compare("a.b.c", "a.d.c") < 0);
    Assert.assertTrue(new DotNotationComparator().compare("a.z.c", "a.b.c") > 0);
  }

  public void testShorterPathSortsBeforeLongerWithCommonPrefix () {

    Assert.assertTrue(new DotNotationComparator().compare("a.b", "a.b.c") < 0);
    Assert.assertTrue(new DotNotationComparator().compare("a.b.c", "a.b") > 0);
  }

  public void testReversedMatchesFromTheRightmostSegmentFirst () {

    DotNotationComparator forward = new DotNotationComparator();
    DotNotationComparator reversed = new DotNotationComparator(true);

    Assert.assertTrue(forward.compare("z.com", "a.org") > 0);
    Assert.assertTrue(reversed.compare("z.com", "a.org") < 0);
  }

  public void testReversedFallsThroughToLeftWhenRightSegmentsAreEqual () {

    DotNotationComparator reversed = new DotNotationComparator(true);

    Assert.assertTrue(reversed.compare("a.com", "b.com") < 0);
  }
}
