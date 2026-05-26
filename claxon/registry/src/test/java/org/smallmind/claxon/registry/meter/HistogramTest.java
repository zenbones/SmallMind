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
package org.smallmind.claxon.registry.meter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.FakeClock;
import org.smallmind.claxon.registry.Percentile;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.QuantityType;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class HistogramTest {

  private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

  private static Map<String, Quantity> byName (Quantity[] quantities) {

    Map<String, Quantity> map = new HashMap<>();

    for (Quantity quantity : quantities) {
      map.put(quantity.getName(), quantity);
    }

    return map;
  }

  private static Histogram defaultHistogram (FakeClock clock, Percentile... percentiles) {

    return new Histogram(clock, 1L, 3_600_000L, 2, new Stint(1, TimeUnit.SECONDS), percentiles);
  }

  public void testRecordEmitsBaseQuantitiesPlusEachConfiguredPercentile () {

    FakeClock clock = new FakeClock();
    Histogram histogram = defaultHistogram(clock, new Percentile("p50", 50.0), new Percentile("p99", 99.0));

    histogram.update(10);
    histogram.update(20);
    histogram.update(30);
    clock.advanceNanos(NANOS_PER_SECOND);

    Map<String, Quantity> byName = byName(histogram.record());

    Assert.assertEquals(byName.size(), 7);
    Assert.assertNotNull(byName.get("count"));
    Assert.assertNotNull(byName.get("rate"));
    Assert.assertNotNull(byName.get("minimum"));
    Assert.assertNotNull(byName.get("maximum"));
    Assert.assertNotNull(byName.get("mean"));
    Assert.assertNotNull(byName.get("p50"));
    Assert.assertNotNull(byName.get("p99"));
  }

  public void testCountIsTypedAsCount () {

    FakeClock clock = new FakeClock();
    Histogram histogram = defaultHistogram(clock);

    histogram.update(1);
    clock.advanceNanos(NANOS_PER_SECOND);

    Assert.assertEquals(byName(histogram.record()).get("count").getType(), QuantityType.COUNT);
  }

  public void testNonCountQuantitiesUseNoneType () {

    FakeClock clock = new FakeClock();
    Histogram histogram = defaultHistogram(clock, new Percentile("p99", 99.0));

    histogram.update(1);
    clock.advanceNanos(NANOS_PER_SECOND);

    Map<String, Quantity> byName = byName(histogram.record());

    Assert.assertEquals(byName.get("rate").getType(), QuantityType.NONE);
    Assert.assertEquals(byName.get("minimum").getType(), QuantityType.NONE);
    Assert.assertEquals(byName.get("maximum").getType(), QuantityType.NONE);
    Assert.assertEquals(byName.get("mean").getType(), QuantityType.NONE);
    Assert.assertEquals(byName.get("p99").getType(), QuantityType.NONE);
  }

  public void testCountTracksTotalNumberOfUpdates () {

    FakeClock clock = new FakeClock();
    Histogram histogram = defaultHistogram(clock);

    for (int i = 0; i < 100; i++) {
      histogram.update(i + 1);
    }
    clock.advanceNanos(NANOS_PER_SECOND);

    Assert.assertEquals(byName(histogram.record()).get("count").getValue(), 100.0);
  }

  public void testNoConfiguredPercentilesYieldsOnlyFiveBaseQuantities () {

    FakeClock clock = new FakeClock();
    Histogram histogram = defaultHistogram(clock);

    histogram.update(1);
    clock.advanceNanos(NANOS_PER_SECOND);

    Assert.assertEquals(histogram.record().length, 5);
  }

  public void testPercentilesAppearedInConstructionOrderAfterBaseQuantities () {

    FakeClock clock = new FakeClock();
    Histogram histogram = defaultHistogram(clock, new Percentile("p50", 50.0), new Percentile("p99", 99.0));

    histogram.update(1);
    clock.advanceNanos(NANOS_PER_SECOND);

    Quantity[] quantities = histogram.record();

    Assert.assertEquals(quantities[5].getName(), "p50");
    Assert.assertEquals(quantities[6].getName(), "p99");
  }

  public void testBuilderDefaultPercentileSet () {

    FakeClock clock = new FakeClock();
    Histogram histogram = new HistogramBuilder().build(clock);

    histogram.update(1);
    clock.advanceNanos(NANOS_PER_SECOND);

    Map<String, Quantity> byName = byName(histogram.record());

    Assert.assertNotNull(byName.get("p75"));
    Assert.assertNotNull(byName.get("p95"));
    Assert.assertNotNull(byName.get("p98"));
    Assert.assertNotNull(byName.get("p99"));
    Assert.assertNotNull(byName.get("p999"));
  }
}
