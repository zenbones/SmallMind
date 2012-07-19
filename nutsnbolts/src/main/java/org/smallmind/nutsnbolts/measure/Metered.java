package org.smallmind.nutsnbolts.measure;

public interface Metered extends Measure {

  public abstract double getOneMinuteRate ();

  public abstract double getFiveMinuteRate ();

  public abstract double getFifteenMinuteRate ();

  public abstract double getMeanRate ();
}
