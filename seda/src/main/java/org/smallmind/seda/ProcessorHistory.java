package org.smallmind.seda;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProcessorHistory {

   private ReentrantReadWriteLock lock;
   private TimeMark idleMark;
   private TimeMark activeMark;

   public ProcessorHistory (long trackingTime, TimeUnit trackingTimeUnit) {

      idleMark = new TimeMark(trackingTimeUnit.toMillis(trackingTime));
      activeMark = new TimeMark(trackingTimeUnit.toMillis(trackingTime));

      lock = new ReentrantReadWriteLock();
   }

   public double getIdlePercentage () {

      lock.readLock().lock();
      try {

         double idleTime;
         double totalTime;

         if ((totalTime = (idleTime = idleMark.getTotal()) + activeMark.getTotal()) == 0) {

            return 0;
         }

         return idleTime / totalTime;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public double getActivePercentage () {

      lock.readLock().lock();
      try {

         double activeTime;
         double totalTime;

         if ((totalTime = idleMark.getTotal() + (activeTime = activeMark.getTotal())) == 0) {

            return 0;
         }

         return activeTime / totalTime;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   protected void addIdleTime (long begin, long end) {

      lock.writeLock().lock();
      try {
         idleMark.additional(begin, end);
         activeMark.update(end);
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   protected void addActiveTime (long begin, long end) {

      lock.writeLock().lock();
      try {
         activeMark.additional(begin, end);
         idleMark.update(end);
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   private static class TimeMark {

      private double average;
      private long track;
      private long start;
      private long stop;

      public TimeMark (long track) {

         this.track = track;
      }

      public double getTotal () {

         if (start == 0) {

            return 0;
         }

         return (stop - start) * average;
      }

      public void update (long end) {

         if (start != 0) {

            long oldest;

            if ((oldest = end - track) > stop) {
               start = 0;
               stop = 0;
               average = 0;
            }
            else {
               oldest = Math.max(start, oldest);
               average = ((stop - oldest) * average) / (end - oldest);
               start = oldest;
               stop = end;
            }
         }
      }

      public void additional (long begin, long end) {

         if (start == 0) {
            start = begin;
            stop = end;
            average = 1;
         }
         else {

            long oldest;

            if ((oldest = end - track) > stop) {
               start = begin;
               average = 1;
            }
            else {
               oldest = Math.max(start, oldest);
               average = (((stop - oldest) * average) + (end - begin)) / (end - oldest);
               start = oldest;
            }

            stop = end;
         }
      }
   }
}
