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
 * Event fired when a test is bypassed without execution because of a prior failure or unmet dependency.
 * <p>
 * Emitted by {@link org.smallmind.sleuth.runner.TestRunner} when a non-null
 * {@link org.smallmind.sleuth.runner.Culprit} is present at the point the test would otherwise
 * run. This can occur when a suite-level before-hook fails, when a test that the current test
 * {@code dependsOn} previously errored, or when the {@link org.smallmind.sleuth.runner.DependencyQueue}
 * propagates a culprit from a failed predecessor. The message field describes which culprit
 * triggered the skip.
 *
 * @see org.smallmind.sleuth.runner.TestRunner
 * @see org.smallmind.sleuth.runner.Culprit
 */
public class SkippedSleuthEvent extends MessageSleuthEvent {

  /**
   * Constructs a skipped event for the given test.
   *
   * @param className  fully qualified name of the test class; must not be {@code null}
   * @param methodName name of the test method that was skipped; must not be {@code null}
   * @param elapsed    time elapsed before the skip decision was made, in milliseconds; typically zero
   * @param message    human-readable explanation of the skip reason; must not be {@code null}
   */
  public SkippedSleuthEvent (String className, String methodName, long elapsed, String message) {

    super(className, methodName, elapsed, message);
  }

  /**
   * Returns {@link SleuthEventType#SKIPPED}.
   *
   * @return {@link SleuthEventType#SKIPPED}
   */
  @Override
  public SleuthEventType getType () {

    return SleuthEventType.SKIPPED;
  }

  /**
   * Returns yellow, used to distinguish skipped tests from passed and failed tests on the console.
   *
   * @return {@link AnsiColor#YELLOW}
   */
  @Override
  public AnsiColor getColor () {

    return AnsiColor.YELLOW;
  }
}
