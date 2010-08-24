package org.smallmind.seda;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProcessorHistory {

   private ReentrantReadWriteLock lock;
   private LinkedList<TimeMark> idleList;
   private LinkedList<TimeMark> activeList;
   private long maxTackedMillis;
   private long idleTotal = 0;
   private long activeTotal = 0;

   public ProcessorHistory (long trackingTime, TimeUnit trackingTimeUnit) {

      maxTackedMillis = trackingTimeUnit.toMillis(trackingTime);

      idleList = new LinkedList<TimeMark>();
      activeList = new LinkedList<TimeMark>();

      lock = new ReentrantReadWriteLock();
   }

   public double getIdlePercentage () {

      lock.readLock().lock();
      try {

         long totalTime;

         if ((totalTime = idleTotal + activeTotal) == 0) {

            return 0;
         }

         return idleTotal / (double)totalTime;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public double getActivePercentage () {

      lock.readLock().lock();
      try {

         long totalTime;

         if ((totalTime = idleTotal + activeTotal) == 0) {

            return 0;
         }

         return activeTotal / (double)totalTime;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   protected void addIdleTime (long start, long end) {

      lock.writeLock().lock();
      try {

         long duration;

         if ((duration = end - start) > 0) {
            idleList.addFirst(new TimeMark(start, duration));
            idleTotal += duration;
         }

         idleTotal -= trimHistory(idleList, end);
         activeTotal -= trimHistory(activeList, end);
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   protected void addActiveTime (long start, long end) {

      lock.writeLock().lock();
      try {

         long duration;

         if ((duration = end - start) > 0) {
            activeList.addFirst(new TimeMark(start, duration));
            activeTotal += duration;
         }

         activeTotal -= trimHistory(activeList, end);
         idleTotal -= trimHistory(idleList, end);
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   private long trimHistory (LinkedList<TimeMark> historyList, long now) {

      if (!historyList.isEmpty()) {

         long oldestTime = now - maxTackedMillis;
         long durationDelta;
         long trimmedTime = 0;

         while ((!historyList.isEmpty()) && (historyList.getLast().getStart() < oldestTime)) {
            if ((durationDelta = oldestTime - historyList.getLast().getStart()) >= historyList.getLast().getDuration()) {
               trimmedTime += historyList.removeLast().getDuration();
            }
            else {
               historyList.getLast().setStart(oldestTime);
               historyList.getLast().setDuration(historyList.getLast().getDuration() - durationDelta);
               trimmedTime += durationDelta;
               break;
            }
         }

         return trimmedTime;
      }

      return 0;
   }

   private static class TimeMark {

      private long start;
      private long duration;

      private TimeMark (long start, long duration) {

         this.start = start;
         this.duration = duration;
      }

      public long getStart () {

         return start;
      }

      public void setStart (long start) {

         this.start = start;
      }

      public long getDuration () {

         return duration;
      }

      public void setDuration (long duration) {

         this.duration = duration;
      }
   }
}
