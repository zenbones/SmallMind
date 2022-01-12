/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class NumericSpread {

  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
  private static final Pattern SPREAD_PATTERN = Pattern.compile("(\\d+)\\.\\.(\\d+)");

  public static int[] calculate (String numbers)
    throws SpreadParserException {

    if ((numbers == null) || numbers.isEmpty()) {

      return new int[0];
    } else {

      Matcher zoneMatcher;
      LinkedList<Integer> numberList = new LinkedList<>();
      String[] zones = numbers.split(",", 0);
      int[] spreadArray;
      int index = 0;

      for (String zone : zones) {
        if (zone.trim().length() == 0) {
          throw new SpreadParserException("Empty elements are not allowed");
        }

        if ((zoneMatcher = NUMBER_PATTERN.matcher(zone.trim())).matches()) {
          numberList.add(Integer.parseInt(zoneMatcher.group()));
        } else if ((zoneMatcher = SPREAD_PATTERN.matcher(zone.trim())).matches()) {

          int start = Integer.parseInt(zoneMatcher.group(1));
          int end = Integer.parseInt(zoneMatcher.group(2));

          if (start <= end) {
            for (int number = start; number <= end; number++) {
              numberList.add(number);
            }
          } else {
            for (int number = start; number >= end; number--) {
              numberList.add(number);
            }
          }
        } else {
          throw new SpreadParserException("Could not parse elements");
        }
      }

      spreadArray = new int[numberList.size()];
      for (Integer number : numberList) {
        spreadArray[index++] = number;
      }

      return spreadArray;
    }
  }
}
