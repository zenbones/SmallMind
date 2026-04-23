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

import java.util.LinkedList;
import java.util.List;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link InitializingBean} that removes a configured list of jobs from
 * a Quartz {@link Scheduler} when the application context starts. Useful for
 * retiring obsolete schedules before new ones are installed.
 */
public class JobDeletionInitializingBean implements InitializingBean {

  private final LinkedList<JobIdentifier> jobIdentifierList;
  private Scheduler scheduler;

  /**
   * Creates an instance with an empty list of jobs to delete.
   */
  public JobDeletionInitializingBean () {

    jobIdentifierList = new LinkedList<JobIdentifier>();
  }

  /**
   * Sets the scheduler from which jobs will be deleted.
   *
   * @param scheduler target Quartz scheduler
   */
  public void setScheduler (Scheduler scheduler) {

    this.scheduler = scheduler;
  }

  /**
   * Sets the list of job name/group pairs identifying jobs to remove.
   *
   * @param jobIdentifierList identifiers of jobs to delete from the scheduler
   */
  public void setJobIdentifierList (List<JobIdentifier> jobIdentifierList) {

    this.jobIdentifierList.addAll(jobIdentifierList);
  }

  /**
   * Deletes each configured job from the scheduler. Called by Spring after
   * all properties are set.
   *
   * @throws SchedulerException if the scheduler reports an error deleting any job
   */
  public void afterPropertiesSet ()
    throws SchedulerException {

    for (JobIdentifier jobIdentifier : jobIdentifierList) {
      scheduler.deleteJob(new JobKey(jobIdentifier.getName(), jobIdentifier.getGroup()));
    }
  }
}
