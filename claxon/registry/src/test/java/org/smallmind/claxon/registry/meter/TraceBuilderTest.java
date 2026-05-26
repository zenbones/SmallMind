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
import org.smallmind.claxon.registry.Window;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TraceBuilderTest {

  public void testCustomWindowsReplaceDefaultsInRecordedQuantities () {

    Trace trace = new TraceBuilder().windows(new Window[] {new Window("short", 1), new Window("long", 60)}).build(new FakeClock());

    Quantity[] quantities = trace.record();

    Assert.assertEquals(quantities.length, 2);
    Assert.assertEquals(quantities[0].getName(), "short");
    Assert.assertEquals(quantities[1].getName(), "long");
  }

  public void testWindowTimeUnitSetterIsHonoredBySwitchingToFasterUnit () {

    FakeClock clock = new FakeClock(0L, 1L);
    Trace minutesTrace = new TraceBuilder().build(clock);
    Trace secondsTrace = new TraceBuilder().windowTimeUnit(TimeUnit.SECONDS).build(clock);

    minutesTrace.update(0);
    secondsTrace.update(0);
    clock.advanceNanos(TimeUnit.SECONDS.toNanos(1));
    minutesTrace.update(100);
    secondsTrace.update(100);

    double minutesM1 = minutesTrace.record()[0].getValue();
    double secondsM1 = secondsTrace.record()[0].getValue();

    Assert.assertTrue(secondsM1 > minutesM1);
  }
}
