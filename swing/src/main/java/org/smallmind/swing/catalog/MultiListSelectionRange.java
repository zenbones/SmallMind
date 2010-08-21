package org.smallmind.swing.catalog;

import org.smallmind.nutsnbolts.util.DirectionalComparator;

public class MultiListSelectionRange<T extends Comparable<T>> {

   public static enum Containment {

      SURROUNDS, HEAD, TAIL, WITHIN, OUT
   }

   ;

   private MultiListSelection<T> firstSelection;
   private MultiListSelection<T> lastSelection;
   private DirectionalComparator.Direction direction;

   public MultiListSelectionRange (MultiListSelection<T> selection0, MultiListSelection<T> selection1, DirectionalComparator.Direction direction) {

      int comparison;

      this.direction = direction;

      comparison = selection0.compareTo(selection1, direction);

      if (comparison <= 0) {
         firstSelection = selection0;
         lastSelection = selection1;
      }
      else {
         firstSelection = selection1;
         lastSelection = selection0;
      }
   }

   public MultiListSelection<T> getFirstSelection () {

      return firstSelection;
   }

   public MultiListSelection<T> getLastSelection () {

      return lastSelection;
   }

   public Containment getContainment (T key) {

      int firstComparison;
      int lastComparison;

      firstComparison = firstSelection.getComparable().compareTo(key);
      lastComparison = lastSelection.getComparable().compareTo(key);

      if (direction.equals(DirectionalComparator.Direction.ASCENDING)) {
         firstComparison *= -1;
         lastComparison *= -1;
      }

      if ((firstComparison == 0) && (lastComparison == 0)) {
         return Containment.SURROUNDS;
      }
      else if (firstComparison == 0) {
         return Containment.HEAD;
      }
      else if (lastComparison == 0) {
         return Containment.TAIL;
      }
      else if ((firstComparison < 0) && (lastComparison > 0)) {
         return Containment.WITHIN;
      }
      else {
         return Containment.OUT;
      }
   }

}
