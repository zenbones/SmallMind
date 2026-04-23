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
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link InitializingBean} that synchronizes a set of
 * {@link JobDetail}/{@link CronTrigger} pairs with a Quartz {@link Scheduler}
 * during context startup. Jobs and triggers are added if absent, or replaced
 * when their definitions have changed since last registration.
 */
public class CronJobInitializingBean implements InitializingBean {

  private final HashMap<JobDetail, List<CronTrigger>> jobMap;
  private Scheduler scheduler;

  /**
   * Creates an instance with an empty job map.
   */
  public CronJobInitializingBean () {

    jobMap = new HashMap<>();
  }

  /**
   * Sets the scheduler into which jobs and triggers will be installed.
   *
   * @param scheduler target Quartz scheduler
   */
  public void setScheduler (Scheduler scheduler) {

    this.scheduler = scheduler;
  }

  /**
   * Supplies the job definitions and their associated triggers to register.
   * All entries are merged into the internal map.
   *
   * @param jobMap map of {@link JobDetail} to one or more {@link CronTrigger}s
   */
  public void setJobMap (Map<JobDetail, List<CronTrigger>> jobMap) {

    this.jobMap.putAll(jobMap);
  }

  /**
   * Iterates the configured job map and registers each job and trigger with
   * the scheduler. A missing job is added; a changed job is replaced. A
   * missing trigger is scheduled; a trigger whose cron expression has changed
   * is rescheduled. Errors for individual entries are logged and do not abort
   * processing of remaining entries.
   */
  public void afterPropertiesSet () {

    CronTrigger installedCronTrigger;
    JobDetail installedJobDetail;

    for (JobDetail jobDetail : jobMap.keySet()) {
      for (CronTrigger cronTrigger : jobMap.get(jobDetail)) {
        try {
          if ((installedJobDetail = scheduler.getJobDetail(jobDetail.getKey())) == null) {
            scheduler.addJob(jobDetail, false);
          } else if (!isSame(jobDetail, installedJobDetail)) {
            scheduler.addJob(jobDetail, true);
          }

          if ((installedCronTrigger = (CronTrigger)scheduler.getTrigger(cronTrigger.getKey())) == null) {
            scheduler.scheduleJob(cronTrigger);
          } else if (!cronTrigger.getCronExpression().equals(installedCronTrigger.getCronExpression())) {
            scheduler.rescheduleJob(installedCronTrigger.getKey(), cronTrigger);
          }
        } catch (Exception exception) {
          LoggerManager.getLogger(CronJobInitializingBean.class).error(exception);
        }
      }
    }
  }

  /**
   * Performs a field-by-field comparison of two {@link JobDetail} instances
   * to determine whether the desired definition differs from what is currently
   * installed in the scheduler. Compares durability, recovery, concurrency,
   * persistence flags, and the full job data map.
   *
   * @param jobDetail          the desired job definition
   * @param installedJobDetail the definition currently held by the scheduler
   * @return {@code true} if all compared fields are equal, {@code false} on any difference
   */
  private boolean isSame (JobDetail jobDetail, JobDetail installedJobDetail) {

    if (jobDetail.isDurable() != installedJobDetail.isDurable()) {

      return false;
    }
    if (jobDetail.requestsRecovery() != installedJobDetail.requestsRecovery()) {

      return false;
    }
    if (jobDetail.isConcurrentExecutionDisallowed() != installedJobDetail.isConcurrentExecutionDisallowed()) {

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
    } else {
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
        } else {
          detailValue = jobDetail.getJobDataMap().get(detailKey);
          installedDetailValue = installedJobDetail.getJobDataMap().get(detailKey);
          if ((detailValue == null) && (installedDetailValue != null)) {

            return false;
          } else if ((detailValue != null) && (installedDetailValue == null)) {

            return false;
          } else if ((detailValue != null) && (!detailValue.equals(installedDetailValue))) {

            return false;
          }
        }
      }
    }

    return true;
  }
}
