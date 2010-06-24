package org.smallmind.nutsnbolts.util.concurrent;

public class Lock {

   private boolean[] tumblers;

   public Lock (int size) {

      tumblers = new boolean[size];

      for (int count = 0; count < tumblers.length; count++) {
         tumblers[count] = true;
      }
   }

   public void lock (int condition) {

      set(condition, true);
   }

   public void unlock (int condition) {

      set(condition, false);
   }

   public synchronized boolean isLocked (int condition) {

      return tumblers[condition];
   }

   public synchronized void set (int condition, boolean value) {

      tumblers[condition] = value;

      for (boolean tumbler : tumblers) {
         if (tumbler) {
            return;
         }
      }

      notifyAll();
   }

}
