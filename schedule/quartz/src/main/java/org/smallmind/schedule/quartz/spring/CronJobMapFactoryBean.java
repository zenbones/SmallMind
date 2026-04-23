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
import org.quartz.JobKey;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Spring {@link FactoryBean} that scans the application context for
 * {@link CronJob} beans and assembles those whose identifiers appear in an
 * explicit allowlist into a {@link Map} of {@link JobDetail} to
 * {@link CronTrigger} lists. The produced map is intended as input to
 * {@link CronJobInitializingBean}.
 */
public class CronJobMapFactoryBean implements FactoryBean<Map<JobDetail, List<CronTrigger>>>, BeanPostProcessor {

  private final HashMap<JobDetail, List<CronTrigger>> jobMap = new HashMap<>();
  private List<String> allowedJobIds;

  /**
   * Sets the identifiers of jobs that should be included in the produced map.
   * Each identifier must be of the form {@code group.name}, matching the
   * {@link JobKey} of the corresponding {@link CronJob}.
   *
   * @param allowedJobIds list of {@code group.name} strings designating permitted jobs
   */
  public void setAllowedJobIds (List<String> allowedJobIds) {

    this.allowedJobIds = allowedJobIds;
  }

  /**
   * Declares the produced object as a singleton.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Declares the type of the produced object.
   *
   * @return {@code Map.class}
   */
  @Override
  public Class<?> getObjectType () {

    return Map.class;
  }

  /**
   * Inspects each initialized bean and, if it is a {@link CronJob} whose
   * {@link JobKey} produces an identifier present in {@link #allowedJobIds},
   * adds it to the internal map. Non-matching beans are returned unchanged.
   *
   * @param bean     the bean that has just been initialized
   * @param beanName the bean's name within the context
   * @return the original {@code bean} instance, unmodified
   * @throws BeansException if post-processing encounters an error
   */
  @Override
  public Object postProcessAfterInitialization (Object bean, String beanName)
    throws BeansException {

    if (bean instanceof CronJob) {

      JobKey jobKey = ((CronJob)bean).getJobDetail().getKey();
      String jobId = jobKey.getGroup() + "." + jobKey.getName();

      for (String allowedJobId : allowedJobIds) {
        if (allowedJobId.equals(jobId)) {
          jobMap.put(((CronJob)bean).getJobDetail(), List.of(((CronJob)bean).getCronTrigger()));
          break;
        }
      }
    }

    return bean;
  }

  /**
   * Returns the assembled map of job details to their cron triggers.
   *
   * @return map populated from allowed {@link CronJob} beans
   */
  @Override
  public Map<JobDetail, List<CronTrigger>> getObject () {

    return jobMap;
  }
}
