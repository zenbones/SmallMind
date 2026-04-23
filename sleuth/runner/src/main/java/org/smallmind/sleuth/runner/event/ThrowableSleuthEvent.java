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
 * Intermediate base class for {@link TimedSleuthEvent} subtypes that also carry the throwable
 * responsible for the non-successful outcome.
 * <p>
 * Concrete subclasses distinguish the category of failure:
 * <ul>
 *   <li>{@link FailureSleuthEvent} — the throwable is an {@link AssertionError} (test assertion failed)</li>
 *   <li>{@link ErrorSleuthEvent} — the throwable is any other exception (unexpected runtime error)</li>
 *   <li>{@link MootSleuthEvent} — the throwable signals an unmet assumption (test rendered moot)</li>
 *   <li>{@link FatalSleuthEvent} — the throwable caused the runner itself to halt</li>
 * </ul>
 *
 * @see FailureSleuthEvent
 * @see ErrorSleuthEvent
 * @see MootSleuthEvent
 * @see FatalSleuthEvent
 */
public abstract class ThrowableSleuthEvent extends TimedSleuthEvent {

  private final Throwable throwable;

  /**
   * Constructs a throwable event with the given identity, elapsed time, and cause.
   *
   * @param className  fully qualified name of the class that produced the event; must not be {@code null}
   * @param methodName name of the method that produced the event; may be {@code null} for suite-level events
   * @param elapsed    wall-clock time in milliseconds from method invocation to the point the throwable was caught
   * @param throwable  the exception or error that triggered this event; must not be {@code null}
   */
  public ThrowableSleuthEvent (String className, String methodName, long elapsed, Throwable throwable) {

    super(className, methodName, elapsed);

    this.throwable = throwable;
  }

  /**
   * Returns the throwable that caused this event.
   *
   * @return the exception or error; never {@code null}
   */
  public Throwable getThrowable () {

    return throwable;
  }

  /**
   * Returns a human-readable, ANSI-colored string showing the event type, class, method, elapsed time,
   * and the throwable.
   *
   * @return formatted event description; never {@code null}
   */
  @Override
  public String toString () {

    return getColor().getCode() + getType() + AnsiColor.DEFAULT.getCode() + " [className=" + getClassName() + ", methodName=" + getMethodName() + ", elapsed=" + getElapsed() + ", throwable=" + getThrowable() + "]";
  }
}
