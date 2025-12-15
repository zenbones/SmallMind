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

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands comma-delimited alphabetic ranges (e.g. {@code a..c,z}) into arrays of characters.
 */
public class AlphaSpread {

  private static final Pattern LETTER_PATTERN = Pattern.compile("[a-z]");
  private static final Pattern SPREAD_PATTERN = Pattern.compile("([a-z])\\.\\.([a-z])");

  /**
   * Parses and expands the supplied alpha spread expression.
   *
   * @param letters comma-separated list of single letters or ranges
   * @return ordered array of characters represented by the spread
   * @throws SpreadParserException if parsing fails or an element is empty/invalid
   */
  public static char[] calculate (String letters)
    throws SpreadParserException {

    if ((letters == null) || letters.isEmpty()) {

      return new char[0];
    } else {

      Matcher zoneMatcher;
      LinkedList<Character> letterList = new LinkedList<>();
      String[] zones = letters.split(",", 0);
      char[] spreadArray;
      int index = 0;

      for (String zone : zones) {
        if (zone.isBlank()) {
          throw new SpreadParserException("Empty elements are not allowed");
        }

        if ((zoneMatcher = LETTER_PATTERN.matcher(zone.strip())).matches()) {
          letterList.add(zoneMatcher.group().charAt(0));
        } else if ((zoneMatcher = SPREAD_PATTERN.matcher(zone.strip())).matches()) {

          char start = zoneMatcher.group(1).charAt(0);
          char end = zoneMatcher.group(2).charAt(0);

          if (start <= end) {
            for (char letter = start; letter <= end; letter++) {
              letterList.add(letter);
            }
          } else {
            for (char letter = start; letter >= end; letter--) {
              letterList.add(letter);
            }
          }
        } else {
          throw new SpreadParserException("Could not parse elements");
        }
      }

      spreadArray = new char[letterList.size()];
      for (Character letter : letterList) {
        spreadArray[index++] = letter;
      }

      return spreadArray;
    }
  }
}
