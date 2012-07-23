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
package org.smallmind.instrument.aop;

import java.util.HashMap;
import org.smallmind.instrument.Metric;

public class MetricSupplier {

  private static final ThreadLocal<HashMap<String, Metric>> METRIC_MAP_LOCAL = new ThreadLocal<HashMap<String, Metric>>() {

    @Override
    protected HashMap<String, Metric> initialValue () {

      return new HashMap<String, Metric>();
    }
  };

  public static void put (String key, Metric metric) {

    METRIC_MAP_LOCAL.get().put(key, metric);
  }

  public static void remove (String key) {

    METRIC_MAP_LOCAL.get().remove(key);
  }

  public static <M extends Metric> M get (String key, Class<M> metricClass) {

    return metricClass.cast(METRIC_MAP_LOCAL.get().get(key));
  }
}
