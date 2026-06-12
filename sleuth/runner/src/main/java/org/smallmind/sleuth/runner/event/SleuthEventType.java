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
package org.smallmind.sleuth.runner.event;

/**
 * Discriminator enum that identifies the concrete type of a {@link SleuthEvent}.
 * <p>
 * Each constant corresponds to exactly one concrete event class and one well-defined phase of
 * test execution. {@link SleuthEventListener} implementations can switch on this value to dispatch
 * to type-specific handlers without relying on {@code instanceof} checks.
 *
 * @see SleuthEvent#getType()
 * @see SleuthEventListener
 */
public enum SleuthEventType {

  /**
   * Indicates that a test method or lifecycle hook has begun execution.
   * Carried by {@link StartSleuthEvent}.
   */
  START,

  /**
   * Indicates that a test method completed without throwing any exception.
   * Carried by {@link SuccessSleuthEvent}.
   */
  SUCCESS,

  /**
   * Indicates that a test method threw an {@link AssertionError}, distinguishing an explicit
   * assertion failure from an unexpected runtime exception.
   * Carried by {@link FailureSleuthEvent}.
   */
  FAILURE,

  /**
   * Indicates that a test method threw an exception other than {@link AssertionError}.
   * Carried by {@link ErrorSleuthEvent}.
   */
  ERROR,

  /**
   * Indicates that a test was bypassed without execution because a prior failure, error,
   * or unmet dependency made running it unsafe or pointless.
   * Carried by {@link SkippedSleuthEvent}.
   */
  SKIPPED,

  /**
   * Indicates an unrecoverable internal error that halts the runner entirely.
   * Carried by {@link FatalSleuthEvent}.
   */
  FATAL,

  /**
   * Indicates that suite execution was explicitly cancelled via
   * {@link org.smallmind.sleuth.runner.SleuthRunner#cancel()}.
   * Carried by {@link CancelledSleuthEvent}.
   */
  CANCELLED
}
