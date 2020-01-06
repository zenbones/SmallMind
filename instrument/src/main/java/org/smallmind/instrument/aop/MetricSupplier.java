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
package org.smallmind.instrument.aop;

import java.util.HashMap;
import java.util.LinkedList;
import org.smallmind.instrument.Metric;

public class MetricSupplier {

  private static final ThreadLocal<HashMap<String, LinkedList<Metric>>> METRIC_MAP_LOCAL = new ThreadLocal<HashMap<String, LinkedList<Metric>>>() {

    @Override
    protected HashMap<String, LinkedList<Metric>> initialValue () {

      return new HashMap<String, LinkedList<Metric>>();
    }
  };

  public static void push (String key, Metric metric) {

    HashMap<String, LinkedList<Metric>> metricMap = METRIC_MAP_LOCAL.get();
    LinkedList<Metric> metricList;

    if ((metricList = metricMap.get(key)) == null) {
      metricMap.put(key, metricList = new LinkedList<Metric>());
    }

    metricList.push(metric);
  }

  public static void pop (String key) {

    METRIC_MAP_LOCAL.get().get(key).pop();
  }

  public static <M extends Metric<M>> M get (String key, Class<M> metricClass) {

    LinkedList<Metric> metricList;

    if (((metricList = METRIC_MAP_LOCAL.get().get(key)) == null) || metricList.isEmpty()) {
      throw new MissingMetricException("The metric(%s) is not locally defined", (key == null) ? "null" : key);
    }

    return metricClass.cast(metricList.peek());
  }
}
