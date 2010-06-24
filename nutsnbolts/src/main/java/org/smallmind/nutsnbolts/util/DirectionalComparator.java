package org.smallmind.nutsnbolts.util;

import java.util.Comparator;

public abstract class DirectionalComparator<T> implements Comparator<T> {

   public static enum Direction {

      DESCENDING, ASCENDING
   }

   private Direction direction;

   public DirectionalComparator (Direction direction) {

      this.direction = direction;
   }

   public Direction getDirection () {

      return direction;
   }

}
