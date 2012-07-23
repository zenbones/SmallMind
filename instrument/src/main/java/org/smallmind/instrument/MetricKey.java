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

import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.nutsnbolts.util.DotNotationComparator;

public class MetricKey implements Comparable<MetricKey> {

  private static final DotNotationComparator DOT_NOTATION_COMPARATOR = new DotNotationComparator();
  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<String>();

  private MetricType type;
  private String domain;
  private String name;
  private String event;

  public MetricKey (String domain, String name, String event, MetricType type) {

    if ((domain == null) || (name == null) || (event == null) || (type == null)) {
      throw new NullPointerException("No part of a metric key may be 'null'");
    }

    this.domain = domain;
    this.name = name;
    this.event = event;
    this.type = type;
  }

  public String getDomain () {

    return domain;
  }

  public String getName () {

    return name;
  }

  public String getEvent () {

    return event;
  }

  public MetricType getType () {

    return type;
  }

  @Override
  public int compareTo (MetricKey metricKey) {

    int comparison;

    if ((comparison = DOT_NOTATION_COMPARATOR.compare(domain, metricKey.getDomain())) == 0) {
      if ((comparison = ALPHA_NUMERIC_COMPARATOR.compare(name, metricKey.getName())) == 0) {
        if ((comparison = ALPHA_NUMERIC_COMPARATOR.compare(event, metricKey.getEvent())) == 0) {
          comparison = ALPHA_NUMERIC_COMPARATOR.compare(type.name(), metricKey.getType().name());
        }
      }
    }

    return comparison;
  }

  @Override
  public int hashCode () {

    return domain.hashCode() ^ name.hashCode() ^ event.hashCode() ^ type.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof MetricKey) && ((MetricKey)obj).getDomain().equals(domain) && ((MetricKey)obj).getName().equals(name) && ((MetricKey)obj).getEvent().equals(event) && ((MetricKey)obj).getType().equals(type);
  }
}
