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

import org.smallmind.nutsnbolts.util.AnsiColor;

/**
 * Event fired when a test becomes moot because a precondition or assumption was not satisfied.
 * <p>
 * A moot test is neither a pass nor a failure: the test ran far enough to discover that its
 * preconditions do not hold, but the test body was not executed. The attached throwable
 * carries the signal (e.g., a violated assumption) that caused the moot state. Surefire maps
 * this outcome to an assumption failure ({@code testAssumptionFailure}).
 *
 * @see SkippedSleuthEvent
 * @see FailureSleuthEvent
 */
public class MootSleuthEvent extends ThrowableSleuthEvent {

  /**
   * Constructs a moot event for the given test.
   *
   * @param className  fully qualified name of the test class; must not be {@code null}
   * @param methodName name of the test method rendered moot; must not be {@code null}
   * @param elapsed    wall-clock time in milliseconds up to the point the moot condition was detected; non-negative
   * @param throwable  the exception or signal that caused the test to be considered moot; must not be {@code null}
   */
  public MootSleuthEvent (String className, String methodName, long elapsed, Throwable throwable) {

    super(className, methodName, elapsed, throwable);
  }

  /**
   * Returns {@link SleuthEventType#MOOT}.
   *
   * @return {@link SleuthEventType#MOOT}
   */
  @Override
  public SleuthEventType getType () {

    return SleuthEventType.MOOT;
  }

  /**
   * Returns yellow, used to distinguish moot tests from hard failures and successes on the console.
   *
   * @return {@link AnsiColor#YELLOW}
   */
  @Override
  public AnsiColor getColor () {

    return AnsiColor.YELLOW;
  }
}
