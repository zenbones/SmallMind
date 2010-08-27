package org.smallmind.seda;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class StopWatch {

   private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
   private static final boolean CURRENT_THREAD_CPU_TIME_SUPPORTED = THREAD_MX_BEAN.isCurrentThreadCpuTimeSupported();

   private boolean initialized = false;
   private long startMillis;
   private long clickClockMillis;
   private long clickClockNanos;
   private long durationNanos;
   private long clickCPUNanos;
   private long cpuTimeNanos;

   public StopWatch click () {

      if (!initialized) {
         System.out.println(CURRENT_THREAD_CPU_TIME_SUPPORTED);
         initialized = true;

         clickClockMillis = System.currentTimeMillis();
         clickClockNanos = System.nanoTime();
      }
      else {

         long stopNanos;

         startMillis = clickClockMillis;
         clickClockMillis = System.currentTimeMillis();

         durationNanos = (stopNanos = System.nanoTime()) - clickClockNanos;
         clickClockNanos = stopNanos;

         if (!CURRENT_THREAD_CPU_TIME_SUPPORTED) {
            cpuTimeNanos = durationNanos;
         }
         else {

            long stopCPUNanos;

            cpuTimeNanos = (stopCPUNanos = THREAD_MX_BEAN.getCurrentThreadCpuTime()) - clickCPUNanos;
            clickCPUNanos = stopCPUNanos;
         }
      }

      return this;
   }

   public long getStartMillis () {

      return startMillis;
   }

   public long getDurationNanos () {

      return durationNanos;
   }

   public long getCpuTimeNanos () {

      return cpuTimeNanos;
   }
}
