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

import java.util.concurrent.Executors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.support.TaskExecutorAdapter;

/**
 * Spring {@link FactoryBean} that creates a {@link TaskExecutorAdapter} backed by a scheduled thread pool for batch
 * job execution.
 */
public class BatchJobExecutorFactory implements InitializingBean, FactoryBean<TaskExecutorAdapter> {

  private TaskExecutorAdapter taskExecutorAdapter;
  private int concurrencyLimit = 1;

  /**
   * Sets the concurrency limit used when creating the underlying thread pool.
   *
   * @param concurrencyLimit The configured concurrency limit.
   */
  public void setConcurrencyLimit (int concurrencyLimit) {

    this.concurrencyLimit = Math.min(1, concurrencyLimit);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Class<?> getObjectType () {

    return TaskExecutorAdapter.class;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable TaskExecutorAdapter getObject () {

    return taskExecutorAdapter;
  }

  /**
   * Initializes this factory by creating the {@link TaskExecutorAdapter} instance.
   */
  @Override
  public void afterPropertiesSet () {

    taskExecutorAdapter = new TaskExecutorAdapter(Executors.newScheduledThreadPool(concurrencyLimit));
  }
}
