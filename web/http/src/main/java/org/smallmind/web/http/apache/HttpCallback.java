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
 * Abstract {@link FutureCallback} that adds blocking await semantics and delegates completion, failure, and
 * cancellation handling to subclasses.
 *
 * @param <V> result type produced by the HTTP operation
 */
public abstract class HttpCallback<V> implements FutureCallback<V> {

  private final CountDownLatch executedLatch = new CountDownLatch(1);

  /**
   * Called when the asynchronous operation completes successfully.
   *
   * @param v the completed result value
   */
  public abstract void onCompleted (V v);

  /**
   * Called when the asynchronous operation fails.
   *
   * @param exception the exception that caused the failure
   */
  public abstract void onFailed (Exception exception);

  /**
   * Called when the asynchronous operation is cancelled before completing.
   */
  public abstract void onCancelled ();

  /**
   * Blocks the calling thread until the callback finishes executing or the timeout expires.
   *
   * @param timeout  maximum time to wait
   * @param timeUnit unit of the timeout value
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws TimeoutException     if the timeout elapses before the callback completes
   */
  public void await (long timeout, TimeUnit timeUnit)
    throws InterruptedException, TimeoutException {

    if (!executedLatch.await(timeout, timeUnit)) {
      throw new TimeoutException();
    }
  }

  /**
   * Forwards the result to {@link #onCompleted(Object)} and releases any threads blocked in {@link #await}.
   *
   * @param v the completed result value
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
   * Forwards the failure to {@link #onFailed(Exception)} and releases any threads blocked in {@link #await}.
   *
   * @param e the exception that caused the failure
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
   * Notifies {@link #onCancelled()} and releases any threads blocked in {@link #await}.
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
