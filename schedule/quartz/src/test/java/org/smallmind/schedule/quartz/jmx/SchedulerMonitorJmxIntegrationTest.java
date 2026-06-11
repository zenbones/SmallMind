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

import java.lang.management.ManagementFactory;
import java.util.Properties;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Registers a {@link SchedulerMonitor} with the platform {@link MBeanServer} and drives its
 * lifecycle operations through a JMX {@code MXBean} proxy, validating the management surface
 * end-to-end against a real (in-memory {@code RAMJobStore}) scheduler.
 */
@Test(groups = "integration")
public class SchedulerMonitorJmxIntegrationTest {

  private Scheduler standbyScheduler (String instanceName)
    throws SchedulerException {

    Properties properties = new Properties();

    properties.setProperty("org.quartz.scheduler.instanceName", instanceName);
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "1");
    properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

    return new StdSchedulerFactory(properties).getScheduler();
  }

  public void testSchedulerLifecycleDrivenOverJmx ()
    throws Exception {

    Scheduler scheduler = standbyScheduler("jmxScheduler");
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName("org.smallmind.schedule.quartz.test:type=Scheduler,name=jmxIntegration");

    mBeanServer.registerMBean(new SchedulerMonitor(scheduler), objectName);

    try {
      SchedulerMXBean monitor = JMX.newMXBeanProxy(mBeanServer, objectName, SchedulerMXBean.class);

      // A scheduler obtained but never started sits in standby.
      Assert.assertEquals(monitor.status(), SchedulerStatus.STANDBY);

      monitor.start();
      Assert.assertEquals(monitor.status(), SchedulerStatus.STARTED);

      monitor.standby();
      Assert.assertEquals(monitor.status(), SchedulerStatus.STANDBY);
    } finally {
      mBeanServer.unregisterMBean(objectName);
      scheduler.shutdown();
    }
  }
}
