package org.smallmind.seda;

public class StopWatch {

   private long start;
   private long stop;

   public StopWatch click () {

      start = stop;
      stop = System.currentTimeMillis();

      return this;
   }

   public long getStart () {

      return start;
   }

   public long getStop () {

      return stop;
   }
}
