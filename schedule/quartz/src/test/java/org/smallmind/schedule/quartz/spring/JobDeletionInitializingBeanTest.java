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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JobDeletionInitializingBeanTest {

  private JobIdentifier identifier (String name, String group) {

    JobIdentifier jobIdentifier = new JobIdentifier();

    jobIdentifier.setName(name);
    jobIdentifier.setGroup(group);

    return jobIdentifier;
  }

  public void testEachIdentifierIsDeletedAsNameGroupKey ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobDeletionInitializingBean bean = new JobDeletionInitializingBean();

    bean.setScheduler(scheduler);
    bean.setJobIdentifierList(List.of(identifier("legacyExport", "scheduled"), identifier("oldReport", "reports")));
    bean.afterPropertiesSet();

    ArgumentCaptor<JobKey> captor = ArgumentCaptor.forClass(JobKey.class);

    Mockito.verify(scheduler, Mockito.times(2)).deleteJob(captor.capture());

    // Lock the (name, group) argument ordering of the constructed JobKey.
    JobKey first = captor.getAllValues().get(0);
    JobKey second = captor.getAllValues().get(1);

    Assert.assertEquals(first.getName(), "legacyExport");
    Assert.assertEquals(first.getGroup(), "scheduled");
    Assert.assertEquals(second.getName(), "oldReport");
    Assert.assertEquals(second.getGroup(), "reports");
  }

  public void testEmptyIdentifierListDeletesNothing ()
    throws Exception {

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    JobDeletionInitializingBean bean = new JobDeletionInitializingBean();

    bean.setScheduler(scheduler);
    bean.afterPropertiesSet();

    Mockito.verifyNoInteractions(scheduler);
  }
}
