package org.smallmind.nutsnbolts.measure;

public interface Sample extends Shutterbug {

  public abstract void clear ();

  public abstract int size ();

  public abstract void update (long value);
}
