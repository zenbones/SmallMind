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
 * Pattern rule that unconditionally emits a fixed literal string, independent of the log record
 * being formatted. Used by {@link PatternFormatter} to represent the literal text segments and
 * the {@code %%} escape between conversion tokens.
 */
public class StaticPatternRule implements PatternRule {

  private final String staticField;

  /**
   * Constructs a rule that always emits the given literal text.
   *
   * @param staticField the literal string to return on every call to {@link #convert}
   */
  public StaticPatternRule (String staticField) {

    this.staticField = staticField;
  }

  /**
   * Returns {@code null} because static rules carry no conditional header text.
   *
   * @return always {@code null}
   */
  public String getHeader () {

    return null;
  }

  /**
   * Returns {@code null} because static rules carry no conditional footer text.
   *
   * @return always {@code null}
   */
  public String getFooter () {

    return null;
  }

  /**
   * Returns the fixed literal string this rule was constructed with, ignoring the record and timestamp entirely.
   *
   * @param record    the log record being formatted (unused)
   * @param timestamp the timestamp provider (unused)
   * @return the literal text supplied at construction time
   */
  public String convert (Record<?> record, Timestamp timestamp) {

    return staticField;
  }
}
