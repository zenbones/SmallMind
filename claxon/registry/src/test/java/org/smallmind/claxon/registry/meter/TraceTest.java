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
public class TraceTest {

  public void testRecordEmitsOneQuantityPerWindowInConstructionOrder () {

    Window m1 = new Window("m1", 1);
    Window m5 = new Window("m5", 5);
    Window m15 = new Window("m15", 15);
    Trace trace = new Trace(new FakeClock(), TimeUnit.MINUTES, m1, m5, m15);

    Quantity[] quantities = trace.record();

    Assert.assertEquals(quantities.length, 3);
    Assert.assertEquals(quantities[0].getName(), "m1");
    Assert.assertEquals(quantities[1].getName(), "m5");
    Assert.assertEquals(quantities[2].getName(), "m15");
  }

  public void testEmptyTraceProducesZeroAverages () {

    Trace trace = new Trace(new FakeClock(), TimeUnit.MINUTES, new Window("m1", 1));

    Assert.assertEquals(trace.record()[0].getValue(), 0.0);
  }

  public void testMovingAverageIsPreservedAcrossReadsWithoutNewUpdates () {

    Trace trace = new Trace(new FakeClock(), TimeUnit.MINUTES, new Window("m1", 1));

    trace.update(10);

    double firstRead = trace.record()[0].getValue();
    double secondRead = trace.record()[0].getValue();
    double thirdRead = trace.record()[0].getValue();

    Assert.assertEquals(firstRead, 10.0);
    Assert.assertEquals(secondRead, 10.0);
    Assert.assertEquals(thirdRead, 10.0);
  }

  public void testCustomWindowNamesPropagateToQuantities () {

    Trace trace = new Trace(new FakeClock(), TimeUnit.MINUTES, new Window("short", 1), new Window("long", 60));

    Quantity[] quantities = trace.record();

    Assert.assertEquals(quantities[0].getName(), "short");
    Assert.assertEquals(quantities[1].getName(), "long");
  }

  public void testBuilderDefaultWindowsAreOneFiveAndFifteen () {

    Trace trace = new TraceBuilder().build(new FakeClock());

    Quantity[] quantities = trace.record();

    Assert.assertEquals(quantities.length, 3);
    Assert.assertEquals(quantities[0].getName(), "m1");
    Assert.assertEquals(quantities[1].getName(), "m5");
    Assert.assertEquals(quantities[2].getName(), "m15");
  }
}
