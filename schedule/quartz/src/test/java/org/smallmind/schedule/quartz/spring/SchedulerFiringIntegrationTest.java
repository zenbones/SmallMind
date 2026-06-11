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

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.listeners.SchedulerListenerSupport;
import org.smallmind.schedule.quartz.QuartzProxyJob;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * End-to-end exercise of the Spring-to-Quartz firing path against an in-memory {@code RAMJobStore}
 * scheduler (no database, no Docker). These tests drive {@link SpringJobFactory#newJob} as Quartz
 * invokes it on a real worker thread, which mocked unit tests cannot reach.
 */
@Test(groups = "integration")
public class SchedulerFiringIntegrationTest {

  /**
   * Records each firing into a shared latch so the test thread can observe that the job actually
   * executed. A new prototype instance is created per firing, so the latch is held statically
   * rather than on the instance.
   */
  public static class CountingJob extends QuartzProxyJob {

    static final AtomicReference<CountDownLatch> LATCH = new AtomicReference<>();

    @Override
    public boolean logOnZeroCount () {

      return false;
    }

    @Override
    public void proceed () {

      CountDownLatch latch;

      incCount();
      if ((latch = LATCH.get()) != null) {
        latch.countDown();
      }
    }

    @Override
    public void cleanup () {
    }
  }

  private Properties ramProperties (String instanceName) {

    Properties properties = new Properties();

    properties.setProperty("org.quartz.scheduler.instanceName", instanceName);
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "1");
    properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

    return properties;
  }

  private String collectMessages (Throwable throwable) {

    StringBuilder builder = new StringBuilder();

    for (Throwable current = throwable; current != null; current = current.getCause()) {
      builder.append(current.getMessage()).append(' ');
      if (current.getCause() == current) {
        break;
      }
    }

    return builder.toString();
  }

  public void testPrototypeJobFiresThroughSpringJobFactory ()
    throws Exception {

    GenericApplicationContext applicationContext = new GenericApplicationContext();

    applicationContext.registerBean("scheduled.countingJob", CountingJob.class, beanDefinition -> beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE));
    applicationContext.refresh();

    CountDownLatch latch = new CountDownLatch(1);

    CountingJob.LATCH.set(latch);

    SpringSchedulerFactory factory = new SpringSchedulerFactory(ramProperties("firingScheduler"));

    factory.setApplicationContext(applicationContext);

    Scheduler scheduler = factory.getScheduler();

    try {
      JobDetail jobDetail = JobBuilder.newJob(CountingJob.class).withIdentity("countingJob", "scheduled").storeDurably().build();
      Trigger trigger = TriggerBuilder.newTrigger().withIdentity("countingTrigger", "scheduled").startNow().build();

      scheduler.scheduleJob(jobDetail, trigger);

      Assert.assertTrue(latch.await(10, TimeUnit.SECONDS), "the scheduled job did not fire within the timeout");
    } finally {
      scheduler.shutdown(true);
      applicationContext.close();
      CountingJob.LATCH.set(null);
    }
  }

  public void testSingletonJobBeanIsRejectedAtFiringTime ()
    throws Exception {

    GenericApplicationContext applicationContext = new GenericApplicationContext();

    // Registered with the default singleton scope; the prototype violation only surfaces when the
    // trigger actually fires and SpringJobFactory.newJob is invoked.
    applicationContext.registerBean("scheduled.singletonJob", CountingJob.class);
    applicationContext.refresh();

    SpringSchedulerFactory factory = new SpringSchedulerFactory(ramProperties("singletonScheduler"));

    factory.setApplicationContext(applicationContext);

    Scheduler scheduler = factory.getScheduler();

    CountDownLatch errorLatch = new CountDownLatch(1);
    AtomicReference<SchedulerException> errorRef = new AtomicReference<>();

    scheduler.getListenerManager().addSchedulerListener(new SchedulerListenerSupport() {

      @Override
      public void schedulerError (String message, SchedulerException cause) {

        errorRef.set(cause);
        errorLatch.countDown();
      }
    });

    try {
      JobDetail jobDetail = JobBuilder.newJob(CountingJob.class).withIdentity("singletonJob", "scheduled").storeDurably().build();
      Trigger trigger = TriggerBuilder.newTrigger().withIdentity("singletonTrigger", "scheduled").startNow().build();

      scheduler.scheduleJob(jobDetail, trigger);

      Assert.assertTrue(errorLatch.await(10, TimeUnit.SECONDS), "the scheduler did not report an instantiation error");

      String message = collectMessages(errorRef.get());

      Assert.assertTrue(message.contains("not a prototype"), "unexpected scheduler error: " + message);
    } finally {
      scheduler.shutdown(true);
      applicationContext.close();
    }
  }
}
