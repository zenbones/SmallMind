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
package org.smallmind.nutsnbolts.util;

/**
 * Lifecycle status values with a numeric priority for ordering.
 */
public enum ComponentStatus {

  INITIALIZING(4), INITIALIZED(5), STARTING(8), STARTED(9), STOPPING(7), STOPPED(6), TERMINATING(2), TERMINATED(1), UNINITIALIZED(3), UNKNOWN(0);

  private final int priority;

  /**
   * @param priority priority value used for ordering
   */
  ComponentStatus (int priority) {

    this.priority = priority;
  }

  /**
   * @return priority value
   */
  public int getPriority () {

    return priority;
  }

  /**
   * Tests whether this status is one of the provided values.
   *
   * @param matchingStatuses statuses to compare against
   * @return {@code true} if this status is contained in the list
   */
  public boolean in (ComponentStatus... matchingStatuses) {

    if (matchingStatuses != null) {
      for (ComponentStatus matchingStatus : matchingStatuses) {
        if (this.equals(matchingStatus)) {

          return true;
        }
      }
    }

    return false;
  }
}
