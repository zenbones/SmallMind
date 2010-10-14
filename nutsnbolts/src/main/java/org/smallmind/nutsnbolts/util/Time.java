package org.smallmind.nutsnbolts.util;

import java.util.concurrent.TimeUnit;

public class Time {

   TimeUnit timeUnit;
   long duration;

   public Time (long duration, TimeUnit timeUnit) {

      this.duration = duration;
      this.timeUnit = timeUnit;
   }

   public long getDuration () {

      return duration;
   }

   public TimeUnit getTimeUnit () {

      return timeUnit;
   }
}
