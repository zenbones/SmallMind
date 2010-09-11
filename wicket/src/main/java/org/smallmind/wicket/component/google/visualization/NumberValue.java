package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class NumberValue extends Value {

   private static NumberValue NULL_VALUE = new NumberValue(null);

   private Double number;

   public static NumberValue asNull () {

      return NULL_VALUE;
   }

   public static NumberValue create (double number) {

      return new NumberValue(number);
   }

   public static NumberValue create (Double number) {

      return (number == null) ? NULL_VALUE : new NumberValue(number);
   }

   private NumberValue (Double number) {

      this.number = number;
   }

   public synchronized Double getNumber () {

      return number;
   }

   public synchronized void add (double number) {

      if (this.number == null) {
         throw new UnsupportedOperationException("Can't manipulate a null value");
      }

      this.number += number;
   }

   @Override
   public ValueType getType () {

      return ValueType.NUMBER;
   }

   @Override
   public boolean isNull () {

      return (number == null);
   }

   @Override
   public int compareTo (Value value) {

      if (!ValueType.NUMBER.equals(value.getType())) {
         throw new TypeMismatchException();
      }

      if (isNull()) {

         return (value.isNull()) ? 0 : -1;
      }
      else if (value.isNull()) {

         return 1;
      }
      else {

         return number.compareTo((((NumberValue)value).getNumber()));
      }
   }

   public String toString () {

      return (number == null) ? "null" : number.toString();
   }
}


