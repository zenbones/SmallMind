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
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * {@link JobRegistry} implementation that discovers {@link Job} beans from the Spring
 * {@link ApplicationContext}.
 * <p>
 * Job names are collected during {@link BeanFactoryPostProcessor#postProcessBeanFactory} by
 * scanning all bean definitions for assignability to {@link Job}. The actual bean references
 * are resolved lazily via the context captured on {@link ContextRefreshedEvent}.
 * <p>
 * Manual {@link #register} and {@link #unregister} operations are intentionally unsupported;
 * jobs must be declared as Spring beans.
 */
public class BatchJobRegistry implements JobRegistry, ApplicationListener<ContextRefreshedEvent>, BeanFactoryPostProcessor {

  private final HashSet<String> jobNameSet = new HashSet<>();
  private ApplicationContext applicationContext;

  /**
   * Returns the {@link Job} bean registered under the given name.
   *
   * @param name the Spring bean name of the job
   * @return the resolved job instance; never {@code null}
   */
  @Override
  public Job getJob (String name) {

    return applicationContext.getBean(name, Job.class);
  }

  /**
   * Returns all job names discovered during bean factory post-processing.
   *
   * @return an unmodifiable view of the set of discovered job names
   */
  @Override
  public Collection<String> getJobNames () {

    return Collections.unmodifiableSet(jobNameSet);
  }

  /**
   * Not supported; jobs must be declared as Spring beans.
   *
   * @param job ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public void register (Job job) {

    throw new UnsupportedOperationException("Job instances should be declared in spring context");
  }

  /**
   * Not supported; jobs must be declared as Spring beans.
   *
   * @param jobName ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public void unregister (String jobName) {

    throw new UnsupportedOperationException("Job instances should be declared in spring context - so cannot be 'deregistered'");
  }

  /**
   * Captures the refreshed application context so that {@link #getJob} can retrieve beans.
   *
   * @param event the context-refreshed event carrying the live application context
   */
  @Override
  public void onApplicationEvent (ContextRefreshedEvent event) {

    applicationContext = event.getApplicationContext();
  }

  /**
   * Scans all registered bean definitions and records the names of any beans assignable to
   * {@link Job}.
   *
   * @param configurableListableBeanFactory the bean factory under construction
   * @throws BeansException if bean metadata cannot be read
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
