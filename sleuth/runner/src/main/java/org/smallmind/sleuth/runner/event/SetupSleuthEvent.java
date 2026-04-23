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
 * Event fired when suite-level or test-level setup is about to begin.
 * <p>
 * Emitted before executing {@link org.smallmind.sleuth.runner.annotation.BeforeSuite} or
 * {@link org.smallmind.sleuth.runner.annotation.BeforeTest} lifecycle methods to allow
 * listeners to mark the beginning of the setup phase in reports. The Surefire integration
 * maps this event to a {@code testSetStarting} notification.
 *
 * @see StartSleuthEvent
 * @see org.smallmind.sleuth.runner.annotation.BeforeSuite
 * @see org.smallmind.sleuth.runner.annotation.BeforeTest
 */
public class SetupSleuthEvent extends SleuthEvent {

  /**
   * Constructs a setup event attributed to the given class and method.
   *
   * @param className  fully qualified name of the class whose setup is starting; must not be {@code null}
   * @param methodName name of the lifecycle setup method about to run; must not be {@code null}
   */
  public SetupSleuthEvent (String className, String methodName) {

    super(className, methodName);
  }

  /**
   * Returns {@link SleuthEventType#SETUP}.
   *
   * @return {@link SleuthEventType#SETUP}
   */
  @Override
  public SleuthEventType getType () {

    return SleuthEventType.SETUP;
  }

  /**
   * Returns bright magenta, used to visually distinguish setup events from test outcome events.
   *
   * @return {@link AnsiColor#BRIGHT_MAGENTA}
   */
  @Override
  public AnsiColor getColor () {

    return AnsiColor.BRIGHT_MAGENTA;
  }
}
