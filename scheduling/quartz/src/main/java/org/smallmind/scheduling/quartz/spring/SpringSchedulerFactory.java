/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.scheduling.quartz.spring;

import java.util.Properties;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.OnOrOff;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class SpringSchedulerFactory extends StdSchedulerFactory implements ApplicationContextAware, ApplicationListener<ContextClosedEvent> {

  private SpringJobFactory jobFactory;
  private OnOrOff standbyMode = OnOrOff.OFF;

  public SpringSchedulerFactory (Properties properties)
    throws SchedulerException {

    super(properties);
  }

  public void setStandbyMode (OnOrOff standbyMode) {

    this.standbyMode = standbyMode;
  }

  @Override
  public synchronized void setApplicationContext (ApplicationContext applicationContext)
    throws BeansException {

    jobFactory = new SpringJobFactory(applicationContext);
  }

  @Override
  public void onApplicationEvent (ContextClosedEvent event) {

    try {
      Scheduler scheduler;

      if ((scheduler = super.getScheduler()) != null) {
        scheduler.shutdown(true);
      }
    } catch (SchedulerException schedulerException) {
      LoggerManager.getLogger(SpringSchedulerFactory.class).error(schedulerException);
    }
  }

  @Override
  public Scheduler getScheduler () throws SchedulerException {

    Scheduler scheduler;

    scheduler = super.getScheduler();
    scheduler.setJobFactory(jobFactory);

    switch (standbyMode) {
      case ON:
        scheduler.standby();
        break;
      case OFF:
        scheduler.start();
        break;
      default:
        throw new UnknownSwitchCaseException(standbyMode.name());
    }

    return scheduler;
  }
}
