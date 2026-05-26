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

import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.FakeClock;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.QuantityType;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TachometerTest {

  private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

  public void testRecordEmitsCountAndRate () {

    FakeClock clock = new FakeClock();
    Tachometer tachometer = new Tachometer(clock, new Stint(1, TimeUnit.SECONDS));

    tachometer.update(0);
    tachometer.update(0);
    tachometer.update(0);
    clock.advanceNanos(NANOS_PER_SECOND);

    Quantity[] quantities = tachometer.record();

    Assert.assertEquals(quantities.length, 2);
    Assert.assertEquals(quantities[0].getName(), "count");
    Assert.assertEquals(quantities[0].getType(), QuantityType.COUNT);
    Assert.assertEquals(quantities[0].getValue(), 3.0);
    Assert.assertEquals(quantities[1].getName(), "rate");
    Assert.assertEquals(quantities[1].getValue(), 3.0);
  }

  public void testUpdateValueIsIgnored () {

    FakeClock clock = new FakeClock();
    Tachometer tachometer = new Tachometer(clock, new Stint(1, TimeUnit.SECONDS));

    tachometer.update(1_000_000);
    tachometer.update(-50);
    clock.advanceNanos(NANOS_PER_SECOND);

    Assert.assertEquals(tachometer.record()[0].getValue(), 2.0);
  }
}
