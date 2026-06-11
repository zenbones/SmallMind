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

import java.util.List;
import org.mockito.Mockito;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CronJobMapFactoryBeanTest {

  private CronJob cronJobWithKey (JobKey jobKey) {

    JobDetail jobDetail = Mockito.mock(JobDetail.class);

    Mockito.when(jobDetail.getKey()).thenReturn(jobKey);

    CronJob cronJob = new CronJob();

    cronJob.setJobDetail(jobDetail);
    cronJob.setCronTrigger(Mockito.mock(CronTrigger.class));

    return cronJob;
  }

  public void testAllowedCronJobIsAddedToMap () {

    CronJobMapFactoryBean factoryBean = new CronJobMapFactoryBean();
    CronJob cronJob = cronJobWithKey(new JobKey("name", "group"));

    factoryBean.setAllowedJobIds(List.of("group.name"));

    Object returned = factoryBean.postProcessAfterInitialization(cronJob, "scheduled.bean");

    Assert.assertSame(returned, cronJob);
    Assert.assertTrue(factoryBean.getObject().containsKey(cronJob.getJobDetail()));
    Assert.assertEquals(factoryBean.getObject().get(cronJob.getJobDetail()), List.of(cronJob.getCronTrigger()));
  }

  public void testDisallowedCronJobIsNotAdded () {

    CronJobMapFactoryBean factoryBean = new CronJobMapFactoryBean();
    CronJob cronJob = cronJobWithKey(new JobKey("name", "group"));

    factoryBean.setAllowedJobIds(List.of("group.somethingElse"));

    Object returned = factoryBean.postProcessAfterInitialization(cronJob, "scheduled.bean");

    Assert.assertSame(returned, cronJob);
    Assert.assertTrue(factoryBean.getObject().isEmpty());
  }

  public void testNonCronJobBeanIsIgnored () {

    CronJobMapFactoryBean factoryBean = new CronJobMapFactoryBean();
    Object plainBean = new Object();

    Object returned = factoryBean.postProcessAfterInitialization(plainBean, "ordinary.bean");

    Assert.assertSame(returned, plainBean);
    Assert.assertTrue(factoryBean.getObject().isEmpty());
  }
}
