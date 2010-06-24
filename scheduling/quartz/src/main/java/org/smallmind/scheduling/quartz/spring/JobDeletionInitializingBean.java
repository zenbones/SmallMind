package org.smallmind.scheduling.quartz.spring;

import java.util.LinkedList;
import java.util.List;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.InitializingBean;

public class JobDeletionInitializingBean implements InitializingBean {

   private Scheduler scheduler;
   private LinkedList<JobIdentifier> jobIdentifierList;

   public JobDeletionInitializingBean () {

      jobIdentifierList = new LinkedList<JobIdentifier>();
   }

   public void setScheduler (Scheduler scheduler) {

      this.scheduler = scheduler;
   }

   public void setJobIdentifierList (List<JobIdentifier> jobIdentifierList) {

      this.jobIdentifierList.addAll(jobIdentifierList);
   }

   public void afterPropertiesSet ()
      throws SchedulerException {

      for (JobIdentifier jobIdentifier : jobIdentifierList) {
         scheduler.deleteJob(jobIdentifier.getName(), jobIdentifier.getGroup());
      }
   }
}
