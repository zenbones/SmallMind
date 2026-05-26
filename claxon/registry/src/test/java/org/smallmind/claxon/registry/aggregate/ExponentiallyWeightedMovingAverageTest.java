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

import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.FakeClock;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ExponentiallyWeightedMovingAverageTest {

  public void testEmptyAverageIsZero () {

    Assert.assertEquals(new ExponentiallyWeightedMovingAverage(new FakeClock(), 1, TimeUnit.MINUTES).getMovingAverage(), 0.0);
  }

  public void testFirstUpdateSeedsAverageWithThatValue () {

    ExponentiallyWeightedMovingAverage ewma = new ExponentiallyWeightedMovingAverage(new FakeClock(), 1, TimeUnit.MINUTES);

    ewma.update(7);

    Assert.assertEquals(ewma.getMovingAverage(), 7.0);
  }

  public void testReadingDoesNotResetAverage () {

    ExponentiallyWeightedMovingAverage ewma = new ExponentiallyWeightedMovingAverage(new FakeClock(), 1, TimeUnit.MINUTES);

    ewma.update(42);

    double firstRead = ewma.getMovingAverage();
    double secondRead = ewma.getMovingAverage();
    double thirdRead = ewma.getMovingAverage();

    Assert.assertEquals(firstRead, 42.0);
    Assert.assertEquals(secondRead, 42.0);
    Assert.assertEquals(thirdRead, 42.0);
  }

  public void testSecondUpdateMovesAverageTowardNewValue () {

    FakeClock clock = new FakeClock(0L, 1L);
    ExponentiallyWeightedMovingAverage ewma = new ExponentiallyWeightedMovingAverage(clock, 1, TimeUnit.MINUTES);

    ewma.update(0);

    Assert.assertEquals(ewma.getMovingAverage(), 0.0);

    clock.advanceNanos(TimeUnit.SECONDS.toNanos(1));
    ewma.update(60);

    double after = ewma.getMovingAverage();

    Assert.assertTrue(after > 0.0);
    Assert.assertTrue(after < 60.0);
  }

  public void testLargeElapsedRelativeToWindowConvergesQuicklyTowardNewValue () {

    FakeClock clock = new FakeClock(0L, 1L);
    ExponentiallyWeightedMovingAverage ewma = new ExponentiallyWeightedMovingAverage(clock, 1, TimeUnit.SECONDS);

    ewma.update(0);
    clock.advanceNanos(TimeUnit.SECONDS.toNanos(100));
    ewma.update(100);

    Assert.assertEquals(ewma.getMovingAverage(), 100.0, 0.001);
  }

  public void testSmallElapsedRelativeToWindowResistsMovement () {

    FakeClock clock = new FakeClock(0L, 1L);
    ExponentiallyWeightedMovingAverage ewma = new ExponentiallyWeightedMovingAverage(clock, 1, TimeUnit.HOURS);

    ewma.update(0);
    clock.advanceNanos(1);
    ewma.update(100);

    Assert.assertTrue(ewma.getMovingAverage() < 0.001);
  }
}
