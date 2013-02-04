package org.smallmind.instrument;

public interface Histogram extends Estimating, Statistician {

  public abstract void clear ();

  public abstract void update (long value);

  public abstract String getSampleType ();

  long getCount ();

  public abstract double getMax ();

  public abstract double getMin ();

  public abstract double getAverage ();

  public abstract double getStdDev ();

  public abstract double getSum ();

  public abstract Statistics getStatistics ();
}
