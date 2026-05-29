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
 * Comparator for dot-separated strings that compares segments one at a time using {@link AlphaNumericComparator}.
 *
 * <p>By default, segments are compared left-to-right, so the leftmost segment is the most significant sort key.
 * When constructed with {@code reversed = true}, segments are instead compared right-to-left, making the
 * rightmost segment the most significant key &mdash; useful for sorting domain-like values where the suffix
 * (such as a TLD) should group together regardless of prefix. In reversed mode, segments of unequal-length
 * inputs are aligned at the right: when {@code "a.foo.com"} is compared against {@code "z.com"}, the
 * rightmost {@code "com"} segments are compared first.
 *
 * <p>The {@code reversed} flag only changes the segment iteration order; it does <strong>not</strong> invert
 * the sign of the comparison result. The natural ordering of individual segments still applies via
 * {@link AlphaNumericComparator}, which sorts digits before letters and compares letters case-insensitively.
 *
 * <p>This comparator does not accept null arguments and throws {@link NullPointerException} if either input
 * is null.
 */
public class DotNotationComparator implements Comparator<String> {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<String>();

  private final boolean reversed;

  /**
   * Constructs a comparator that compares segments left-to-right, treating the leftmost segment as the most significant.
   */
  public DotNotationComparator () {

    this(false);
  }

  /**
   * Constructs a comparator with configurable segment traversal direction.
   *
   * @param reversed {@code true} to iterate segments right-to-left, treating the rightmost segment as the most significant;
   *                 {@code false} for the default left-to-right iteration order. This flag changes <em>which</em> segment
   *                 is compared first when looking for a difference; it does not invert the sign of the comparison result.
   */
  public DotNotationComparator (boolean reversed) {

    this.reversed = reversed;
  }

  /**
   * Splits both strings on dots and compares corresponding segments using alphanumeric ordering until a
   * difference is found. In default (left-to-right) mode segments are compared in their natural index order;
   * in reversed mode the rightmost segments are compared first, with unequal-length inputs right-aligned.
   * When all overlapping segments are equal, the string with fewer segments sorts before the longer one.
   *
   * @param string1 the first dot-separated string; must not be null
   * @param string2 the second dot-separated string; must not be null
   * @return a negative integer, zero, or positive integer per the {@link Comparator} contract
   * @throws NullPointerException if either argument is null
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
