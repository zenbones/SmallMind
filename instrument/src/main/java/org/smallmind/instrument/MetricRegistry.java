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

import java.util.concurrent.ConcurrentHashMap;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.smallmind.instrument.jmx.ChronometerMonitor;
import org.smallmind.instrument.jmx.DefaultJMXNamingPolicy;
import org.smallmind.instrument.jmx.HistogramMonitor;
import org.smallmind.instrument.jmx.JMXNamingPolicy;
import org.smallmind.instrument.jmx.MeterMonitor;
import org.smallmind.instrument.jmx.RegisterMonitor;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class MetricRegistry {

  private final ConcurrentHashMap<MetricKey, Metric> metricMap = new ConcurrentHashMap<MetricKey, Metric>();

  private MBeanServer server;
  private JMXNamingPolicy jmxNamingPolicy = new DefaultJMXNamingPolicy();

  public void setServer (MBeanServer server) {

    this.server = server;
  }

  public void setJmxNamingPolicy (JMXNamingPolicy jmxNamingPolicy) {

    this.jmxNamingPolicy = jmxNamingPolicy;
  }

  public void register () {

    MetricRegistryFactory.register(this);
  }

  public <M extends Metric> M ensure (String domain, String name, String event, Metrics.MetricBuilder<M> metricBuilder)
    throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

    M metric;
    MetricKey metricKey = new MetricKey(domain, name, event, metricBuilder.getType());

    if ((metric = metricBuilder.getMetricClass().cast(metricMap.get(metricKey))) == null) {
      synchronized (metricMap) {
        if ((metric = metricBuilder.getMetricClass().cast(metricMap.get(metricKey))) == null) {
          metricMap.put(metricKey, metric = metricBuilder.construct());

          if (server != null) {

            DynamicMBean mBean = null;

            switch (metricBuilder.getType()) {
              case REGISTER:
                mBean = new RegisterMonitor((Register)metric);
                break;
              case METER:
                mBean = new MeterMonitor((Meter)metric);
                break;
              case HISTOGRAM:
                new HistogramMonitor((Histogram)metric);
                break;
              case CHRONOMETER:
                new ChronometerMonitor((Chronometer)metric);
                break;
              default:
                throw new UnknownSwitchCaseException(metricBuilder.getType().name());
            }

            server.registerMBean(mBean, jmxNamingPolicy.createObjectName(domain, name, event, metricBuilder.getType()));
          }
        }
      }
    }

    return metric;
  }
}
