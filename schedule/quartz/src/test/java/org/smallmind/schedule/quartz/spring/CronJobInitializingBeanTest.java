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
import org.mockito.Mockito;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CronJobInitializingBeanTest {

  private JobDetail mockDetail (JobKey jobKey, boolean durable, boolean recovery, boolean concurrentDisallowed, boolean persist, JobDataMap jobDataMap) {

    JobDetail jobDetail = Mockito.mock(JobDetail.class);

    Mockito.when(jobDetail.getKey()).thenReturn(jobKey);
    Mockito.when(jobDetail.isDurable()).thenReturn(durable);
    Mockito.when(jobDetail.requestsRecovery()).thenReturn(recovery);
    Mockito.when(jobDetail.isConcurrentExecutionDisallowed()).thenReturn(concurrentDisallowed);
    Mockito.when(jobDetail.isPersistJobDataAfterExecution()).thenReturn(persist);
    Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

    return jobDetail;
  }

  private CronTrigger mockTrigger (TriggerKey triggerKey, String cronExpression) {

    CronTrigger cronTrigger = Mockito.mock(CronTrigger.class);

    Mockito.when(cronTrigger.getKey()).thenReturn(triggerKey);
    Mockito.when(cronTrigger.getCronExpression()).thenReturn(cronExpression);

    return cronTrigger;
  }

  private CronJobInitializingBean beanFor (Scheduler scheduler, JobDetail jobDetail, CronTrigger cronTrigger) {

    Map<JobDetail, List<CronTrigger>> jobMap = new HashMap<>();

    jobMap.put(jobDetail, List.of(cronTrigger));

    CronJobInitializingBean bean = new CronJobInitializingBean();

    bean.setScheduler(scheduler);
    bean.setJobMap(jobMap);

    return bean;
  }

  public void testMissingJobAddedAndMissingTriggerScheduled ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobKey jobKey = new JobKey("name", "group");
    TriggerKey triggerKey = new TriggerKey("trigger", "group");
    JobDetail jobDetail = mockDetail(jobKey, true, false, false, false, new JobDataMap());
    CronTrigger cronTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");

    Mockito.when(scheduler.getJobDetail(jobKey)).thenReturn(null);
    Mockito.when(scheduler.getTrigger(triggerKey)).thenReturn(null);

    beanFor(scheduler, jobDetail, cronTrigger).afterPropertiesSet();

    Mockito.verify(scheduler).addJob(jobDetail, false);
    Mockito.verify(scheduler).scheduleJob(cronTrigger);
  }

  public void testUnchangedJobAndTriggerAreLeftAlone ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobKey jobKey = new JobKey("name", "group");
    TriggerKey triggerKey = new TriggerKey("trigger", "group");
    JobDetail jobDetail = mockDetail(jobKey, true, false, false, false, new JobDataMap());
    JobDetail installedDetail = mockDetail(jobKey, true, false, false, false, new JobDataMap());
    CronTrigger cronTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");
    CronTrigger installedTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");

    Mockito.when(scheduler.getJobDetail(jobKey)).thenReturn(installedDetail);
    Mockito.when(scheduler.getTrigger(triggerKey)).thenReturn(installedTrigger);

    beanFor(scheduler, jobDetail, cronTrigger).afterPropertiesSet();

    Mockito.verify(scheduler, Mockito.never()).addJob(Mockito.any(JobDetail.class), Mockito.anyBoolean());
    Mockito.verify(scheduler, Mockito.never()).scheduleJob(Mockito.any(CronTrigger.class));
    Mockito.verify(scheduler, Mockito.never()).rescheduleJob(Mockito.any(TriggerKey.class), Mockito.any(CronTrigger.class));
  }

  public void testChangedJobIsReplaced ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobKey jobKey = new JobKey("name", "group");
    TriggerKey triggerKey = new TriggerKey("trigger", "group");
    JobDetail jobDetail = mockDetail(jobKey, true, false, false, false, new JobDataMap());
    JobDetail installedDetail = mockDetail(jobKey, false, false, false, false, new JobDataMap());
    CronTrigger cronTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");
    CronTrigger installedTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");

    Mockito.when(scheduler.getJobDetail(jobKey)).thenReturn(installedDetail);
    Mockito.when(scheduler.getTrigger(triggerKey)).thenReturn(installedTrigger);

    beanFor(scheduler, jobDetail, cronTrigger).afterPropertiesSet();

    Mockito.verify(scheduler).addJob(jobDetail, true);
  }

  public void testJobReplacedWhenJobDataMapDiffers ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobKey jobKey = new JobKey("name", "group");
    TriggerKey triggerKey = new TriggerKey("trigger", "group");
    JobDataMap desiredData = new JobDataMap();
    JobDataMap installedData = new JobDataMap();

    desiredData.put("threshold", "10");
    installedData.put("threshold", "20");

    JobDetail jobDetail = mockDetail(jobKey, true, false, false, false, desiredData);
    JobDetail installedDetail = mockDetail(jobKey, true, false, false, false, installedData);
    CronTrigger cronTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");
    CronTrigger installedTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");

    Mockito.when(scheduler.getJobDetail(jobKey)).thenReturn(installedDetail);
    Mockito.when(scheduler.getTrigger(triggerKey)).thenReturn(installedTrigger);

    beanFor(scheduler, jobDetail, cronTrigger).afterPropertiesSet();

    Mockito.verify(scheduler).addJob(jobDetail, true);
  }

  public void testChangedTriggerIsRescheduled ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobKey jobKey = new JobKey("name", "group");
    TriggerKey triggerKey = new TriggerKey("trigger", "group");
    JobDetail jobDetail = mockDetail(jobKey, true, false, false, false, new JobDataMap());
    JobDetail installedDetail = mockDetail(jobKey, true, false, false, false, new JobDataMap());
    CronTrigger cronTrigger = mockTrigger(triggerKey, "0 0/10 * * * ?");
    CronTrigger installedTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");

    Mockito.when(scheduler.getJobDetail(jobKey)).thenReturn(installedDetail);
    Mockito.when(scheduler.getTrigger(triggerKey)).thenReturn(installedTrigger);

    beanFor(scheduler, jobDetail, cronTrigger).afterPropertiesSet();

    Mockito.verify(scheduler).rescheduleJob(triggerKey, cronTrigger);
  }

  public void testErrorOnOneEntryDoesNotAbortRemaining ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobKey failingKey = new JobKey("failing", "group");
    JobKey healthyKey = new JobKey("healthy", "group");
    TriggerKey healthyTriggerKey = new TriggerKey("healthyTrigger", "group");
    JobDetail failingDetail = mockDetail(failingKey, true, false, false, false, new JobDataMap());
    JobDetail healthyDetail = mockDetail(healthyKey, true, false, false, false, new JobDataMap());
    CronTrigger failingTrigger = mockTrigger(new TriggerKey("failingTrigger", "group"), "0 0/5 * * * ?");
    CronTrigger healthyTrigger = mockTrigger(healthyTriggerKey, "0 0/5 * * * ?");

    Mockito.when(scheduler.getJobDetail(failingKey)).thenThrow(new RuntimeException("scheduler unavailable"));
    Mockito.when(scheduler.getJobDetail(healthyKey)).thenReturn(null);
    Mockito.when(scheduler.getTrigger(healthyTriggerKey)).thenReturn(null);

    Map<JobDetail, List<CronTrigger>> jobMap = new HashMap<>();

    jobMap.put(failingDetail, List.of(failingTrigger));
    jobMap.put(healthyDetail, List.of(healthyTrigger));

    CronJobInitializingBean bean = new CronJobInitializingBean();

    bean.setScheduler(scheduler);
    bean.setJobMap(jobMap);
    bean.afterPropertiesSet();

    // The healthy entry is processed despite the failing entry throwing.
    Mockito.verify(scheduler).addJob(healthyDetail, false);
    Mockito.verify(scheduler).scheduleJob(healthyTrigger);
  }

  /**
   * Installs a single entry whose installed job detail already exists under the same key, so the
   * only decision the bean makes is whether {@code isSame} considers the desired detail a match.
   * Returns the scheduler mock for replacement verification. The trigger is identical on both
   * sides, so trigger handling never contributes an {@code addJob} call.
   */
  private Scheduler runAgainstInstalled (JobDetail desired, JobDetail installed)
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobKey jobKey = desired.getKey();
    TriggerKey triggerKey = new TriggerKey("trigger", "group");
    CronTrigger desiredTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");
    CronTrigger installedTrigger = mockTrigger(triggerKey, "0 0/5 * * * ?");

    Mockito.when(scheduler.getJobDetail(jobKey)).thenReturn(installed);
    Mockito.when(scheduler.getTrigger(triggerKey)).thenReturn(installedTrigger);

    beanFor(scheduler, desired, desiredTrigger).afterPropertiesSet();

    return scheduler;
  }

  private void assertReplaced (JobDetail desired, JobDetail installed)
    throws Exception {

    Mockito.verify(runAgainstInstalled(desired, installed)).addJob(desired, true);
  }

  private void assertNotReplaced (JobDetail desired, JobDetail installed)
    throws Exception {

    Mockito.verify(runAgainstInstalled(desired, installed), Mockito.never()).addJob(Mockito.any(JobDetail.class), Mockito.eq(true));
  }

  private JobDataMap dataMap (String... keyValuePairs) {

    JobDataMap jobDataMap = new JobDataMap();

    for (int index = 0; index < keyValuePairs.length; index += 2) {
      jobDataMap.put(keyValuePairs[index], keyValuePairs[index + 1]);
    }

    return jobDataMap;
  }

  public void testJobReplacedWhenRequestsRecoveryDiffers ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertReplaced(mockDetail(jobKey, true, false, false, false, new JobDataMap()), mockDetail(jobKey, true, true, false, false, new JobDataMap()));
  }

  public void testJobReplacedWhenConcurrentExecutionDisallowedDiffers ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertReplaced(mockDetail(jobKey, true, false, false, false, new JobDataMap()), mockDetail(jobKey, true, false, true, false, new JobDataMap()));
  }

  public void testJobReplacedWhenPersistJobDataAfterExecutionDiffers ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertReplaced(mockDetail(jobKey, true, false, false, false, new JobDataMap()), mockDetail(jobKey, true, false, false, true, new JobDataMap()));
  }

  public void testJobReplacedWhenJobDataKeyCountDiffers ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertReplaced(mockDetail(jobKey, true, false, false, false, dataMap("a", "1")), mockDetail(jobKey, true, false, false, false, new JobDataMap()));
  }

  public void testJobReplacedWhenJobDataKeyNameDiffers ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertReplaced(mockDetail(jobKey, true, false, false, false, dataMap("a", "1")), mockDetail(jobKey, true, false, false, false, dataMap("b", "1")));
  }

  public void testJobReplacedWhenJobDataValueNullOnDesiredOnly ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertReplaced(mockDetail(jobKey, true, false, false, false, dataMap("k", null)), mockDetail(jobKey, true, false, false, false, dataMap("k", "value")));
  }

  public void testJobReplacedWhenJobDataValueNullOnInstalledOnly ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertReplaced(mockDetail(jobKey, true, false, false, false, dataMap("k", "value")), mockDetail(jobKey, true, false, false, false, dataMap("k", null)));
  }

  public void testIdenticalMultiKeyJobDataIsNotReplaced ()
    throws Exception {

    JobKey jobKey = new JobKey("name", "group");

    assertNotReplaced(mockDetail(jobKey, true, false, false, false, dataMap("a", "1", "b", "2")), mockDetail(jobKey, true, false, false, false, dataMap("a", "1", "b", "2")));
  }
}
