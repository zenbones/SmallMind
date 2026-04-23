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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link WorkQueue} implementation backed by a {@link LinkedBlockingQueue}.
 *
 * <p>Supports both bounded and unbounded configurations.  {@code offer} enqueues by waiting up to
 * the specified timeout; {@code poll} dequeues by waiting up to the specified timeout.</p>
 *
 * @param <E> type of work items stored in this queue
 */
public class BlockingWorkQueue<E> implements WorkQueue<E> {

  private final LinkedBlockingQueue<E> linkedBlockingQueue;

  /**
   * Creates an unbounded blocking queue with no capacity constraint.
   */
  public BlockingWorkQueue () {

    linkedBlockingQueue = new LinkedBlockingQueue<>();
  }

  /**
   * Creates a bounded blocking queue that holds at most {@code capacity} elements.
   *
   * @param capacity maximum number of elements the queue may hold at one time
   */
  public BlockingWorkQueue (int capacity) {

    linkedBlockingQueue = new LinkedBlockingQueue<>(capacity);
  }

  /**
   * Inserts the specified element into the queue, waiting up to the given timeout if the queue is full.
   *
   * @param e       the work item to enqueue
   * @param timeout maximum time to wait for space to become available
   * @param unit    time unit of the {@code timeout} argument
   * @return {@code true} if the element was accepted; {@code false} if the timeout elapsed before space was available
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  @Override
  public boolean offer (E e, long timeout, TimeUnit unit)
    throws InterruptedException {

    return linkedBlockingQueue.offer(e, timeout, unit);
  }

  /**
   * Retrieves and removes the head of the queue, waiting up to the given timeout if the queue is empty.
   *
   * @param timeout maximum time to wait for an element to become available
   * @param unit    time unit of the {@code timeout} argument
   * @return the head element, or {@code null} if the timeout elapsed before an element was available
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  @Override
  public E poll (long timeout, TimeUnit unit)
    throws InterruptedException {

    return linkedBlockingQueue.poll(timeout, unit);
  }
}
