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
package org.smallmind.nutsnbolts.time;

import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class StintTest {

  public void testConstructorStoresTimeAndUnit () {

    Stint stint = new Stint(5, TimeUnit.SECONDS);

    Assert.assertEquals(stint.getTime(), 5);
    Assert.assertEquals(stint.getTimeUnit(), TimeUnit.SECONDS);
  }

  public void testStaticOfMatchesConstructor () {

    Stint stint = Stint.of(10, TimeUnit.MINUTES);

    Assert.assertEquals(stint.getTime(), 10);
    Assert.assertEquals(stint.getTimeUnit(), TimeUnit.MINUTES);
  }

  public void testNoneIsZeroSeconds () {

    Stint none = Stint.none();

    Assert.assertEquals(none.getTime(), 0);
    Assert.assertEquals(none.getTimeUnit(), TimeUnit.SECONDS);
  }

  public void testNoneIsSharedInstance () {

    Assert.assertSame(Stint.none(), Stint.none());
  }

  public void testToMillisecondsConvertsAcrossUnits () {

    Assert.assertEquals(new Stint(5, TimeUnit.SECONDS).toMilliseconds(), 5000L);
    Assert.assertEquals(new Stint(2, TimeUnit.MINUTES).toMilliseconds(), 120000L);
    Assert.assertEquals(new Stint(500, TimeUnit.MILLISECONDS).toMilliseconds(), 500L);
  }

  public void testZeroTimeIsPermitted () {

    Stint zero = new Stint(0, TimeUnit.NANOSECONDS);

    Assert.assertEquals(zero.getTime(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeTimeIsRejected () {

    new Stint(-1, TimeUnit.SECONDS);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullTimeUnitIsRejected () {

    new Stint(5, null);
  }
}
