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
 * Pairs a reference {@link ZonedDateTime} with a {@link TimeOperation} to support reusable temporal comparisons
 * against arbitrary {@link java.time.Instant} values.
 */
public class TimeArithmetic {

  private final ZonedDateTime date;
  private final TimeOperation operation;

  /**
   * Creates a {@code TimeArithmetic} bound to the given reference date and comparison operation.
   *
   * @param date      the reference date-time against which instants will be compared
   * @param operation the comparison operation to apply
   */
  public TimeArithmetic (ZonedDateTime date, TimeOperation operation) {

    this.date = date;
    this.operation = operation;
  }

  /**
   * Returns the reference date-time used in comparisons.
   *
   * @return the reference {@link ZonedDateTime}
   */
  public ZonedDateTime getDate () {

    return date;
  }

  /**
   * Returns the comparison operation applied by {@link #accept(Instant)}.
   *
   * @return the configured {@link TimeOperation}
   */
  public TimeOperation getOperation () {

    return operation;
  }

  /**
   * Evaluates the configured {@link TimeOperation} by comparing the reference date against the given instant.
   *
   * @param instant the instant to compare with the reference date
   * @return {@code true} if the configured operation is satisfied for the given instant
   */
  public boolean accept (Instant instant) {

    return operation.accept(date, instant);
  }
}
