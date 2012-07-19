package org.smallmind.nutsnbolts.measure;

public interface Summarizing {

  public abstract double getMax ();

  public abstract double getMin ();

  public abstract double getMean ();

  public abstract double getStdDev ();

  public abstract double getSum ();
}
