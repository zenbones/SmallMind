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
 * Single composable rule used by pattern-based formatters to extract and render one fragment of a log record,
 * optionally surrounded by a static header and footer.
 */
public interface PatternRule {

  /**
   * Returns the static text that is emitted immediately before the converted fragment, if any.
   *
   * @return the header string, or {@code null} if there is no header
   */
  String getHeader ();

  /**
   * Returns the static text that is emitted immediately after the converted fragment, if any.
   *
   * @return the footer string, or {@code null} if there is no footer
   */
  String getFooter ();

  /**
   * Extracts and formats the portion of the record that this rule is responsible for rendering.
   *
   * @param record    the log record from which to extract information
   * @param timestamp the timestamp strategy to use when formatting date/time values
   * @return the formatted fragment; never {@code null}
   */
  String convert (Record<?> record, Timestamp timestamp);
}
