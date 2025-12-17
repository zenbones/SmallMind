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

/**
 * Quartz {@link StdSchedulerFactory} that integrates with Spring lifecycle.
 * It wires in a {@link SpringJobFactory} for job creation, starts or
 * suspends the scheduler based on configuration, and shuts down the
 * scheduler and job factory when the Spring context closes.
 */
public class SpringSchedulerFactory extends StdSchedulerFactory implements ApplicationContextAware, ApplicationListener<ContextClosedEvent> {

  private SpringJobFactory jobFactory;
  private OnOrOff activeMode = OnOrOff.ON;

  /**
   * Construct the factory using the provided Quartz properties.
   *
   * @param properties scheduler configuration properties
   * @throws SchedulerException if the scheduler factory cannot be initialized
   */
  public SpringSchedulerFactory (Properties properties)
    throws SchedulerException {

    super(properties);
  }

  /**
   * Configure whether the scheduler should start immediately ({@link OnOrOff#ON})
   * or remain in standby ({@link OnOrOff#OFF}) when obtained.
   *
   * @param activeMode desired activation mode
   */
  public void setActiveMode (OnOrOff activeMode) {

    this.activeMode = activeMode;
  }

  /**
   * Capture the application context so that job instances can be created
   * via {@link SpringJobFactory}.
   *
   * @param applicationContext Spring application context
   * @throws BeansException if the context cannot be applied
   */
  @Override
  public synchronized void setApplicationContext (ApplicationContext applicationContext)
    throws BeansException {

    jobFactory = new SpringJobFactory(applicationContext);
  }

  /**
   * Respond to Spring context shutdown by stopping the scheduler and closing
   * the job factory to release resources.
   *
   * @param event Spring context closed event
   */
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

  /**
   * Obtain a scheduler configured with the Spring-aware job factory. The
   * scheduler is started or placed in standby based on {@link #activeMode}.
   *
   * @return configured {@link Scheduler}
   * @throws SchedulerException if the scheduler cannot be created or configured
   */
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
