/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.instrument;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.instrument.context.MetricFact;
import org.smallmind.instrument.context.MetricItem;
import org.smallmind.instrument.context.MetricSnapshot;

public class HistogramImpl extends MetricImpl<Histogram> implements Histogram {

  private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
  // These are for the Welford algorithm for calculating running variance without floating-point doom.
  private final AtomicReference<double[]> variance = new AtomicReference<>(new double[] {-1, 0});
  private final AtomicLong count = new AtomicLong(0);
  private final ArrayCache arrayCache = new ArrayCache();
  private final Sample sample;
  private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
  private final AtomicLong sum = new AtomicLong(0);

  public HistogramImpl (Samples samples) {

    sample = samples.createSample();
  }

  @Override
  public Class<Histogram> getMetricClass () {

    return Histogram.class;
  }

  @Override
  public void clear () {

    MetricSnapshot metricSnapshot;

    sample.clear();
    count.set(0);
    sum.set(0);
    min.set(Long.MAX_VALUE);
    max.set(Long.MIN_VALUE);
    variance.set(new double[] {-1, 0});

    if ((metricSnapshot = getMetricSnapshot()) != null) {
      if (metricSnapshot.willTrace(MetricFact.COUNT)) {
        metricSnapshot.addItem(new MetricItem<>("count", 0L));
      }
      if (metricSnapshot.willTrace(MetricFact.SUM)) {
        metricSnapshot.addItem(new MetricItem<>("sum", 0L));
      }
      if (metricSnapshot.willTrace(MetricFact.MIN)) {
        metricSnapshot.addItem(new MetricItem<>("min", "n/a"));
      }
      if (metricSnapshot.willTrace(MetricFact.MAX)) {
        metricSnapshot.addItem(new MetricItem<>("max", "n/a"));
      }
      if (metricSnapshot.willTrace(MetricFact.AVG)) {
        metricSnapshot.addItem(new MetricItem<>("avg", 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.STD_DEV)) {
        metricSnapshot.addItem(new MetricItem<>("std dev", 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.MEDIAN)) {
        metricSnapshot.addItem(new MetricItem<>("median", 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.P75_Q)) {
        metricSnapshot.addItem(new MetricItem<>("75th pctl", 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.P95_Q)) {
        metricSnapshot.addItem(new MetricItem<>("95th pctl", 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.P98_Q)) {
        metricSnapshot.addItem(new MetricItem<>("98th pctl", 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.P99_Q)) {
        metricSnapshot.addItem(new MetricItem<>("99th pctl", 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.P999_Q)) {
        metricSnapshot.addItem(new MetricItem<>("999th pctl", 0.0));
      }
    }
  }

  @Override
  public void update (long value) {

    MetricSnapshot metricSnapshot;
    double[] currentValues;
    long currentCount;
    long currentSum;
    long currentMin;
    long currentMax;

    currentCount = count.incrementAndGet();
    currentSum = sum.getAndAdd(value);
    currentMin = setMin(value);
    currentMax = setMax(value);
    currentValues = updateVariance(value);
    sample.update(value);

    if ((metricSnapshot = getMetricSnapshot()) != null) {

      Statistics currentStatistics = sample.getStatistics();

      if (metricSnapshot.willTrace(MetricFact.COUNT)) {
        metricSnapshot.addItem(new MetricItem<>("count", currentCount));
      }
      if (metricSnapshot.willTrace(MetricFact.SUM)) {
        metricSnapshot.addItem(new MetricItem<>("sum", currentSum));
      }
      if (metricSnapshot.willTrace(MetricFact.MIN)) {
        metricSnapshot.addItem(new MetricItem<>("min", currentMin));
      }
      if (metricSnapshot.willTrace(MetricFact.MAX)) {
        metricSnapshot.addItem(new MetricItem<>("max", currentMax));
      }
      if (metricSnapshot.willTrace(MetricFact.AVG)) {
        metricSnapshot.addItem(new MetricItem<>("avg", (currentCount > 0) ? currentSum / (double)currentCount : 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.STD_DEV)) {
        metricSnapshot.addItem(new MetricItem<>("std dev", (currentCount > 0) ? Math.sqrt((currentCount <= 1) ? 0.0 : currentValues[1] / (currentCount - 1)) : 0.0));
      }
      if (metricSnapshot.willTrace(MetricFact.MEDIAN)) {
        metricSnapshot.addItem(new MetricItem<>("median", currentStatistics.getMedian()));
      }
      if (metricSnapshot.willTrace(MetricFact.P75_Q)) {
        metricSnapshot.addItem(new MetricItem<>("75th pctl", currentStatistics.get75thPercentile()));
      }
      if (metricSnapshot.willTrace(MetricFact.P95_Q)) {
        metricSnapshot.addItem(new MetricItem<>("95th pctl", currentStatistics.get95thPercentile()));
      }
      if (metricSnapshot.willTrace(MetricFact.P98_Q)) {
        metricSnapshot.addItem(new MetricItem<>("98th pctl", currentStatistics.get98thPercentile()));
      }
      if (metricSnapshot.willTrace(MetricFact.P99_Q)) {
        metricSnapshot.addItem(new MetricItem<>("99th pctl", currentStatistics.get99thPercentile()));
      }
      if (metricSnapshot.willTrace(MetricFact.P999_Q)) {
        metricSnapshot.addItem(new MetricItem<>("999th pctl", currentStatistics.get999thPercentile()));
      }
    }
  }

  @Override
  public String getSampleType () {

    return sample.getType().name();
  }

  @Override
  public long getCount () {

    return count.get();
  }

  @Override
  public double getMax () {

    return (getCount() > 0) ? max.get() : 0.0;
  }

  private long setMax (long potentialMax) {

    boolean replaced = false;
    long currentMax;

    do {
      currentMax = max.get();
    } while (!(currentMax >= potentialMax || (replaced = max.compareAndSet(currentMax, potentialMax))));

    return replaced ? potentialMax : currentMax;
  }

  @Override
  public double getMin () {

    return (getCount() > 0) ? min.get() : 0.0;
  }

  private long setMin (long potentialMin) {

    boolean replaced = false;
    long currentMin;

    do {
      currentMin = min.get();
    } while (!(currentMin <= potentialMin || (replaced = min.compareAndSet(currentMin, potentialMin))));

    return replaced ? potentialMin : currentMin;
  }

  @Override
  public double getAverage () {

    return (getCount() > 0) ? sum.get() / (double)getCount() : 0.0;
  }

  @Override
  public double getStdDev () {

    return (getCount() > 0) ? Math.sqrt(getVariance()) : 0.0;
  }

  @Override
  public double getSum () {

    return (double)sum.get();
  }

  private double getVariance () {

    return (getCount() <= 1) ? 0.0 : variance.get()[1] / (getCount() - 1);
  }

  @Override
  public Statistics getStatistics () {

    return sample.getStatistics();
  }

  private double[] updateVariance (long value) {

    boolean done;
    double[] newValues;

    do {

      double[] oldValues = variance.get();

      newValues = arrayCache.get();

      if (oldValues[0] == -1) {
        newValues[0] = value;
        newValues[1] = 0;
      }
      else {

        double oldM = oldValues[0];
        double oldS = oldValues[1];
        double newM = oldM + ((value - oldM) / getCount());
        double newS = oldS + ((value - oldM) * (value - newM));

        newValues[0] = newM;
        newValues[1] = newS;
      }

      if (done = variance.compareAndSet(oldValues, newValues)) {
        // recycle the old array into the cache
        arrayCache.set(oldValues);
      }
    } while (!done);

    return newValues;
  }

  private static final class ArrayCache extends ThreadLocal<double[]> {

    @Override
    protected double[] initialValue () {

      return new double[2];
    }
  }
}
