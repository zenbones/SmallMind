package org.smallmind.instrument;

public interface Tally extends Countable {

  public abstract void clear ();

  public abstract void inc ();

  public abstract void inc (long n);

  public abstract void dec ();

  public abstract void dec (long n);

  public abstract long getCount ();
}
