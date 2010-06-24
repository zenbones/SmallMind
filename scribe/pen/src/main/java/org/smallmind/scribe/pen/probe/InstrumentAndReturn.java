package org.smallmind.scribe.pen.probe;

public abstract class InstrumentAndReturn<T> {

   public abstract T withProbe (Probe probe)
      throws Exception;
}
