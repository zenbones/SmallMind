package org.smallmind.nutsnbolts.util;

public class Counter {

   private int count;

   public Counter () {

      count = 0;
   }

   public Counter (int count) {

      this.count = count;
   }

   public int inc () {

      count++;
      return count;
   }

   public int dec () {

      count--;
      return count;
   }

   public int getCount () {

      return count;
   }

}
