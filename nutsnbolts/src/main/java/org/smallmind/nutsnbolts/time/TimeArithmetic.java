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
package org.smallmind.nutsnbolts.time;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Encapsulates a date and {@link TimeOperation} for reusable comparisons against instants.
 */
public class TimeArithmetic {

  private final ZonedDateTime date;
  private final TimeOperation operation;

  /**
   * Constructs a comparison helper for a specific date and operation.
   *
   * @param date      reference date-time
   * @param operation comparison operation to apply
   */
  public TimeArithmetic (ZonedDateTime date, TimeOperation operation) {

    this.date = date;
    this.operation = operation;
  }

  /**
   * @return reference date-time
   */
  public ZonedDateTime getDate () {

    return date;
  }

  /**
   * @return comparison operation
   */
  public TimeOperation getOperation () {

    return operation;
  }

  /**
   * Evaluates the configured operation against the supplied instant.
   *
   * @param instant instant to compare with the reference date
   * @return {@code true} if the operation accepts the instant
   */
  public boolean accept (Instant instant) {

    return operation.accept(date, instant);
  }
}
