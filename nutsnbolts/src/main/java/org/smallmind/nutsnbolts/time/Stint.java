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
 * Represents a non-negative duration measured in a specific {@link TimeUnit}.
 */
public class Stint {

  private static final Stint NONE = new Stint(0, TimeUnit.SECONDS);

  private final TimeUnit timeUnit;
  private final long time;

  /**
   * Creates a new duration.
   *
   * @param time     duration value; must be non-negative
   * @param timeUnit unit of the duration; must not be {@code null}
   * @throws IllegalArgumentException if the time is negative or the unit is {@code null}
   */
  public Stint (long time, TimeUnit timeUnit) {

    if ((time < 0) || (timeUnit == null)) {
      throw new IllegalArgumentException("Must represent a positive time duration");
    }

    this.time = time;
    this.timeUnit = timeUnit;
  }

  /**
   * Returns a shared representation of a zero-length duration.
   *
   * @return zero stint instance
   */
  public static Stint none () {

    return NONE;
  }

  /**
   * Factory method mirroring the constructor.
   *
   * @param time     duration value; must be non-negative
   * @param timeUnit unit of the duration; must not be {@code null}
   * @return newly created stint
   */
  public static Stint of (long time, TimeUnit timeUnit) {

    return new Stint(time, timeUnit);
  }

  /**
   * @return numeric duration value
   */
  public long getTime () {

    return time;
  }

  /**
   * @return unit associated with the duration value
   */
  public TimeUnit getTimeUnit () {

    return timeUnit;
  }

  /**
   * Converts the duration into milliseconds.
   *
   * @return duration expressed in milliseconds
   */
  public long toMilliseconds () {

    return timeUnit.toMillis(time);
  }
}
