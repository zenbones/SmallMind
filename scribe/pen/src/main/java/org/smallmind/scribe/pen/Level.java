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
package org.smallmind.scribe.pen;

/**
 * Ordered enumeration of severity levels recognized by the scribe logging system, ranging from least severe
 * ({@link #TRACE}) to most severe ({@link #FATAL}), with {@link #OFF} used to suppress all output.
 */
public enum Level {

  TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF;

  /**
   * Returns whether this level is at least as severe as the supplied level, that is, whether this level's
   * ordinal position is greater than or equal to the supplied level's ordinal position.
   *
   * @param level the level to compare against; must not be {@code null}
   * @return {@code true} if this level is equally or more severe than {@code level}
   */
  public boolean atLeast (Level level) {

    return this.ordinal() >= level.ordinal();
  }

  /**
   * Returns whether this level is no more severe than the supplied level, that is, whether this level's
   * ordinal position is less than or equal to the supplied level's ordinal position.
   *
   * @param level the level to compare against; must not be {@code null}
   * @return {@code true} if this level is equally or less severe than {@code level}
   */
  public boolean noGreater (Level level) {

    return this.ordinal() <= level.ordinal();
  }
}
