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
package org.smallmind.nutsnbolts.retry;

/**
 * {@link Runnable} adapter used by {@link Retry} to execute a {@link RetryCall} on a
 * dedicated thread, capturing any thrown error so the outcome can be inspected after
 * the thread completes.
 */
public class RetryWorker implements Runnable {

  private final RetryCall retryCall;
  private Throwable throwable;

  /**
   * Constructs a {@code RetryWorker} that will delegate to the given call.
   *
   * @param retryCall the operation to attempt each time this worker is run
   */
  public RetryWorker (RetryCall retryCall) {

    this.retryCall = retryCall;
  }

  /**
   * Clears the throwable captured from the previous attempt, preparing this worker for reuse.
   */
  public void reset () {

    throwable = null;
  }

  /**
   * Returns whether the most recent execution attempt completed without throwing.
   *
   * @return {@code true} if no throwable was captured during the last {@link #run()} call; {@code false} otherwise
   */
  public boolean isSuccess () {

    return throwable == null;
  }

  /**
   * Invokes the wrapped {@link RetryCall}, capturing any {@link Throwable} it throws so
   * that {@link Retry} can determine whether a further attempt is needed.
   */
  @Override
  public void run () {

    try {
      retryCall.execute();
    } catch (Throwable throwable) {
      this.throwable = throwable;
    }
  }
}
