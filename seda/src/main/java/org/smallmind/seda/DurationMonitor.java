package org.smallmind.seda;

public class DurationMonitor {

   private boolean initialized = false;
   private long[] durations;
   private long durationTotal = 0;
   private int index = 0;

   public DurationMonitor (int size) {

      durations = new long[size];
   }

   public double getAverage () {

      return durationTotal / (double)((initialized) ? durations.length : index);
   }

   protected void accumulate (long duration) {

      durationTotal -= durations[index];
      durations[index++] = duration;
      durationTotal += duration;

      if (index == durations.length) {
         initialized = true;
         index = 0;
      }
   }
}
