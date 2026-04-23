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
 * Event fired when a test method throws an exception other than {@link AssertionError}.
 * <p>
 * Emitted by {@link org.smallmind.sleuth.runner.TestRunner} when the exception unwrapped from
 * a {@link java.lang.reflect.InvocationTargetException} is not an {@link AssertionError}, or
 * when any other unexpected exception escapes the method call. Explicit assertion failures are
 * reported as {@link FailureSleuthEvent} instead.
 * <p>
 * If the runner's {@code stopOnError} flag is set, {@link org.smallmind.sleuth.runner.SleuthRunner#cancel()}
 * is called before this event is fired.
 *
 * @see FailureSleuthEvent
 * @see org.smallmind.sleuth.runner.TestRunner
 */
public class ErrorSleuthEvent extends ThrowableSleuthEvent {

  /**
   * Constructs an error event for the given test and cause.
   *
   * @param className  fully qualified name of the test class; must not be {@code null}
   * @param methodName name of the test method that errored; must not be {@code null}
   * @param elapsed    wall-clock duration of the test method invocation in milliseconds; non-negative
   * @param throwable  the unexpected exception that caused the error; must not be {@code null}
   */
  public ErrorSleuthEvent (String className, String methodName, long elapsed, Throwable throwable) {

    super(className, methodName, elapsed, throwable);
  }

  /**
   * Returns {@link SleuthEventType#ERROR}.
   *
   * @return {@link SleuthEventType#ERROR}
   */
  @Override
  public SleuthEventType getType () {

    return SleuthEventType.ERROR;
  }

  /**
   * Returns bright red, used to make unexpected errors visually prominent on the console.
   *
   * @return {@link AnsiColor#BRIGHT_RED}
   */
  @Override
  public AnsiColor getColor () {

    return AnsiColor.BRIGHT_RED;
  }
}
