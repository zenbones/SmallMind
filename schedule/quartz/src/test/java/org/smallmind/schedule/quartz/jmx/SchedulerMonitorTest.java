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
package org.smallmind.schedule.quartz.jmx;

import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SchedulerMonitorTest {

  public void testStartDelegatesToScheduler ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);

    new SchedulerMonitor(scheduler).start();

    Mockito.verify(scheduler).start();
  }

  public void testStandbyDelegatesToScheduler ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);

    new SchedulerMonitor(scheduler).standby();

    Mockito.verify(scheduler).standby();
  }

  public void testStandbyTakesPrecedenceOverStarted ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);

    Mockito.when(scheduler.isInStandbyMode()).thenReturn(true);
    Mockito.when(scheduler.isStarted()).thenReturn(true);

    Assert.assertEquals(new SchedulerMonitor(scheduler).status(), SchedulerStatus.STANDBY);
  }

  public void testStartedReportedWhenNotInStandby ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);

    Mockito.when(scheduler.isInStandbyMode()).thenReturn(false);
    Mockito.when(scheduler.isStarted()).thenReturn(true);

    Assert.assertEquals(new SchedulerMonitor(scheduler).status(), SchedulerStatus.STARTED);
  }

  public void testShutdownReportedWhenNeitherStandbyNorStarted ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);

    Mockito.when(scheduler.isInStandbyMode()).thenReturn(false);
    Mockito.when(scheduler.isStarted()).thenReturn(false);
    Mockito.when(scheduler.isShutdown()).thenReturn(true);

    Assert.assertEquals(new SchedulerMonitor(scheduler).status(), SchedulerStatus.SHUTDOWN);
  }

  public void testUnknownReportedWhenNoFlagMatches ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);

    Mockito.when(scheduler.isInStandbyMode()).thenReturn(false);
    Mockito.when(scheduler.isStarted()).thenReturn(false);
    Mockito.when(scheduler.isShutdown()).thenReturn(false);

    Assert.assertEquals(new SchedulerMonitor(scheduler).status(), SchedulerStatus.UNKNOWN);
  }
}
