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
package org.smallmind.quorum.pool.complex;

/**
 * Metric event labels used as Claxon tag values when the complex component pool instruments
 * its operations.
 * <p>
 * Each constant represents a distinct pool event category:
 * <ul>
 *   <li>{@link #PROCESSING} — components currently checked out by callers</li>
 *   <li>{@link #FREE} — components sitting idle on the free queue</li>
 *   <li>{@link #TIMEOUT} — acquisition attempts that exceeded the wait limit</li>
 *   <li>{@link #RELEASED} — components that have been returned to the pool, tagged with lease
 *       duration measurements</li>
 *   <li>{@link #WAITED} — acquisition calls that had to wait before a component became
 *       available</li>
 * </ul>
 */
public enum ClaxonTag {

  PROCESSING("Processing"), FREE("Free"), TIMEOUT("Timeout"), RELEASED("Released"), WAITED("Waited");

  private final String display;

  /**
   * Associates the enum constant with its human-readable display label.
   *
   * @param display the label emitted in Claxon metric tag values
   */
  ClaxonTag (String display) {

    this.display = display;
  }

  /**
   * Returns the human-readable label for this tag, suitable for use as a Claxon metric tag value.
   *
   * @return the display label string
   */
  public String getDisplay () {

    return display;
  }
}
