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
            if ((installedJobDetail = scheduler.getJobDetail(jobDetail.getName(), jobDetail.getGroup())) == null) {
               scheduler.addJob(jobDetail, false);
            }
            else if (!isSame(jobDetail, installedJobDetail)) {
               scheduler.addJob(jobDetail, true);
            }

            if ((installedCronTrigger = (CronTrigger)scheduler.getTrigger(cronTrigger.getName(), cronTrigger.getGroup())) == null) {
               scheduler.scheduleJob(cronTrigger);
            }
            else if (!cronTrigger.getCronExpression().equals(installedCronTrigger.getCronExpression())) {
               scheduler.rescheduleJob(installedCronTrigger.getName(), installedCronTrigger.getGroup(), cronTrigger);
            }
         }
      }

      scheduler.start();
   }

   private boolean isSame (JobDetail jobDetail, JobDetail installedJobDetail) {

      if (jobDetail.isVolatile() != installedJobDetail.isVolatile()) {

         return false;
      }
      if (jobDetail.isDurable() != installedJobDetail.isDurable()) {

         return false;
      }
      if (jobDetail.isStateful() != installedJobDetail.isStateful()) {

         return false;
      }
      if (jobDetail.requestsRecovery() != installedJobDetail.requestsRecovery()) {

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
