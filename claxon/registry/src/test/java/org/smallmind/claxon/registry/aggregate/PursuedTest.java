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
public class PursuedTest {

  public void testEmptyAverageIsZeroForEveryWindow () {

    Pursued pursued = new Pursued(new FakeClock(), TimeUnit.MINUTES, 1, 5, 15);

    double[] averages = pursued.getMovingAverages();

    Assert.assertEquals(averages.length, 3);
    Assert.assertEquals(averages[0], 0.0);
    Assert.assertEquals(averages[1], 0.0);
    Assert.assertEquals(averages[2], 0.0);
  }

  public void testUpdateFansOutToEveryWindow () {

    Pursued pursued = new Pursued(new FakeClock(), TimeUnit.MINUTES, 1, 5, 15);

    pursued.update(10);

    double[] averages = pursued.getMovingAverages();

    Assert.assertEquals(averages[0], 10.0);
    Assert.assertEquals(averages[1], 10.0);
    Assert.assertEquals(averages[2], 10.0);
  }

  public void testReturnedArrayLengthMatchesConfiguredWindowCount () {

    Pursued pursued = new Pursued(new FakeClock(), TimeUnit.SECONDS, 1, 2, 3, 4, 5);

    Assert.assertEquals(pursued.getMovingAverages().length, 5);
  }

  public void testShorterWindowDecaysFasterThanLongerWindow () {

    FakeClock clock = new FakeClock(0L, 1L);
    Pursued pursued = new Pursued(clock, TimeUnit.SECONDS, 1, 60);

    pursued.update(0);
    clock.advanceNanos(TimeUnit.SECONDS.toNanos(1));
    pursued.update(100);

    double[] averages = pursued.getMovingAverages();

    Assert.assertTrue(averages[0] > averages[1]);
  }

  public void testDefaultConstructorTracksThreeOneMinuteFiveMinuteFifteenMinuteWindows () {

    Pursued pursued = new Pursued(new FakeClock());

    pursued.update(42);

    double[] averages = pursued.getMovingAverages();

    Assert.assertEquals(averages.length, 3);
    Assert.assertEquals(averages[0], 42.0);
    Assert.assertEquals(averages[1], 42.0);
    Assert.assertEquals(averages[2], 42.0);
  }
}
