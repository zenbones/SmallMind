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

import org.quartz.CronTrigger;
import org.quartz.JobDetail;

/**
 * Pairs a Quartz {@link JobDetail} with a {@link CronTrigger} for convenient
 * Spring bean configuration. Instances are discovered by
 * {@link CronJobMapFactoryBean} and registered with the scheduler by
 * {@link CronJobInitializingBean}.
 */
public class CronJob {

  private JobDetail jobDetail;
  private CronTrigger cronTrigger;

  /**
   * Returns the job definition to be scheduled.
   *
   * @return the associated {@link JobDetail}
   */
  public JobDetail getJobDetail () {

    return jobDetail;
  }

  /**
   * Sets the job definition to schedule.
   *
   * @param jobDetail the {@link JobDetail} describing the job class and data
   */
  public void setJobDetail (JobDetail jobDetail) {

    this.jobDetail = jobDetail;
  }

  /**
   * Returns the cron trigger that governs when the job fires.
   *
   * @return the associated {@link CronTrigger}
   */
  public CronTrigger getCronTrigger () {

    return cronTrigger;
  }

  /**
   * Sets the cron trigger that governs when the job fires.
   *
   * @param cronTrigger the {@link CronTrigger} defining the firing schedule
   */
  public void setCronTrigger (CronTrigger cronTrigger) {

    this.cronTrigger = cronTrigger;
  }
}
