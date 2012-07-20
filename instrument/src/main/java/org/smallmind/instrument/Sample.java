package org.smallmind.instrument;

public interface Sample extends Shutterbug {

  public abstract void clear ();

  public abstract int size ();

  public abstract void update (long value);
}
