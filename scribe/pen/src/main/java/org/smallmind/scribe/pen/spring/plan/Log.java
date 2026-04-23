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
package org.smallmind.scribe.pen.spring.plan;

import org.smallmind.scribe.pen.Level;

/**
 * Data-transfer object that pairs a {@link Level} threshold with a class-name pattern string, used by
 * {@link LoggingPlan} to register per-logger {@link org.smallmind.scribe.pen.ClassNameTemplate} instances.
 */
public class Log {

  private Level level;
  private String pattern;

  /**
   * Constructs a {@code Log} with no level or pattern set; both must be supplied before the instance is used.
   */
  public Log () {

  }

  /**
   * Constructs a {@code Log} with the given level threshold and class-name pattern.
   *
   * @param level   the minimum {@link Level} that loggers matching this entry will emit
   * @param pattern the class-name glob or prefix pattern identifying which loggers this entry applies to
   */
  public Log (Level level, String pattern) {

    this.level = level;
    this.pattern = pattern;
  }

  /**
   * Returns the minimum level threshold configured for this log entry.
   *
   * @return the {@link Level} associated with this entry
   */
  public Level getLevel () {

    return level;
  }

  /**
   * Sets the minimum level threshold that loggers matching the pattern of this entry will apply.
   *
   * @param level the {@link Level} to associate with this entry
   */
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * Returns the class-name pattern that identifies which loggers this entry governs.
   *
   * @return the pattern string used to match logger names
   */
  public String getPattern () {

    return pattern;
  }

  /**
   * Sets the class-name pattern that identifies which loggers this entry governs.
   *
   * @param pattern the pattern string used to match logger names
   */
  public void setPattern (String pattern) {

    this.pattern = pattern;
  }
}
