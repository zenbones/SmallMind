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
 * Enumeration of binary temporal comparison operations that test a reference {@link ZonedDateTime} relative
 * to a candidate {@link Instant}.
 */
public enum TimeOperation {

  BEFORE {
    /**
     * Returns {@code true} when the reference date is strictly after the candidate instant,
     * meaning the instant falls before the reference date.
     *
     * @param date    the reference date-time
     * @param instant the candidate instant to test
     * @return {@code true} if {@code instant} is before {@code date}
     */
    @Override
    public boolean accept (ZonedDateTime date, Instant instant) {

      return date.toInstant().isAfter(instant);
    }
  }, BEFORE_OR_ON {
    /**
     * Returns {@code true} when the reference date is after or equal to the candidate instant,
     * meaning the instant falls on or before the reference date.
     *
     * @param date    the reference date-time
     * @param instant the candidate instant to test
     * @return {@code true} if {@code instant} is on or before {@code date}
     */
    @Override
    public boolean accept (ZonedDateTime date, Instant instant) {

      Instant dateInstant;

      return (dateInstant = date.toInstant()).equals(instant) || dateInstant.isAfter(instant);
    }
  }, ON_OR_AFTER {
    /**
     * Returns {@code true} when the reference date is before or equal to the candidate instant,
     * meaning the instant falls on or after the reference date.
     *
     * @param date    the reference date-time
     * @param instant the candidate instant to test
     * @return {@code true} if {@code instant} is on or after {@code date}
     */
    @Override
    public boolean accept (ZonedDateTime date, Instant instant) {

      Instant dateInstant;

      return (dateInstant = date.toInstant()).isBefore(instant) || dateInstant.equals(instant);
    }
  }, AFTER {
    /**
     * Returns {@code true} when the reference date is strictly before the candidate instant,
     * meaning the instant falls after the reference date.
     *
     * @param date    the reference date-time
     * @param instant the candidate instant to test
     * @return {@code true} if {@code instant} is after {@code date}
     */
    @Override
    public boolean accept (ZonedDateTime date, Instant instant) {

      return date.toInstant().isBefore(instant);
    }
  };

  /**
   * Evaluates this temporal comparison between a reference date-time and a candidate instant.
   *
   * @param date    the reference date-time
   * @param instant the candidate instant to compare against the reference
   * @return {@code true} if the comparison represented by this constant is satisfied
   */
  public abstract boolean accept (ZonedDateTime date, Instant instant);
}
