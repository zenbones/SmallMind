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
package org.smallmind.phalanx.worker;

/**
 * Factory interface for producing {@link Worker} instances bound to a specific {@link WorkQueue}.
 *
 * <p>Implementations are supplied to {@link WorkManager#startUp} and are called once per worker
 * slot in the pool.</p>
 *
 * @param <W> the concrete {@link Worker} subtype this factory creates
 * @param <T> the type of work items consumed by the workers
 */
public interface WorkerFactory<W extends Worker<T>, T> {

  /**
   * Creates a new worker instance that will read work items from the given queue.
   *
   * @param workQueue the queue the created worker should poll for work
   * @return a fully initialised but not yet started worker instance
   */
  W createWorker (WorkQueue<T> workQueue);
}
