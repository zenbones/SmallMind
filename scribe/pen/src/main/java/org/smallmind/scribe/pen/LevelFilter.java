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
 * A {@link Filter} that allows only log records whose level is at or above a configured threshold,
 * blocking all records that fall below it.
 */
public class LevelFilter implements Filter {

  private Level level = Level.TRACE;

  /**
   * Constructs a filter with the minimum possible threshold ({@link Level#TRACE}), passing all records.
   */
  public LevelFilter () {

  }

  /**
   * Constructs a filter that passes only records at or above the given level.
   *
   * @param level the minimum {@link Level} a record must have to be allowed through
   */
  public LevelFilter (Level level) {

    this.level = level;
  }

  /**
   * Replaces the current level threshold with the one provided.
   *
   * @param level the new minimum {@link Level} required for a record to pass
   */
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * Returns {@code true} if the record's level is at least as severe as the configured threshold.
   *
   * @param record the log record to evaluate
   * @return {@code true} if the record should be logged; {@code false} if it should be suppressed
   */
  public boolean willLog (Record<?> record) {

    return record.getLevel().atLeast(level);
  }
}
