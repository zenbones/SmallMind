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

import org.aspectj.lang.ProceedingJoinPoint;
import org.smallmind.instrument.Metric;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.MetricRegistry;
import org.smallmind.instrument.MetricRegistryFactory;
import org.smallmind.instrument.Metrics;
import org.smallmind.nutsnbolts.reflection.aop.AOPUtility;

public abstract class MetricAspect {

  private static final MetricRegistry METRIC_REGISTRY;

  static {

    if ((METRIC_REGISTRY = MetricRegistryFactory.getMetricRegistry()) == null) {
      throw new ExceptionInInitializerError("No MetricRegistry instance has been registered with the MetricRegistryFactory");
    }
  }

  public Object engage (ProceedingJoinPoint thisJoinPoint, JMX jmx, String alias, Metrics.MetricBuilder<?> metricBuilder)
    throws Throwable {

    Metric metric;
    MetricProperty[] properties;
    String supplierKey;

    properties = new MetricProperty[jmx.properties().length];
    for (int index = 0; index < properties.length; index++) {
      properties[index] = new MetricProperty(jmx.properties()[index].key(), jmx.properties()[index].constant() ? jmx.properties()[index].value() : AOPUtility.getParameterValue(thisJoinPoint, jmx.properties()[index].value(), false).toString());
    }

    metric = METRIC_REGISTRY.ensure(metricBuilder, jmx.domain(), properties);
    MetricSupplier.push(supplierKey = (alias.length() == 0) ? null : alias, metric);

    try {
      return thisJoinPoint.proceed();
    }
    finally {
      MetricSupplier.pop(supplierKey);
    }
  }
}
