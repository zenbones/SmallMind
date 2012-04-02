/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scheduling.quartz.spring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.InitializingBean;

public class CronJobInitializingBean implements InitializingBean {

  private Scheduler scheduler;
  private HashMap<JobDetail, List<CronTrigger>> jobMap;

  public CronJobInitializingBean () {

    jobMap = new HashMap<JobDetail, List<CronTrigger>>();
  }

  public void setScheduler (Scheduler scheduler) {

    this.scheduler = scheduler;
  }

  public void setJobMap (Map<JobDetail, List<CronTrigger>> jobMap) {

    this.jobMap.putAll(jobMap);
  }

  public void afterPropertiesSet ()
    throws SchedulerException {

    CronTrigger installedCronTrigger;
    JobDetail installedJobDetail;

    for (JobDetail jobDetail : jobMap.keySet()) {
      for (CronTrigger cronTrigger : jobMap.get(jobDetail)) {
        if ((installedJobDetail = scheduler.getJobDetail(jobDetail.getKey())) == null) {
          scheduler.addJob(jobDetail, false);
        }
        else if (!isSame(jobDetail, installedJobDetail)) {
          scheduler.addJob(jobDetail, true);
        }

        if ((installedCronTrigger = (CronTrigger)scheduler.getTrigger(cronTrigger.getKey())) == null) {
          scheduler.scheduleJob(cronTrigger);
        }
        else if (!cronTrigger.getCronExpression().equals(installedCronTrigger.getCronExpression())) {
          scheduler.rescheduleJob(installedCronTrigger.getKey(), cronTrigger);
        }
      }
    }

    scheduler.start();
  }

  private boolean isSame (JobDetail jobDetail, JobDetail installedJobDetail) {

    if (jobDetail.isDurable() != installedJobDetail.isDurable()) {

      return false;
    }
    if (jobDetail.requestsRecovery() != installedJobDetail.requestsRecovery()) {

      return false;
    }
    if (jobDetail.isConcurrentExectionDisallowed() != installedJobDetail.isConcurrentExectionDisallowed()) {

      return false;
    }
    if (jobDetail.isPersistJobDataAfterExecution() != installedJobDetail.isPersistJobDataAfterExecution()) {

      return false;
    }

    Object detailValue;
    Object installedDetailValue;
    String[] detailKeys = jobDetail.getJobDataMap().getKeys();
    String[] installedKeys = installedJobDetail.getJobDataMap().getKeys();
    boolean match;

    if (detailKeys.length != installedKeys.length) {

      return false;
    }
    else {
      for (String detailKey : detailKeys) {
        match = false;
        for (String installedKey : installedKeys) {
          if (detailKey.equals(installedKey)) {
            match = true;
            break;
          }
        }

        if (!match) {
          return false;
        }
        else {
          detailValue = jobDetail.getJobDataMap().get(detailKey);
          installedDetailValue = installedJobDetail.getJobDataMap().get(detailKey);
          if ((detailValue == null) && (installedDetailValue != null)) {

            return false;
          }
          else if ((detailValue != null) && (installedDetailValue == null)) {

            return false;
          }
          else if ((detailValue != null) && (!detailValue.equals(installedDetailValue))) {

            return false;
          }
        }
      }
    }

    return true;
  }
}
