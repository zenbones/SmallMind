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
package org.smallmind.schedule.quartz.spring;

import java.io.IOException;
import java.util.Properties;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.quartz.impl.SchedulerRepository;
import org.springframework.context.ApplicationContext;
import org.smallmind.nutsnbolts.util.OnOrOff;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SpringSchedulerFactoryTest {

  /**
   * Backs the factory with an in-memory {@code RAMJobStore} so {@code super.getScheduler()}
   * yields a real, fully started scheduler without a database or external
   * resources. Each test uses a distinct instance name because Quartz caches
   * schedulers by name in a process-wide repository.
   */
  private Properties ramProperties (String instanceName) {

    Properties properties = new Properties();

    properties.setProperty("org.quartz.scheduler.instanceName", instanceName);
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "1");
    properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

    return properties;
  }

  public void testActiveModeOnStartsScheduler ()
    throws Exception {

    SpringSchedulerFactory factory = new SpringSchedulerFactory(ramProperties("activeOnScheduler"));

    factory.setApplicationContext(Mockito.mock(ConfigurableApplicationContext.class));

    Scheduler scheduler = factory.getScheduler();

    try {
      Assert.assertTrue(scheduler.isStarted());
      Assert.assertFalse(scheduler.isInStandbyMode());
    } finally {
      scheduler.shutdown();
    }
  }

  public void testActiveModeOffPlacesSchedulerInStandby ()
    throws Exception {

    SpringSchedulerFactory factory = new SpringSchedulerFactory(ramProperties("standbyScheduler"));

    factory.setActiveMode(OnOrOff.OFF);
    factory.setApplicationContext(Mockito.mock(ConfigurableApplicationContext.class));

    Scheduler scheduler = factory.getScheduler();

    try {
      Assert.assertTrue(scheduler.isInStandbyMode());
      Assert.assertFalse(scheduler.isStarted());
    } finally {
      scheduler.shutdown();
    }
  }

  public void testContextCloseShutsDownSchedulerAndClosesContext ()
    throws Exception {

    ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);
    SpringSchedulerFactory factory = new SpringSchedulerFactory(ramProperties("closingScheduler"));

    factory.setApplicationContext(applicationContext);

    Scheduler scheduler = factory.getScheduler();

    factory.onApplicationEvent(new ContextClosedEvent(applicationContext));

    Assert.assertTrue(scheduler.isShutdown());
    Mockito.verify(applicationContext).close();
  }

  public void testContextCloseSwallowsFactoryCloseError ()
    throws Exception {

    ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);

    // doAnswer (rather than doThrow) is required to raise a checked IOException from close(), which
    // ConfigurableApplicationContext does not declare; the ingester's Closeable cast call site does.
    Mockito.doAnswer(invocation -> {
      throw new IOException("close failed");
    }).when(applicationContext).close();

    SpringSchedulerFactory factory = new SpringSchedulerFactory(ramProperties("closeErrorScheduler"));

    factory.setApplicationContext(applicationContext);

    Scheduler scheduler = factory.getScheduler();

    // The factory-close failure must be swallowed: the scheduler still shuts down and no exception
    // escapes onApplicationEvent.
    factory.onApplicationEvent(new ContextClosedEvent(applicationContext));

    Assert.assertTrue(scheduler.isShutdown());
    Mockito.verify(applicationContext).close();
  }

  public void testContextCloseWithoutApplicationContextSkipsFactoryClose ()
    throws Exception {

    SpringSchedulerFactory factory = new SpringSchedulerFactory(ramProperties("noContextScheduler"));

    // setApplicationContext is never called, so jobFactory stays null. onApplicationEvent must shut
    // the scheduler down (via super.getScheduler()) and skip the factory-close block without error.
    factory.onApplicationEvent(new ContextClosedEvent(Mockito.mock(ApplicationContext.class)));

    // A shut-down scheduler is removed from the StdSchedulerFactory repository, confirming the
    // shutdown path ran to completion past the null-jobFactory guard.
    Assert.assertNull(SchedulerRepository.getInstance().lookup("noContextScheduler"));
  }
}
