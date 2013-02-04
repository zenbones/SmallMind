package org.smallmind.instrument;

import java.util.concurrent.TimeUnit;

public interface Chronometer extends Metered, Estimating, Timed, Statistician, Temporal, Stoppable {

  public abstract void clear ();

  public abstract void update (long duration);

  public abstract String getSampleType ();

  public abstract Clock getClock ();

  public abstract TimeUnit getLatencyTimeUnit ();

  public abstract TimeUnit getRateTimeUnit ();

  public abstract long getCount ();

  public abstract double getOneMinuteAvgRate ();

  public abstract double getFiveMinuteAvgRate ();

  public abstract double getFifteenMinuteAvgRate ();

  public abstract double getAverageRate ();

  public abstract double getMax ();

  public abstract double getMin ();

  public abstract double getAverage ();

  public abstract double getStdDev ();

  public abstract double getSum ();

  public abstract Statistics getStatistics ();

  public abstract void stop ();
}
