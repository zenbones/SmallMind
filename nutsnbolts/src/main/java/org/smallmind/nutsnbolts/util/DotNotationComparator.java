/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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

public class DotNotationComparator implements Comparator<String> {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<String>();

  private boolean reversed;

  public DotNotationComparator () {

    this(false);
  }

  public DotNotationComparator (boolean reversed) {

    this.reversed = reversed;
  }

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
