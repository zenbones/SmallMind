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

import java.util.LinkedHashMap;
import java.util.Map;
import org.mockito.Mockito;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SpringJobFactoryTest {

  public static class DummyJob implements Job {

    @Override
    public void execute (JobExecutionContext context) {
    }
  }

  private TriggerFiredBundle bundleFor (JobKey jobKey) {

    JobDetail jobDetail = Mockito.mock(JobDetail.class);
    TriggerFiredBundle bundle = Mockito.mock(TriggerFiredBundle.class);

    Mockito.when(jobDetail.getKey()).thenReturn(jobKey);
    Mockito.doReturn(DummyJob.class).when(jobDetail).getJobClass();
    Mockito.when(bundle.getJobDetail()).thenReturn(jobDetail);

    return bundle;
  }

  public void testZeroBeansThrows () {

    ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);

    Mockito.when(applicationContext.getBeansOfType(DummyJob.class)).thenReturn(new LinkedHashMap<>());

    try {
      new SpringJobFactory(applicationContext).newJob(bundleFor(new JobKey("name", "group")), Mockito.mock(Scheduler.class));
      Assert.fail("expected FormattedSchedulerException");
    } catch (SchedulerException expected) {
      Assert.assertTrue(expected instanceof FormattedSchedulerException);
      Assert.assertTrue(expected.getMessage().contains("No job"));
    }
  }

  public void testSingleBeanPrototypeIsReturned ()
    throws Exception {

    ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
    DummyJob dummyJob = new DummyJob();
    Map<String, DummyJob> beanMap = new LinkedHashMap<>();

    beanMap.put("anyBeanName", dummyJob);
    Mockito.when(applicationContext.getBeansOfType(DummyJob.class)).thenReturn(beanMap);
    Mockito.when(applicationContext.isPrototype("anyBeanName")).thenReturn(true);

    Job resolved = new SpringJobFactory(applicationContext).newJob(bundleFor(new JobKey("name", "group")), Mockito.mock(Scheduler.class));

    Assert.assertSame(resolved, dummyJob);
  }

  public void testSingleBeanNotPrototypeThrows () {

    ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
    Map<String, DummyJob> beanMap = new LinkedHashMap<>();

    beanMap.put("anyBeanName", new DummyJob());
    Mockito.when(applicationContext.getBeansOfType(DummyJob.class)).thenReturn(beanMap);
    Mockito.when(applicationContext.isPrototype("anyBeanName")).thenReturn(false);

    try {
      new SpringJobFactory(applicationContext).newJob(bundleFor(new JobKey("name", "group")), Mockito.mock(Scheduler.class));
      Assert.fail("expected FormattedSchedulerException");
    } catch (SchedulerException expected) {
      Assert.assertTrue(expected instanceof FormattedSchedulerException);
      Assert.assertTrue(expected.getMessage().contains("not a prototype"));
    }
  }

  public void testMultipleBeansResolveByExactKeyMatch ()
    throws Exception {

    ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
    DummyJob matching = new DummyJob();
    Map<String, DummyJob> beanMap = new LinkedHashMap<>();

    beanMap.put("group.name", matching);
    beanMap.put("otherBean", new DummyJob());
    Mockito.when(applicationContext.getBeansOfType(DummyJob.class)).thenReturn(beanMap);
    Mockito.when(applicationContext.isPrototype("group.name")).thenReturn(true);

    Job resolved = new SpringJobFactory(applicationContext).newJob(bundleFor(new JobKey("name", "group")), Mockito.mock(Scheduler.class));

    Assert.assertSame(resolved, matching);
  }

  public void testMultipleBeansExactMatchNotPrototypeThrows () {

    ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
    Map<String, DummyJob> beanMap = new LinkedHashMap<>();

    beanMap.put("group.name", new DummyJob());
    beanMap.put("otherBean", new DummyJob());
    Mockito.when(applicationContext.getBeansOfType(DummyJob.class)).thenReturn(beanMap);
    Mockito.when(applicationContext.isPrototype("group.name")).thenReturn(false);

    try {
      new SpringJobFactory(applicationContext).newJob(bundleFor(new JobKey("name", "group")), Mockito.mock(Scheduler.class));
      Assert.fail("expected FormattedSchedulerException");
    } catch (SchedulerException expected) {
      Assert.assertTrue(expected instanceof FormattedSchedulerException);
      Assert.assertTrue(expected.getMessage().contains("not a prototype"));
    }
  }

  public void testMultipleBeansWithoutExactMatchThrows () {

    ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
    Map<String, DummyJob> beanMap = new LinkedHashMap<>();

    beanMap.put("firstBean", new DummyJob());
    beanMap.put("secondBean", new DummyJob());
    Mockito.when(applicationContext.getBeansOfType(DummyJob.class)).thenReturn(beanMap);

    try {
      new SpringJobFactory(applicationContext).newJob(bundleFor(new JobKey("name", "group")), Mockito.mock(Scheduler.class));
      Assert.fail("expected FormattedSchedulerException");
    } catch (SchedulerException expected) {
      Assert.assertTrue(expected instanceof FormattedSchedulerException);
      Assert.assertTrue(expected.getMessage().contains("Multiple jobs"));
    }
  }

  public void testCloseClosesCloseableContext ()
    throws Exception {

    ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);

    new SpringJobFactory(applicationContext).close();

    Mockito.verify(applicationContext).close();
  }

  public void testCloseIgnoresNonCloseableContext ()
    throws Exception {

    // A plain ApplicationContext is not Closeable, so close() must be a no-op.
    new SpringJobFactory(Mockito.mock(ApplicationContext.class)).close();
  }
}
