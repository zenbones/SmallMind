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
 * FactoryBean that assembles a map of Quartz {@link JobDetail} to
 * {@link CronTrigger} definitions discovered in the Spring context. Only
 * {@link CronJob} beans whose identifiers are explicitly allowed are
 * included.
 */
public class CronJobMapFactoryBean implements FactoryBean<Map<JobDetail, List<CronTrigger>>>, BeanPostProcessor {

  private final HashMap<JobDetail, List<CronTrigger>> jobMap = new HashMap<>();
  private List<String> allowedJobIds;

  /**
   * Restrict which jobs should be collected by id of form {@code group.name}.
   *
   * @param allowedJobIds list of allowed identifiers
   */
  public void setAllowedJobIds (List<String> allowedJobIds) {

    this.allowedJobIds = allowedJobIds;
  }

  /**
   * FactoryBean contract indicating the produced map is a singleton.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Declares {@link Map} as the produced object type.
   *
   * @return {@code Map.class}
   */
  @Override
  public Class<?> getObjectType () {

    return Map.class;
  }

  /**
   * Collect {@link CronJob} beans after initialization. Matches only beans
   * whose {@link JobDetail} key forms an identifier present in
   * {@link #allowedJobIds}. Matching entries are added to the produced map.
   *
   * @param bean     the initialized bean
   * @param beanName Spring bean name
   * @return the original bean
   * @throws BeansException if post-processing fails
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
   * Provide the collected mapping of job details to cron triggers.
   *
   * @return map of configured jobs to their triggers
   */
  @Override
  public Map<JobDetail, List<CronTrigger>> getObject () {

    return jobMap;
  }
}
