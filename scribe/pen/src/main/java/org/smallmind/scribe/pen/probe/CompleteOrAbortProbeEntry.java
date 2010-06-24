package org.smallmind.scribe.pen.probe;

public class CompleteOrAbortProbeEntry extends ProbeEntry {

   private long startTime;
   private long stopTime;

   public CompleteOrAbortProbeEntry (ProbeStatus probeStatus, Probe probe, long startTime, long stopTime) {

      super(probeStatus, probe);

      this.startTime = startTime;
      this.stopTime = stopTime;
   }

   public long getStartTime () {

      return startTime;
   }

   public long getStopTime () {

      return stopTime;
   }
}
