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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MetricContext {

  private final TracingOptions tracingOptions;
  private final LinkedList<MetricSnapshot> arabesqueQueue = new LinkedList<>();
  private final LinkedList<MetricSnapshot> outputList = new LinkedList<>();
  private final long startTime;

  public MetricContext (TracingOptions tracingOptions) {

    this.tracingOptions = tracingOptions;

    startTime = System.currentTimeMillis();
  }

  public long getStartTime () {

    return startTime;
  }

  public void append (MetricContext metricContext) {

    outputList.addAll(metricContext.getSnapshots());
  }

  public boolean pushSnapshot (MetricAddress metricAddress) {

    MetricSnapshot metricSnapshot;

    arabesqueQueue.addFirst(metricSnapshot = new MetricSnapshot(metricAddress, tracingOptions));
    outputList.addLast(metricSnapshot);

    return true;
  }

  public void popSnapshot () {

    arabesqueQueue.removeFirst();
  }

  public MetricSnapshot getSnapshot () {

    if (arabesqueQueue.isEmpty()) {

      return null;
    }

    return arabesqueQueue.getFirst();
  }

  public boolean isEmpty () {

    if (outputList.isEmpty()) {

      return true;
    }

    for (MetricSnapshot snapshot : outputList) {
      if (!snapshot.isEmpty()) {

        return false;
      }
    }

    return true;
  }

  public List<MetricSnapshot> getSnapshots () {

    return Collections.unmodifiableList(outputList);
  }

  @Override
  public String toString () {

    StringBuilder contextBuilder = new StringBuilder();
    boolean firstContext = true;

    for (MetricSnapshot snapshot : outputList) {
      if (!snapshot.isEmpty()) {
        if (!firstContext) {
          contextBuilder.append(',');
        }

        contextBuilder.append('[').append(snapshot).append(']');
        firstContext = false;
      }
    }

    return contextBuilder.toString();
  }
}
