package org.smallmind.swing.memory;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.swing.event.MemoryUsageEvent;
import org.smallmind.swing.event.MemoryUsageListener;

public class MemoryTimer implements java.lang.Runnable {

   private static final String[] scalingTitles = {" bytes", "k", "M", "G", "T", "P", "E"};
   private static final long pulseTime = 3000;

   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private WeakEventListenerList<MemoryUsageListener> listenerList;

   public MemoryTimer () {

      listenerList = new WeakEventListenerList<MemoryUsageListener>();

      pulseLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
   }

   public synchronized void addMemoryUsageListener (MemoryUsageListener memoryUsageListener) {

      listenerList.addListener(memoryUsageListener);
   }

   public synchronized void removeMemoryUsageListener (MemoryUsageListener memoryUsageListener) {

      listenerList.removeListener(memoryUsageListener);
   }

   public void finish ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
         pulseLatch.countDown();
      }

      exitLatch.await();
   }

   public void run () {

      Runtime runtime;
      long totalMemory;
      long freeMemory;
      long usedMemory;
      int scalingFactor;
      int scalingUnit;
      int maximumUsage;
      int currentUsage;

      runtime = Runtime.getRuntime();

      while (!finished.get()) {
         totalMemory = runtime.totalMemory();
         freeMemory = runtime.freeMemory();
         usedMemory = totalMemory - freeMemory;

         scalingFactor = 1;
         scalingUnit = 0;

         while ((usedMemory / scalingFactor) >= 1024) {
            scalingFactor *= 1024;
            scalingUnit++;
         }

         if ((scalingUnit > 0) && ((usedMemory / scalingFactor) == (totalMemory / scalingFactor))) {
            scalingFactor /= 1024;
            scalingUnit--;
         }

         maximumUsage = (int)(totalMemory / scalingFactor);
         currentUsage = (int)(usedMemory / scalingFactor);

         fireMemoryUsageUpdate(maximumUsage, currentUsage, Integer.toString(currentUsage) + scalingTitles[scalingUnit] + " of " + Integer.toString(maximumUsage) + scalingTitles[scalingUnit]);

         try {
            pulseLatch.await(pulseTime, TimeUnit.MILLISECONDS);
         }
         catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(MemoryTimer.class).error(interruptedException);
         }
      }

      exitLatch.countDown();
   }

   private void fireMemoryUsageUpdate (int maximumUsage, int currentUsage, String displayUsage) {

      Iterator<MemoryUsageListener> listenerIter = listenerList.getListeners();
      MemoryUsageEvent memoryUsageEvent;

      memoryUsageEvent = new MemoryUsageEvent(this, maximumUsage, currentUsage, displayUsage);
      while (listenerIter.hasNext()) {
         listenerIter.next().usageUpdate(memoryUsageEvent);
      }
   }

}
