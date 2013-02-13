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
package org.smallmind.instrument;

import java.util.concurrent.TimeUnit;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.instrument.context.MetricContext;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.scribe.pen.LoggerManager;

public class InstrumentationManager implements PerApplicationDataManager {

  private static final ThreadLocal<MetricContext> METRIC_CONTEXT_LOCAL = new ThreadLocal<MetricContext>();

  public static void register (MetricRegistry metricRegistry) {

    PerApplicationContext.setPerApplicationData(InstrumentationManager.class, metricRegistry);
  }

  public static MetricRegistry getMetricRegistry () {

    return PerApplicationContext.getPerApplicationData(InstrumentationManager.class, MetricRegistry.class);
  }

  public static MetricContext getMetricContext () {

    return METRIC_CONTEXT_LOCAL.get();
  }

  public static void setMetricContext (MetricContext metricContext) {

    METRIC_CONTEXT_LOCAL.set(metricContext);
  }

  public static void createMetricContext () {

    MetricRegistry metricRegistry;

    setMetricContext(new MetricContext(((metricRegistry = InstrumentationManager.getMetricRegistry()) == null) ? null : metricRegistry.getTracingOptions()));
  }

  public static void appendMetricContext (MetricContext appendedMetricContext) {

    MetricContext currentMetricContext;

    if ((currentMetricContext = METRIC_CONTEXT_LOCAL.get()) == null) {
      METRIC_CONTEXT_LOCAL.set(appendedMetricContext);
    }
    else {
      currentMetricContext.append(appendedMetricContext);
    }
  }

  public static MetricContext removeMetricContext () {

    MetricContext metricContext = METRIC_CONTEXT_LOCAL.get();

    METRIC_CONTEXT_LOCAL.remove();

    return metricContext;
  }

  public static void publishMetricContext () {

    MetricContext metricContext;

    if (((metricContext = removeMetricContext()) != null) && metricContext.hasAged() && (!metricContext.isEmpty())) {
      LoggerManager.getLogger(MetricContext.class).info(metricContext);
    }
  }

  public static <M extends Metric<M>> void execute (Instrument<M> instrument)
    throws Exception {

    MetricRegistry metricRegistry;
    InstrumentationArguments<M> arguments;

    instrument.with((((metricRegistry = getMetricRegistry()) == null) || ((arguments = instrument.getArguments()) == null)) ? null : metricRegistry.instrument(arguments.getBuilder(), arguments.getDomain(), arguments.getProperties()));
  }

  public static <M extends Metric<M>, T> T execute (InstrumentAndReturn<M, T> instrumentAndReturn)
    throws Exception {

    MetricRegistry metricRegistry;
    InstrumentationArguments<M> arguments;

    return instrumentAndReturn.with((((metricRegistry = getMetricRegistry()) == null) || ((arguments = instrumentAndReturn.getArguments()) == null)) ? null : metricRegistry.instrument(arguments.getBuilder(), arguments.getDomain(), arguments.getProperties()));
  }

  public static void instrumentWithTally (MetricConfigurationProvider provider, MetricProperty... properties) {

    instrumentWithTally(provider, 1, properties);
  }

  public static void instrumentWithTally (MetricConfigurationProvider provider, long count, MetricProperty... properties) {

    if ((provider != null) && (provider.getMetricConfiguration() != null) && provider.getMetricConfiguration().isInstrumented()) {
      getMetricRegistry().instrument(Metrics.buildTally(0), provider.getMetricConfiguration().getMetricDomain().getDomain(), properties).inc(count);
    }
  }

  public static void instrumentWithMeter (MetricConfigurationProvider provider, MetricProperty... properties) {

    instrumentWithMeter(provider, 1, properties);
  }

  public static void instrumentWithMeter (MetricConfigurationProvider provider, long quantity, MetricProperty... properties) {

    if ((provider != null) && (provider.getMetricConfiguration() != null) && provider.getMetricConfiguration().isInstrumented()) {
      getMetricRegistry().instrument(Metrics.buildMeter(provider.getMetricConfiguration().getTickInterval(), provider.getMetricConfiguration().getTickTimeUnit(), Clocks.EPOCH), provider.getMetricConfiguration().getMetricDomain().getDomain(), properties).mark(quantity);
    }
  }

  public static void instrumentWithHistogram (MetricConfigurationProvider provider, long value, MetricProperty... properties) {

    if ((provider != null) && (provider.getMetricConfiguration() != null) && provider.getMetricConfiguration().isInstrumented()) {
      getMetricRegistry().instrument(Metrics.buildHistogram(provider.getMetricConfiguration().getSamples()), provider.getMetricConfiguration().getMetricDomain().getDomain(), properties).update(value);
    }
  }

  public static void instrumentWithSpeedometer (MetricConfigurationProvider provider, long quantity, MetricProperty... properties) {

    if ((provider != null) && (provider.getMetricConfiguration() != null) && provider.getMetricConfiguration().isInstrumented()) {
      getMetricRegistry().instrument(Metrics.buildSpeedometer(provider.getMetricConfiguration().getTickInterval(), provider.getMetricConfiguration().getTickTimeUnit(), Clocks.EPOCH), provider.getMetricConfiguration().getMetricDomain().getDomain(), properties).update(quantity);
    }
  }

  public static void instrumentWithChronometer (MetricConfigurationProvider provider, long duration, TimeUnit durationTimeUnit, MetricProperty... properties) {

    if ((provider != null) && (provider.getMetricConfiguration() != null) && provider.getMetricConfiguration().isInstrumented()) {

      Chronometer chronometer;

      (chronometer = getMetricRegistry().instrument(Metrics.buildChronometer(provider.getMetricConfiguration().getSamples(), TimeUnit.MILLISECONDS, provider.getMetricConfiguration().getTickInterval(), provider.getMetricConfiguration().getTickTimeUnit(), Clocks.EPOCH), provider.getMetricConfiguration().getMetricDomain().getDomain(), properties)).update(chronometer.getLatencyTimeUnit().convert(duration, durationTimeUnit));
    }
  }
}
