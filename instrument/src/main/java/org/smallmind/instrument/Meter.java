package org.smallmind.instrument;

import java.util.concurrent.TimeUnit;

public interface Meter extends Metered, Temporal, Stoppable {

  public abstract void clear ();

  public abstract void mark ();

  public abstract void mark (long n);

  public abstract Clock getClock ();

  public abstract TimeUnit getRateTimeUnit ();

  public abstract long getCount ();

  public abstract double getOneMinuteAvgRate ();

  public abstract double getFiveMinuteAvgRate ();

  public abstract double getFifteenMinuteAvgRate ();

  public abstract double getAverageRate ();

  public abstract void stop ();
}
