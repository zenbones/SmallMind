package org.smallmind.persistence;

import java.util.Comparator;

public class TemporalComparator<I extends Comparable<I>, D extends Durable<I> & Temporal> implements Comparator<D> {

   public int compare (D durable1, D durable2) {

      int dateComparison;

      if ((dateComparison = durable2.getComparableDate().compareTo(durable1.getComparableDate())) != 0) {

         return dateComparison;
      }

      if (durable1.getId() == null) {
         if (durable2.getId() == null) {

            return 0;
         }
         else {

            return -1;
         }
      }

      if (durable2.getId() == null) {

         return 1;
      }

      return durable2.getId().compareTo(durable1.getId());
   }
}
