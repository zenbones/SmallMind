package org.smallmind.claxon.meter;

public interface Clock {

  long wallTime ();

  long monotonicTime ();
}
