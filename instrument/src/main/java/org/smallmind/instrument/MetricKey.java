/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.instrument;

import org.smallmind.nutsnbolts.util.DotNotationComparator;

public class MetricKey implements Comparable<MetricKey> {

  private static final DotNotationComparator DOT_NOTATION_COMPARATOR = new DotNotationComparator();

  private String group;
  private String name;

  public MetricKey (String group, String name) {

    this.group = group;
    this.name = name;
  }

  public String getGroup () {

    return group;
  }

  public String getName () {

    return name;
  }

  @Override
  public int compareTo (MetricKey metricKey) {

    int comparison;

    if ((comparison = DOT_NOTATION_COMPARATOR.compare(group, metricKey.getGroup())) == 0) {
      comparison = name.compareTo(metricKey.getName());
    }

    return comparison;
  }

  @Override
  public int hashCode () {

    return group.hashCode() ^ name.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof MetricKey) && ((MetricKey)obj).getGroup().equals(group) && ((MetricKey)obj).getName().equals(name);
  }
}
