package org.smallmind.swing.table;

public class SortableColumnTracker<E extends Enum> {

   private E enumDataType;
   private SortableDirection direction;

   public SortableColumnTracker (E enumDataType, SortableDirection direction) {

      this.enumDataType = enumDataType;
      this.direction = direction;
   }

   public E getEnumDataType () {

      return enumDataType;
   }

   public synchronized SortableDirection getDirection () {

      return direction;
   }

   public synchronized void setDirection (SortableDirection direction) {

      this.direction = direction;
   }

   public int hashCode () {

      return enumDataType.hashCode();
   }

   public boolean equals (Object obj) {

      if (obj instanceof SortableColumnTracker) {
         return enumDataType.equals(((SortableColumnTracker)obj).getEnumDataType());
      }

      return false;
   }

   public String toString () {

      StringBuilder displayBuilder;

      displayBuilder = new StringBuilder("Tracker[name=");
      displayBuilder.append(enumDataType.name());
      displayBuilder.append(";direction=");
      displayBuilder.append(direction.name());
      displayBuilder.append(']');

      return displayBuilder.toString();
   }

}