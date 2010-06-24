package org.smallmind.nutsnbolts.swing.memory;

import java.util.Iterator;
import org.smallmind.nutsnbolts.swing.event.MemoryUsageEvent;
import org.smallmind.nutsnbolts.swing.event.MemoryUsageListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class MemoryTimer implements java.lang.Runnable {

   private static final String[] scalingTitles = {" bytes", "k", "M", "G", "T", "P", "E"};
   private static final long pulseTime = 3000;

   private Thread runnableThread = null;
   private WeakEventListenerList<MemoryUsageListener> listenerList;
   private boolean finished = false;
   private boolean exited = false;

   public MemoryTimer () {

      listenerList = new WeakEventListenerList<MemoryUsageListener>();
   }

   public synchronized void addMemoryUsageListener (MemoryUsageListener memoryUsageListener) {

      listenerList.addListener(memoryUsageListener);
   }

   public synchronized void removeMemoryUsageListener (MemoryUsageListener memoryUsageListener) {

      listenerList.removeListener(memoryUsageListener);
   }

   public void finish () {

      finished = true;

      while (!exited) {
         runnableThread.interrupt();

         try {
            Thread.sleep(100);
         }
         catch (InterruptedException i) {
         }
      }
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

      runnableThread = Thread.currentThread();
      runtime = Runtime.getRuntime();

      while (!finished) {
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
            Thread.sleep(pulseTime);
         }
         catch (InterruptedException i) {
         }
      }
      exited = true;
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
