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
package org.smallmind.batch.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Job locator that captures Spring Batch job beans during context initialization and exposes them to the framework.
 */
public class BatchJobLocator implements ListableJobLocator, ApplicationListener<ContextRefreshedEvent>, BeanFactoryPostProcessor {

  private final HashSet<String> jobNameSet = new HashSet<>();
  private ApplicationContext applicationContext;

  /**
   * Stores the refreshed application context so jobs can be retrieved later.
   *
   * @param event the context refreshed event
   */
  @Override
  public void onApplicationEvent (ContextRefreshedEvent event) {

    applicationContext = event.getApplicationContext();
  }

  /**
   * Retrieves a job bean by name from the application context.
   *
   * @param name the bean name corresponding to a {@link Job}
   * @return the resolved job instance
   */
  @Override
  public Job getJob (String name) {

    return applicationContext.getBean(name, Job.class);
  }

  /**
   * Returns all discovered job names.
   *
   * @return an unmodifiable collection of job names
   */
  @Override
  public Collection<String> getJobNames () {

    return Collections.unmodifiableSet(jobNameSet);
  }

  /**
   * Captures all {@link Job} bean names during bean factory post processing.
   *
   * @param configurableListableBeanFactory the bean factory being processed
   * @throws BeansException if the bean factory cannot be inspected
   */
  @Override
  public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
    throws BeansException {

    for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {

      Class<?> beanClass;

      if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
        if (Job.class.isAssignableFrom(beanClass)) {
          jobNameSet.add(beanName);
        }
      }
    }
  }
}
