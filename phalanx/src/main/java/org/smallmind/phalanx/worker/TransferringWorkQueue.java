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

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link WorkQueue} implementation backed by a {@link LinkedTransferQueue} that prefers direct hand-offs to waiting consumers.
 *
 * <p>{@code offer} uses {@link LinkedTransferQueue#tryTransfer} so that the element is delivered directly to a
 * consumer thread when one is available, falling back to queuing only if the timeout elapses without a
 * consumer.  {@code poll} performs an ordinary timed retrieval.</p>
 *
 * @param <E> type of work items stored in this queue
 */
public class TransferringWorkQueue<E> implements WorkQueue<E> {

  private final LinkedTransferQueue<E> linkedTransferQueue = new LinkedTransferQueue<>();

  /**
   * Attempts to transfer the element directly to a waiting consumer, or enqueues it if one becomes available
   * within the timeout window.
   *
   * @param e       the work item to transfer or enqueue
   * @param timeout maximum time to wait for a consumer to accept the element
   * @param unit    time unit of the {@code timeout} argument
   * @return {@code true} if the element was accepted by a consumer; {@code false} if the timeout elapsed
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  @Override
  public boolean offer (E e, long timeout, TimeUnit unit)
    throws InterruptedException {

    return linkedTransferQueue.tryTransfer(e, timeout, unit);
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

    return linkedTransferQueue.poll(timeout, unit);
  }
}
