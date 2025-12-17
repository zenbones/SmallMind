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

import java.util.concurrent.TimeUnit;

/**
 * Contract for queue implementations that accept work items and allow timed retrieval.
 *
 * @param <E> type of work items stored in the queue
 */
public interface WorkQueue<E> {

  /**
   * Attempts to enqueue the supplied work item within the provided timeout window.
   *
   * @param e       the work item to enqueue
   * @param timeout maximum time to wait before giving up
   * @param unit    unit for the timeout argument
   * @return {@code true} if the work was accepted, {@code false} if timed out before enqueueing
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  boolean offer (E e, long timeout, TimeUnit unit)
    throws InterruptedException;

  /**
   * Attempts to retrieve a work item, waiting up to the supplied timeout.
   *
   * @param timeout maximum time to wait before returning
   * @param unit    unit for the timeout argument
   * @return the dequeued work item, or {@code null} if the timeout elapsed before one was available
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  E poll (long timeout, TimeUnit unit)
    throws InterruptedException;
}
