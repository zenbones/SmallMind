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
package org.smallmind.sleuth.runner;

/**
 * Contract for runnable units of work dispatched by {@link SleuthThreadPool}.
 * <p>
 * Implementations ({@link SuiteRunner} and {@link TestRunner}) execute test logic in {@link #run()}
 * and release their semaphore slot, decrement completion latches, and notify dependent nodes in
 * {@link #complete()}. The {@code complete()} method must always be called — even when execution is
 * cancelled or an exception escapes — to prevent the scheduler from deadlocking.
 *
 * @see SuiteRunner
 * @see TestRunner
 * @see SleuthThreadPool
 */
public interface TestController extends Runnable {

  /**
   * Releases all resources held by this controller and notifies the scheduler that this unit
   * of work is done.
   * <p>
   * Specifically, implementations release the tier semaphore permit, mark the corresponding
   * {@link Dependency} complete in the {@link DependencyQueue}, and count down any
   * {@link java.util.concurrent.CountDownLatch} that callers are waiting on. Must be called
   * exactly once per controller instance, even on error or cancellation paths.
   */
  void complete ();
}
