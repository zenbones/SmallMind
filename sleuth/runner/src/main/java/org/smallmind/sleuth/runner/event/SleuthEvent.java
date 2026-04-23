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
 * Abstract base class for all events emitted by the Sleuth test runner during execution.
 * <p>
 * Every event carries the fully qualified class name and method name of the test or lifecycle
 * hook that produced it, along with a {@link SleuthEventType} discriminator. Subclasses extend
 * the event with additional context such as elapsed time, a failure throwable, or a descriptive
 * message.
 * <p>
 * The event hierarchy is:
 * <ul>
 *   <li>{@link SleuthEvent} — class and method identity only</li>
 *   <li>{@link TimedSleuthEvent} — adds elapsed execution time in milliseconds</li>
 *   <li>{@link ThrowableSleuthEvent} — adds a failure or error {@link Throwable}</li>
 *   <li>{@link MessageSleuthEvent} — adds a human-readable reason string</li>
 * </ul>
 * Concrete types: {@link StartSleuthEvent}, {@link SuccessSleuthEvent},
 * {@link FailureSleuthEvent}, {@link ErrorSleuthEvent}, {@link SkippedSleuthEvent},
 * {@link MootSleuthEvent}, {@link SetupSleuthEvent}, {@link CancelledSleuthEvent},
 * {@link FatalSleuthEvent}.
 *
 * @see SleuthEventListener
 * @see SleuthEventType
 */
public abstract class SleuthEvent {

  private final String className;
  private final String methodName;

  /**
   * Constructs an event attributed to the given class and method.
   *
   * @param className  fully qualified name of the class that produced the event; must not be {@code null}
   * @param methodName name of the method that produced the event; may be {@code null} for suite-level events
   */
  public SleuthEvent (String className, String methodName) {

    this.className = className;
    this.methodName = methodName;
  }

  /**
   * Returns the discriminator that identifies the concrete type of this event.
   *
   * @return the {@link SleuthEventType} constant for this event; never {@code null}
   */
  public abstract SleuthEventType getType ();

  /**
   * Returns the ANSI color used when rendering this event to the console.
   * <p>
   * The default returns {@link AnsiColor#DEFAULT}. Subclasses override this to provide
   * distinct colors per event type (e.g., green for success, red for failure).
   *
   * @return ANSI color for console rendering; never {@code null}
   */
  public AnsiColor getColor () {

    return AnsiColor.DEFAULT;
  }

  /**
   * Returns the fully qualified name of the class that produced this event.
   *
   * @return class name; never {@code null}
   */
  public String getClassName () {

    return className;
  }

  /**
   * Returns the name of the method that produced this event.
   *
   * @return method name; may be {@code null} for suite-level events
   */
  public String getMethodName () {

    return methodName;
  }

  /**
   * Returns a human-readable, ANSI-colored string showing the event type, class, and method.
   *
   * @return formatted event description; never {@code null}
   */
  @Override
  public String toString () {

    return getColor().getCode() + getType() + AnsiColor.DEFAULT.getCode() + " [className=" + getClassName() + ", methodName=" + getMethodName() + "]";
  }
}
