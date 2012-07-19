package org.smallmind.nutsnbolts.measure;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Histogram implements Measure, Shutterbug, Summarizing {

  private static final class ArrayCache extends ThreadLocal<double[]> {

    @Override
    protected double[] initialValue () {

      return new double[2];
    }
  }

  // These are for the Welford algorithm for calculating running variance without floating-point doom.
  private final AtomicReference<double[]> variance = new AtomicReference<double[]>(new double[] {-1, 0}); // M, S
  private final AtomicLong count = new AtomicLong(0);
  private final ArrayCache arrayCache = new ArrayCache();

  private final Sample sample;
  private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
  private final AtomicLong sum = new AtomicLong(0);

  Histogram (Samples samples) {

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

  public void update (int value) {

    update((long)value);
  }

  public void update (long value) {

    count.incrementAndGet();
    sample.update(value);
    setMax(value);
    setMin(value);
    sum.getAndAdd(value);
    updateVariance(value);
  }

  public long getCount () {

    return count.get();
  }

  @Override
  public double getMax () {

    return (getCount() > 0) ? max.get() : 0.0;
  }

  @Override
  public double getMin () {

    return (getCount() > 0) ? min.get() : 0.0;
  }

  @Override
  public double getMean () {

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
  public Snapshot getSnapshot () {

    return sample.getSnapshot();
  }

  private void setMax (long potentialMax) {

    boolean done = false;

    while (!done) {

      long currentMax = max.get();

      done = currentMax >= potentialMax || max.compareAndSet(currentMax, potentialMax);
    }
  }

  private void setMin (long potentialMin) {

    boolean done = false;

    while (!done) {

      long currentMin = min.get();

      done = currentMin <= potentialMin || min.compareAndSet(currentMin, potentialMin);
    }
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
}
