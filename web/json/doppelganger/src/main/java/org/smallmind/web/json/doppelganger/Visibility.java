/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.web.json.doppelganger;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Visibility of a property or view: inbound only, outbound only, or both.
 */
public enum Visibility {

  IN, OUT, BOTH;

  /**
   * Combines this visibility with another, producing {@link #BOTH} when they differ.
   *
   * @param that other visibility value
   * @return composed visibility
   */
  public Visibility compose (Visibility that) {

    return ((that == null) || this.equals(that)) ? this : Visibility.BOTH;
  }

  /**
   * Determines whether this visibility applies to the given direction.
   *
   * @param direction direction to test
   * @return {@code true} if the visibility permits the direction
   */
  public boolean matches (Direction direction) {

    switch (this) {
      case IN:
        return Direction.IN.equals(direction);
      case OUT:
        return Direction.OUT.equals(direction);
      case BOTH:
        return true;
      default:
        throw new UnknownSwitchCaseException(this.name());
    }
  }
}
