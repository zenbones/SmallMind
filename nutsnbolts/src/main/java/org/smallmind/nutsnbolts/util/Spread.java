/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spread {

  private static Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
  private static Pattern SPREAD_PATTERN = Pattern.compile("(\\d+)\\.\\.(\\d+)");

  public static int[] calculate (String numbers)
    throws SpreadParserException {

    Matcher zoneMatcher;
    HashSet<Integer> intHash;
    String[] zones = numbers.split(",", 0);
    int[] spreadArray;
    int index = 0;

    intHash = new HashSet<>();
    for (String zone : zones) {
      if (zone.trim().length() == 0) {
        throw new SpreadParserException("Empty elements are not allowed");
      }

      if ((zoneMatcher = NUMBER_PATTERN.matcher(zone.trim())).matches()) {
        intHash.add(Integer.parseInt(zoneMatcher.group()));
      }
      else if ((zoneMatcher = SPREAD_PATTERN.matcher(zone.trim())).matches()) {

        int start = Integer.parseInt(zoneMatcher.group(1));
        int end = Integer.parseInt(zoneMatcher.group(2));

        for (int number = start; number <= end; number++) {
          intHash.add(number);
        }
      }
    }

    spreadArray = new int[intHash.size()];
    for (Integer number : intHash) {
      spreadArray[index++] = number;
    }

    Arrays.sort(spreadArray);

    return spreadArray;
  }
}
