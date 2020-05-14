/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.context.ApplicationContext;

public class SpringJobFactory implements JobFactory {

  private final ApplicationContext applicationContext;

  public SpringJobFactory (ApplicationContext applicationContext) {

    this.applicationContext = applicationContext;
  }

  public synchronized void close ()
    throws IOException {

    if ((applicationContext != null) && (Closeable.class.isAssignableFrom(applicationContext.getClass()))) {
      ((Closeable)applicationContext).close();
    }
  }

  @Override
  public synchronized Job newJob (TriggerFiredBundle bundle, Scheduler scheduler)
    throws SchedulerException {

    Map<String, ? extends Job> jobMap = applicationContext.getBeansOfType(bundle.getJobDetail().getJobClass());

    if (jobMap.size() == 0) {
      throw new FormattedSchedulerException("No job(%s) of type(%s) is present in the application context", bundle.getJobDetail().getKey(), bundle.getJobDetail().getJobClass().getName());
    } else if (jobMap.size() == 1) {

      Map.Entry<String, ? extends Job> jobEntry = jobMap.entrySet().iterator().next();

      if (!applicationContext.isPrototype(jobEntry.getKey())) {
        throw new FormattedSchedulerException("The matching job(%s) with id(%s) in the application context is not a prototype bean", bundle.getJobDetail().getKey(), jobEntry.getKey());
      }

      return jobEntry.getValue();
    } else {
      for (Map.Entry<String, ? extends Job> jobEntry : jobMap.entrySet()) {
        if (jobEntry.getKey().equals(bundle.getJobDetail().getKey().toString())) {
          if (!applicationContext.isPrototype(jobEntry.getKey())) {
            throw new FormattedSchedulerException("The matching job(%s) with id(%s) in the application context is not a prototype bean", bundle.getJobDetail().getKey(), jobEntry.getKey());
          }

          return jobEntry.getValue();
        }
      }

      throw new FormattedSchedulerException("Multiple jobs of type(%s) are present in the application context, but none with an exact id(%s)", bundle.getJobDetail().getJobClass().getName(), bundle.getJobDetail().getKey());
    }
  }
}
