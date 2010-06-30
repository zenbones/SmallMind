package org.smallmind.persistence.statistics;

public class ThreadLocalStatisticsFactory implements StatisticsFactory {

   private static final InheritableThreadLocal<Statistics> STATISTICS_THREAD_LOCAL = new InheritableThreadLocal<Statistics>() {

      protected Statistics initialValue () {

         return new Statistics();
      }
   };

   private final InheritableThreadLocal<Boolean> ENABLED_THREAD_LOCAL = new InheritableThreadLocal<Boolean>() {

      protected Boolean initialValue () {

         return enabled;
      }
   };

   private boolean enabled = false;

   public ThreadLocalStatisticsFactory (boolean enabled) {

      this.enabled = enabled;
   }

   public boolean isEnabled () {

      return ENABLED_THREAD_LOCAL.get();
   }

   public void setEnabled (boolean enabled) {

      ENABLED_THREAD_LOCAL.set(enabled);
   }

   public Statistics getStatistics () {

      if (!isEnabled()) {

         return null;
      }

      return STATISTICS_THREAD_LOCAL.get();
   }

   public Statistics removeStatistics () {

      try {
         if (!isEnabled()) {

            return null;
         }

         return STATISTICS_THREAD_LOCAL.get();
      }
      finally {
         STATISTICS_THREAD_LOCAL.set(new Statistics());
      }
   }
}