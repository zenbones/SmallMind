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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.smallmind.schedule.quartz.QuartzProxyJob;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link CronJobInitializingBean} against a real (in-memory {@code RAMJobStore})
 * scheduler held in standby, so the synchronize decision tree is verified against genuine Quartz
 * job-store state rather than a mocked {@link Scheduler}.
 */
@Test(groups = "integration")
public class CronJobInstallationIntegrationTest {

  public static class NoopJob extends QuartzProxyJob {

    @Override
    public boolean logOnZeroCount () {

      return false;
    }

    @Override
    public void proceed () {
    }

    @Override
    public void cleanup () {
    }
  }

  private Scheduler standbyScheduler (String instanceName)
    throws SchedulerException {

    Properties properties = new Properties();

    properties.setProperty("org.quartz.scheduler.instanceName", instanceName);
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "1");
    properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

    // getScheduler() returns a scheduler that has not been started, so it sits in standby and the
    // installed cron triggers never fire during the test.
    return new StdSchedulerFactory(properties).getScheduler();
  }

  private void install (Scheduler scheduler, JobDetail jobDetail, CronTrigger cronTrigger) {

    Map<JobDetail, List<CronTrigger>> jobMap = new HashMap<>();

    jobMap.put(jobDetail, List.of(cronTrigger));

    CronJobInitializingBean bean = new CronJobInitializingBean();

    bean.setScheduler(scheduler);
    bean.setJobMap(jobMap);
    bean.afterPropertiesSet();
  }

  public void testCronJobInstalledThenRescheduledAgainstLiveStore ()
    throws Exception {

    Scheduler scheduler = standbyScheduler("cronInstallScheduler");

    try {
      JobKey jobKey = new JobKey("reportJob", "scheduled");
      TriggerKey triggerKey = new TriggerKey("reportTrigger", "scheduled");
      JobDetail jobDetail = JobBuilder.newJob(NoopJob.class).withIdentity(jobKey).storeDurably().build();
      CronTrigger firstTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey).withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")).build();

      install(scheduler, jobDetail, firstTrigger);

      Assert.assertNotNull(scheduler.getJobDetail(jobKey), "job was not installed");
      Assert.assertEquals(((CronTrigger)scheduler.getTrigger(triggerKey)).getCronExpression(), "0 0/5 * * * ?");

      CronTrigger changedTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey).withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?")).build();

      // Re-running with only the cron expression changed must reschedule the existing trigger.
      install(scheduler, jobDetail, changedTrigger);

      Assert.assertEquals(((CronTrigger)scheduler.getTrigger(triggerKey)).getCronExpression(), "0 0/10 * * * ?", "the trigger was not rescheduled");
    } finally {
      scheduler.shutdown();
    }
  }
}
