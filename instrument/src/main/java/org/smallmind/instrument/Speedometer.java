package org.smallmind.instrument;

import java.util.concurrent.TimeUnit;

public interface Speedometer extends Tracked, Temporal, Stoppable {

  public abstract void clear ();

  public abstract void update ();

  public abstract void update (long quantity);

  public abstract Clock getClock ();

  public abstract TimeUnit getRateTimeUnit ();

  public abstract long getCount ();

  public abstract double getOneMinuteAvgRate ();

  public abstract double getOneMinuteAvgVelocity ();

  public abstract double getFiveMinuteAvgRate ();

  public abstract double getFiveMinuteAvgVelocity ();

  public abstract double getFifteenMinuteAvgRate ();

  public abstract double getFifteenMinuteAvgVelocity ();

  public abstract double getAverageRate ();

  public abstract double getAverageVelocity ();

  public abstract double getMax ();

  public abstract double getMin ();

  public abstract void stop ();
}
