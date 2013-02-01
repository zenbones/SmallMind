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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Histogram extends Metric implements Estimating, Statistician {

  private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
  // These are for the Welford algorithm for calculating running variance without floating-point doom.
  private final AtomicReference<double[]> variance = new AtomicReference<double[]>(new double[] {-1, 0});
  private final AtomicLong count = new AtomicLong(0);
  private final ArrayCache arrayCache = new ArrayCache();
  private final Sample sample;
  private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
  private final AtomicLong sum = new AtomicLong(0);

  public Histogram (Samples samples) {

    sample = samples.createSample();
  }

  public void clear () {

    sample.clear();
    count.set(0);
    max.set(Long.MIN_VALUE);
    min.set(Long.MAX_VALUE);
    sum.set(0);
    variance.set(new double[] {-1, 0});
  }

  public void update (long value) {

    count.incrementAndGet();
    sample.update(value);
    setMax(value);
    setMin(value);
    sum.getAndAdd(value);
    updateVariance(value);
  }

  @Override
  public String getSampleType () {

    return sample.getType().name();
  }

  public long getCount () {

    return count.get();
  }

  @Override
  public double getMax () {

    return (getCount() > 0) ? max.get() : 0.0;
  }

  private void setMax (long potentialMax) {

    boolean done = false;

    while (!done) {

      long currentMax = max.get();

      done = currentMax >= potentialMax || max.compareAndSet(currentMax, potentialMax);
    }
  }

  @Override
  public double getMin () {

    return (getCount() > 0) ? min.get() : 0.0;
  }

  private void setMin (long potentialMin) {

    boolean done = false;

    while (!done) {

      long currentMin = min.get();

      done = currentMin <= potentialMin || min.compareAndSet(currentMin, potentialMin);
    }
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

  private void updateVariance (long value) {

    boolean done = false;

    while (!done) {

      double[] oldValues = variance.get();
      double[] newValues = arrayCache.get();

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
    }
  }

  private static final class ArrayCache extends ThreadLocal<double[]> {

    @Override
    protected double[] initialValue () {

      return new double[2];
    }
  }
}
