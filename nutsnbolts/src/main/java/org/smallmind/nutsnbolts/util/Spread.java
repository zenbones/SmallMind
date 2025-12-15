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

/**
 * Convenience wrapper that delegates to numeric or alphabetic spread expansion depending on the first character.
 */
public class Spread {

  /**
   * Expands a comma-delimited set of ranges into string values.
   *
   * @param spreadable expression such as {@code 1..3,7} or {@code a..c,z}
   * @return ordered array of values as strings
   * @throws SpreadParserException if parsing fails
   */
  public static String[] calculate (String spreadable)
    throws SpreadParserException {

    if ((spreadable == null) || spreadable.isEmpty()) {

      return new String[0];
    } else {

      String[] values;
      int index = 0;

      if (Character.isDigit(spreadable.charAt(0))) {

        int[] numbers = NumericSpread.calculate(spreadable);
        values = new String[numbers.length];

        for (int number : numbers) {
          values[index++] = String.valueOf(number);
        }
      } else {

        char[] letters = AlphaSpread.calculate(spreadable);
        values = new String[letters.length];

        for (char letter : letters) {
          values[index++] = String.valueOf(letter);
        }
      }

      return values;
    }
  }
}
