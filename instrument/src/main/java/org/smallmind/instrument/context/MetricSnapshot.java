/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.instrument.context;

import java.io.Serializable;
import java.util.LinkedList;
import org.smallmind.instrument.MetricProperty;

public class MetricSnapshot implements Serializable {

  private LinkedList<MetricItem> itemList;
  private MetricProperty[] properties;
  private String domain;

  public MetricSnapshot (MetricAddress metricAddress) {

    domain = metricAddress.getDomain();
    properties = metricAddress.getProperties();

    itemList = new LinkedList<>();
  }

  public String getDomain () {

    return domain;
  }

  public MetricProperty[] getProperties () {

    return properties;
  }

  public synchronized void addItem (MetricItem item) {

    itemList.add(item);
  }

  @Override
  public synchronized String toString () {

    StringBuilder snapshotBuilder = new StringBuilder(domain);
    boolean firstProperty = true;
    boolean firstItem = true;

    snapshotBuilder.append(':');
    for (MetricProperty property : properties) {
      if (!firstProperty) {
        snapshotBuilder.append(',');
      }

      snapshotBuilder.append(property);
      firstProperty = false;
    }

    snapshotBuilder.append('(');
    for (MetricItem item : itemList) {
      if (!firstItem) {
        snapshotBuilder.append(',');
      }

      snapshotBuilder.append(item);
      firstItem = false;
    }
    snapshotBuilder.append(')');

    return snapshotBuilder.toString();
  }
}
