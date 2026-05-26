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
package org.smallmind.claxon.registry.json;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.FakeClock;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.meter.Histogram;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class HistogramParserTest {

  private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

  private static Set<String> namesOf (Quantity[] quantities) {

    Set<String> names = new HashSet<>();

    for (Quantity quantity : quantities) {
      names.add(quantity.getName());
    }

    return names;
  }

  private static Set<String> recordWith (String json, long updateValue)
    throws Throwable {

    FakeClock clock = new FakeClock();
    Histogram histogram = new HistogramParser().parse(json).build(clock);

    histogram.update(updateValue);
    clock.advanceNanos(NANOS_PER_SECOND);

    return namesOf(histogram.record());
  }

  public void testEmptyJsonProducesBuilderDefaults ()
    throws Throwable {

    Set<String> names = recordWith("{}", 1);

    Assert.assertTrue(names.contains("count"));
    Assert.assertTrue(names.contains("rate"));
    Assert.assertTrue(names.contains("minimum"));
    Assert.assertTrue(names.contains("maximum"));
    Assert.assertTrue(names.contains("mean"));
    Assert.assertTrue(names.contains("p75"));
    Assert.assertTrue(names.contains("p95"));
    Assert.assertTrue(names.contains("p98"));
    Assert.assertTrue(names.contains("p99"));
    Assert.assertTrue(names.contains("p999"));
  }

  public void testJsonPercentilesReplaceDefaults ()
    throws Throwable {

    Set<String> names = recordWith("{\"percentiles\":[{\"name\":\"p50\",\"value\":50.0},{\"name\":\"p999\",\"value\":99.9}]}", 1);

    Assert.assertTrue(names.contains("p50"));
    Assert.assertTrue(names.contains("p999"));
    Assert.assertFalse(names.contains("p75"));
    Assert.assertFalse(names.contains("p95"));
    Assert.assertFalse(names.contains("p98"));
    Assert.assertFalse(names.contains("p99"));
  }

  public void testJsonEmptyPercentilesArraySuppressesAllPercentiles ()
    throws Throwable {

    Set<String> names = recordWith("{\"percentiles\":[]}", 1);

    Assert.assertEquals(names.size(), 5);
    Assert.assertTrue(names.contains("count"));
    Assert.assertTrue(names.contains("rate"));
    Assert.assertTrue(names.contains("minimum"));
    Assert.assertTrue(names.contains("maximum"));
    Assert.assertTrue(names.contains("mean"));
  }

  public void testJsonHighestTrackableValueExpandsRange ()
    throws Throwable {

    FakeClock clock = new FakeClock();
    Histogram histogram = new HistogramParser()
                            .parse("{\"highestTrackableValue\":10000000000,\"numberOfSignificantValueDigits\":2}")
                            .build(clock);

    histogram.update(5_000_000_000L);
    clock.advanceNanos(NANOS_PER_SECOND);

    for (Quantity quantity : histogram.record()) {
      if ("count".equals(quantity.getName())) {
        Assert.assertEquals(quantity.getValue(), 1.0);
      }
    }
  }

  public void testJsonResolutionStintIsHonored ()
    throws Throwable {

    FakeClock clock = new FakeClock();
    Histogram histogramFromJson = new HistogramParser()
                                    .parse("{\"resolutionStint\":{\"time\":2,\"timeUnit\":\"SECONDS\"}}")
                                    .build(clock);
    Histogram histogramDefault = new HistogramParser().parse("{}").build(clock);

    histogramFromJson.update(10);
    histogramDefault.update(10);
    clock.advanceNanos(NANOS_PER_SECOND);

    double customRate = 0;
    double defaultRate = 0;

    for (Quantity quantity : histogramFromJson.record()) {
      if ("rate".equals(quantity.getName())) {
        customRate = quantity.getValue();
      }
    }
    for (Quantity quantity : histogramDefault.record()) {
      if ("rate".equals(quantity.getName())) {
        defaultRate = quantity.getValue();
      }
    }

    Assert.assertEquals(customRate / defaultRate, 2.0, 0.0001);
  }
}
