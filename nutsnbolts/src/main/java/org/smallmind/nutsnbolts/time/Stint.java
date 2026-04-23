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

import java.util.concurrent.TimeUnit;

/**
 * Immutable value type pairing a non-negative duration with its {@link TimeUnit}.
 */
public class Stint {

  private static final Stint NONE = new Stint(0, TimeUnit.SECONDS);

  private final TimeUnit timeUnit;
  private final long time;

  /**
   * Creates a new {@code Stint} with the given duration and unit.
   *
   * @param time     non-negative duration value
   * @param timeUnit the unit of the duration; must not be {@code null}
   * @throws IllegalArgumentException if {@code time} is negative or {@code timeUnit} is {@code null}
   */
  public Stint (long time, TimeUnit timeUnit) {

    if ((time < 0) || (timeUnit == null)) {
      throw new IllegalArgumentException("Must represent a positive time duration");
    }

    this.time = time;
    this.timeUnit = timeUnit;
  }

  /**
   * Returns a shared zero-length {@code Stint} expressed in seconds.
   *
   * @return the shared zero-duration instance
   */
  public static Stint none () {

    return NONE;
  }

  /**
   * Creates a new {@code Stint}; equivalent to calling the constructor directly.
   *
   * @param time     non-negative duration value
   * @param timeUnit the unit of the duration; must not be {@code null}
   * @return a new {@code Stint} with the specified time and unit
   */
  public static Stint of (long time, TimeUnit timeUnit) {

    return new Stint(time, timeUnit);
  }

  /**
   * Returns the numeric duration value.
   *
   * @return the duration expressed in {@link #getTimeUnit()}
   */
  public long getTime () {

    return time;
  }

  /**
   * Returns the unit in which the duration value is expressed.
   *
   * @return the {@link TimeUnit} for this stint
   */
  public TimeUnit getTimeUnit () {

    return timeUnit;
  }

  /**
   * Converts this duration to milliseconds.
   *
   * @return the duration expressed in milliseconds
   */
  public long toMilliseconds () {

    return timeUnit.toMillis(time);
  }
}
