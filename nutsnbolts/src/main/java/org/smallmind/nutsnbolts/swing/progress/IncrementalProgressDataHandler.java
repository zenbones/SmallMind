package org.smallmind.nutsnbolts.swing.progress;

public class IncrementalProgressDataHandler implements ProgressDataHandler {

   private long length;
   private long index = 0;

   public IncrementalProgressDataHandler (int length) {

      this.length = length;
   }

   public long getLength () {

      return length;
   }

   public synchronized long getIndex () {

      return index;
   }

   public synchronized void setIndex (long index) {

      this.index = index;
   }

}
