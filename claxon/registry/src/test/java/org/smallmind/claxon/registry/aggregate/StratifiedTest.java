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
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class StratifiedTest {

  private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

  public void testEmptyIntervalSnapshotHasZeroCount () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock, new Stint(1, TimeUnit.SECONDS));

    clock.advanceNanos(NANOS_PER_SECOND);

    HistogramTime snapshot = stratified.get();

    Assert.assertEquals(snapshot.getHistogram().getTotalCount(), 0L);
  }

  public void testRecordedValuesAppearInNextSnapshot () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock, new Stint(1, TimeUnit.SECONDS));

    stratified.update(10);
    stratified.update(20);
    stratified.update(30);
    clock.advanceNanos(NANOS_PER_SECOND);

    HistogramTime snapshot = stratified.get();

    Assert.assertEquals(snapshot.getHistogram().getTotalCount(), 3L);
    Assert.assertEquals(snapshot.getHistogram().getMinValue(), 10L);
    Assert.assertEquals(snapshot.getHistogram().getMaxValue(), 30L);
  }

  public void testSnapshotIntervalResetsBetweenGets () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock, new Stint(1, TimeUnit.SECONDS));

    stratified.update(42);
    clock.advanceNanos(NANOS_PER_SECOND);
    stratified.get();

    clock.advanceNanos(NANOS_PER_SECOND);

    Assert.assertEquals(stratified.get().getHistogram().getTotalCount(), 0L);
  }

  public void testTimeFactorMatchesWindowOverElapsed () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock, new Stint(1, TimeUnit.SECONDS));

    clock.advanceNanos(NANOS_PER_SECOND);

    Assert.assertEquals(stratified.get().getTimeFactor(), 1.0);
  }

  public void testTimeFactorShrinksWhenElapsedExceedsWindow () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock, new Stint(1, TimeUnit.SECONDS));

    clock.advanceNanos(2 * NANOS_PER_SECOND);

    Assert.assertEquals(stratified.get().getTimeFactor(), 0.5);
  }

  public void testTimeFactorGrowsWhenElapsedBelowWindow () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock, new Stint(1, TimeUnit.SECONDS));

    clock.advanceNanos(NANOS_PER_SECOND / 2);

    Assert.assertEquals(stratified.get().getTimeFactor(), 2.0);
  }

  public void testDoubledWindowDoublesTimeFactorForSameElapsed () {

    FakeClock oneSecondClock = new FakeClock();
    FakeClock twoSecondClock = new FakeClock();
    Stratified oneSecondWindow = new Stratified(oneSecondClock, new Stint(1, TimeUnit.SECONDS));
    Stratified twoSecondWindow = new Stratified(twoSecondClock, new Stint(2, TimeUnit.SECONDS));

    oneSecondClock.advanceNanos(NANOS_PER_SECOND);
    twoSecondClock.advanceNanos(NANOS_PER_SECOND);

    Assert.assertEquals(twoSecondWindow.get().getTimeFactor() / oneSecondWindow.get().getTimeFactor(), 2.0);
  }

  public void testDefaultConstructorRecordsAndSnapshots () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock);

    stratified.update(100);
    clock.advanceNanos(NANOS_PER_SECOND);

    HistogramTime snapshot = stratified.get();

    Assert.assertEquals(snapshot.getHistogram().getTotalCount(), 1L);
    Assert.assertEquals(snapshot.getHistogram().getMinValue(), 100L);
  }

  public void testCustomBoundsConstructorRecordsAndSnapshots () {

    FakeClock clock = new FakeClock();
    Stratified stratified = new Stratified(clock, 1, 1000, 1);

    stratified.update(50);
    clock.advanceNanos(NANOS_PER_SECOND);

    HistogramTime snapshot = stratified.get();

    Assert.assertEquals(snapshot.getHistogram().getTotalCount(), 1L);
    Assert.assertEquals(snapshot.getHistogram().getMinValue(), 50L);
  }
}
