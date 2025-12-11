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

import java.io.IOException;
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
  private OnOrOff activeMode = OnOrOff.ON;

  public SpringSchedulerFactory (Properties properties)
    throws SchedulerException {

    super(properties);
  }

  public void setActiveMode (OnOrOff activeMode) {

    this.activeMode = activeMode;
  }

  @Override
  public synchronized void setApplicationContext (ApplicationContext applicationContext)
    throws BeansException {

    jobFactory = new SpringJobFactory(applicationContext);
  }

  @Override
  public synchronized void onApplicationEvent (ContextClosedEvent event) {

    try {
      Scheduler scheduler;

      if ((scheduler = super.getScheduler()) != null) {
        scheduler.shutdown(true);
      }
    } catch (SchedulerException schedulerException) {
      LoggerManager.getLogger(SpringSchedulerFactory.class).error(schedulerException);
    }

    if (jobFactory != null) {
      try {
        jobFactory.close();
      } catch (IOException ioException) {
        LoggerManager.getLogger(SpringSchedulerFactory.class).error(ioException);
      }
    }
  }

  @Override
  public synchronized Scheduler getScheduler ()
    throws SchedulerException {

    Scheduler scheduler;

    scheduler = super.getScheduler();
    scheduler.setJobFactory(jobFactory);

    switch (activeMode) {
      case ON:
        scheduler.start();
        break;
      case OFF:
        scheduler.standby();
        break;
      default:
        throw new UnknownSwitchCaseException(activeMode.name());
    }

    return scheduler;
  }
}
