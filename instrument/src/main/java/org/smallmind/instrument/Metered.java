package org.smallmind.instrument;

public interface Metered extends Measure {

  public abstract double getOneMinuteRate ();

  public abstract double getFiveMinuteRate ();

  public abstract double getFifteenMinuteRate ();

  public abstract double getMeanRate ();
}
