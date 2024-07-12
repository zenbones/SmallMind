/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class CronJobMapFactoryBean implements FactoryBean<Map<JobDetail, List<CronTrigger>>>, BeanPostProcessor {

  private final HashMap<JobDetail, List<CronTrigger>> jobMap = new HashMap<>();
  private List<String> allowedJobIds;

  public void setAllowedJobIds (List<String> allowedJobIds) {

    this.allowedJobIds = allowedJobIds;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return Map.class;
  }

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

  @Override
  public Map<JobDetail, List<CronTrigger>> getObject () {

    return jobMap;
  }
}

