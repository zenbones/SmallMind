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
package org.smallmind.web.http.apache;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.hc.core5.concurrent.FutureCallback;

/**
 * Base {@link FutureCallback} that provides blocking await semantics and defers completion handling to subclasses. The
 * latch ensures callers can wait for completion while allowing async callbacks to signal results or failures.
 *
 * @param <V> result type returned by the HTTP operation
 */
public abstract class HttpCallback<V> implements FutureCallback<V> {

  private final CountDownLatch executedLatch = new CountDownLatch(1);

  /**
   * Invoked when the operation completes successfully.
   *
   * @param v completed value
   */
  public abstract void onCompleted (V v);

  /**
   * Invoked when the operation fails with an exception.
   *
   * @param exception failure cause
   */
  public abstract void onFailed (Exception exception);

  /**
   * Invoked when the operation is cancelled prior to completion.
   */
  public abstract void onCancelled ();

  /**
   * Blocks until the callback executes or a timeout elapses.
   *
   * @param timeout  maximum time to wait
   * @param timeUnit unit for the timeout
   * @throws InterruptedException if the waiting thread is interrupted
   * @throws TimeoutException     if the callback does not complete before the timeout
   */
  public void await (long timeout, TimeUnit timeUnit)
    throws InterruptedException, TimeoutException {

    if (!executedLatch.await(timeout, timeUnit)) {
      throw new TimeoutException();
    }
  }

  /**
   * Signals successful completion to {@link #onCompleted(Object)} and releases any waiters.
   *
   * @param v completed value
   */
  @Override
  public void completed (V v) {

    try {
      onCompleted(v);
    } finally {
      executedLatch.countDown();
    }
  }

  /**
   * Signals failure to {@link #onFailed(Exception)} and releases any waiters.
   *
   * @param e failure cause
   */
  @Override
  public void failed (Exception e) {

    try {
      onFailed(e);
    } finally {
      executedLatch.countDown();
    }
  }

  /**
   * Signals cancellation to {@link #onCancelled()} and releases any waiters.
   */
  @Override
  public void cancelled () {

    try {
      onCancelled();
    } finally {
      executedLatch.countDown();
    }
  }
}
