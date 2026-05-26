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
import org.smallmind.claxon.registry.Quantity;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class GaugeTest {

  private static Map<String, Quantity> byName (Quantity[] quantities) {

    Map<String, Quantity> map = new HashMap<>();

    for (Quantity quantity : quantities) {
      map.put(quantity.getName(), quantity);
    }

    return map;
  }

  public void testEmptyGaugeOmitsMinimumAndMaximum () {

    Quantity[] quantities = new Gauge().record();

    Assert.assertEquals(quantities.length, 1);
    Assert.assertEquals(quantities[0].getName(), "average");
    Assert.assertTrue(Double.isNaN(quantities[0].getValue()));
  }

  public void testRecordEmitsMinimumMaximumAndAverage () {

    Gauge gauge = new Gauge();

    gauge.update(2);
    gauge.update(8);
    gauge.update(5);

    Map<String, Quantity> byName = byName(gauge.record());

    Assert.assertEquals(byName.size(), 3);
    Assert.assertEquals(byName.get("minimum").getValue(), 2.0);
    Assert.assertEquals(byName.get("maximum").getValue(), 8.0);
    Assert.assertEquals(byName.get("average").getValue(), 5.0);
  }

  public void testRecordResetsAggregateState () {

    Gauge gauge = new Gauge();

    gauge.update(10);
    gauge.record();

    Quantity[] quantities = gauge.record();

    Assert.assertEquals(quantities.length, 1);
    Assert.assertEquals(quantities[0].getName(), "average");
    Assert.assertTrue(Double.isNaN(quantities[0].getValue()));
  }
}
