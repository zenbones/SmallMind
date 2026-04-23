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
 * Intermediate base class for {@link SleuthEvent} subtypes that record elapsed execution time.
 * <p>
 * Elapsed time is measured from immediately before the test or lifecycle method is invoked to
 * immediately after it completes or throws. All outcome events ({@link SuccessSleuthEvent},
 * {@link FailureSleuthEvent}, {@link ErrorSleuthEvent}, {@link SkippedSleuthEvent},
 * {@link MootSleuthEvent}, {@link FatalSleuthEvent}) extend this class.
 *
 * @see ThrowableSleuthEvent
 * @see MessageSleuthEvent
 */
public abstract class TimedSleuthEvent extends SleuthEvent {

  private final long elapsed;

  /**
   * Constructs a timed event with the given identity and elapsed duration.
   *
   * @param className  fully qualified name of the class that produced the event; must not be {@code null}
   * @param methodName name of the method that produced the event; may be {@code null} for suite-level events
   * @param elapsed    wall-clock time in milliseconds from method invocation to completion or throw
   */
  public TimedSleuthEvent (String className, String methodName, long elapsed) {

    super(className, methodName);

    this.elapsed = elapsed;
  }

  /**
   * Returns the wall-clock duration of the test or lifecycle method invocation.
   *
   * @return elapsed time in milliseconds; non-negative
   */
  public long getElapsed () {

    return elapsed;
  }

  /**
   * Returns a human-readable, ANSI-colored string showing the event type, class, method, and elapsed time.
   *
   * @return formatted event description; never {@code null}
   */
  @Override
  public String toString () {

    return getColor().getCode() + getType() + AnsiColor.DEFAULT.getCode() + " [className=" + getClassName() + ", methodName=" + getMethodName() + ", elapsed=" + getElapsed() + ", elapsed=" + getElapsed() + "]";
  }
}
