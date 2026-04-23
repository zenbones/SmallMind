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
package org.smallmind.nutsnbolts.util;

import java.util.Comparator;

/**
 * Comparator for dot-separated strings that compares segments one at a time using {@link AlphaNumericComparator},
 * with optional right-to-left segment ordering.
 */
public class DotNotationComparator implements Comparator<String> {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<String>();

  private final boolean reversed;

  /**
   * Constructs a comparator that compares segments from left to right.
   */
  public DotNotationComparator () {

    this(false);
  }

  /**
   * Constructs a comparator with configurable segment traversal direction.
   *
   * @param reversed {@code true} to compare segments from the rightmost side first; {@code false} for left-to-right
   */
  public DotNotationComparator (boolean reversed) {

    this.reversed = reversed;
  }

  /**
   * Splits both strings on dots and compares each corresponding segment using alphanumeric ordering;
   * when all shared segments are equal, the string with fewer segments sorts first.
   *
   * @param string1 the first dot-separated string
   * @param string2 the second dot-separated string
   * @return a negative integer, zero, or positive integer per the {@link Comparator} contract
   */
  @Override
  public int compare (String string1, String string2) {

    String[] segments1 = string1.split("\\.", -1);
    String[] segments2 = string2.split("\\.", -1);
    int commonSegments = Math.min(segments1.length, segments2.length);
    int comparison;

    for (int index = 0; index < commonSegments; index++) {
      if ((comparison = ALPHA_NUMERIC_COMPARATOR.compare(segments1[reversed ? segments1.length - (index + 1) : index], segments2[reversed ? segments2.length - (index + 1) : index])) != 0) {

        return comparison;
      }
    }

    return (segments1.length == segments2.length) ? 0 : (segments1.length > segments2.length) ? 1 : -1;
  }
}
