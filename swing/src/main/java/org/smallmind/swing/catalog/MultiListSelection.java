package org.smallmind.swing.catalog;

import org.smallmind.nutsnbolts.util.DirectionalComparator;

public class MultiListSelection<T extends Comparable<T>> {

   private T comparable;
   private int index;

   public MultiListSelection (T comparable, int index) {

      this.comparable = comparable;
      this.index = index;
   }

   public T getComparable () {

      return comparable;
   }

   public int getIndex () {

      return index;
   }

   public int compareTo (MultiListSelection<T> selection, DirectionalComparator.Direction direction) {

      if (getComparable().compareTo(selection.getComparable()) < 0) {
         if (direction.equals(DirectionalComparator.Direction.ASCENDING)) {
            return 1;
         }
         else {
            return -1;
         }
      }
      else if (getComparable().compareTo(selection.getComparable()) > 0) {
         if (direction.equals(DirectionalComparator.Direction.ASCENDING)) {
            return -1;
         }
         else {
            return 1;
         }
      }
      else {
         return getIndex() - selection.getIndex();
      }
   }

}
