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
 * Event fired immediately before a test method or lifecycle hook begins execution.
 * <p>
 * Emitted by {@link org.smallmind.sleuth.runner.TestRunner} just before the test body is
 * about to run (after before-test hooks) and by
 * {@link org.smallmind.sleuth.runner.annotation.AnnotationMethodology} before each lifecycle
 * method invocation. A corresponding outcome event ({@link SuccessSleuthEvent},
 * {@link FailureSleuthEvent}, {@link ErrorSleuthEvent}, or {@link SkippedSleuthEvent}) always
 * follows for the same class/method pair.
 *
 * @see SuccessSleuthEvent
 * @see FailureSleuthEvent
 * @see ErrorSleuthEvent
 * @see SkippedSleuthEvent
 */
public class StartSleuthEvent extends SleuthEvent {

  /**
   * Constructs a start event for the given test.
   *
   * @param className  fully qualified name of the test class; must not be {@code null}
   * @param methodName name of the test or lifecycle method beginning execution; must not be {@code null}
   */
  public StartSleuthEvent (String className, String methodName) {

    super(className, methodName);
  }

  /**
   * Returns {@link SleuthEventType#START}.
   *
   * @return {@link SleuthEventType#START}
   */
  @Override
  public SleuthEventType getType () {

    return SleuthEventType.START;
  }

  /**
   * Returns bright blue, used to make start events visually distinct on the console.
   *
   * @return {@link AnsiColor#BRIGHT_BLUE}
   */
  @Override
  public AnsiColor getColor () {

    return AnsiColor.BRIGHT_BLUE;
  }
}
