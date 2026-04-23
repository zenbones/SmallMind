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
 * Event fired when an unhandled exception escapes the {@link org.smallmind.sleuth.runner.SuiteRunner}
 * and halts execution of the affected suite.
 * <p>
 * Emitted by {@link org.smallmind.sleuth.runner.SuiteRunner} in its outer {@code catch} block when
 * an exception propagates out of the suite lifecycle (test instantiation, dependency analysis,
 * or uncaught errors in the run loop). The Surefire integration captures the throwable and
 * re-throws it as a {@code TestSetFailedException} after the run completes, causing Maven to
 * report a build failure. If {@code stopOnError} is set, the runner is also cancelled.
 *
 * @see org.smallmind.sleuth.runner.SuiteRunner
 * @see CancelledSleuthEvent
 */
public class FatalSleuthEvent extends ThrowableSleuthEvent {

  /**
   * Constructs a fatal event for the suite runner that encountered the unhandled exception.
   *
   * @param className  fully qualified name of the runner class that caught the exception; must not be {@code null}
   * @param methodName name of the method from which the exception escaped; must not be {@code null}
   * @param elapsed    wall-clock time in milliseconds from suite start to the point the exception was caught; non-negative
   * @param throwable  the unhandled exception that terminated the suite; must not be {@code null}
   */
  public FatalSleuthEvent (String className, String methodName, long elapsed, Throwable throwable) {

    super(className, methodName, elapsed, throwable);
  }

  /**
   * Returns {@link SleuthEventType#FATAL}.
   *
   * @return {@link SleuthEventType#FATAL}
   */
  @Override
  public SleuthEventType getType () {

    return SleuthEventType.FATAL;
  }

  /**
   * Returns bright red, emphasizing the severity of a fatal runner failure.
   *
   * @return {@link AnsiColor#BRIGHT_RED}
   */
  @Override
  public AnsiColor getColor () {

    return AnsiColor.BRIGHT_RED;
  }
}
