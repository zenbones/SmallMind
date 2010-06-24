package org.smallmind.nutsnbolts.util;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class NaturalDirectionalComparator<T extends Comparable<T>> extends DirectionalComparator<T> {

   public NaturalDirectionalComparator (Direction direction) {

      super(direction);
   }

   public int compare (T t1, T t2) {

      switch (getDirection()) {
         case DESCENDING:
            return t1.compareTo(t2);
         case ASCENDING:
            return t1.compareTo(t2) * -1;
         default:
            throw new UnknownSwitchCaseException(getDirection().name());
      }
   }

}
