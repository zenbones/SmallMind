package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class BooleanValue extends Value {

   private static BooleanValue NULL_VALUE = new BooleanValue(null);

   private Boolean logic;

   public static BooleanValue asNull () {

      return NULL_VALUE;
   }

   public static BooleanValue create (boolean logic) {

      return new BooleanValue(logic);
   }

   public static BooleanValue create (Boolean logic) {

      return (logic == null) ? NULL_VALUE : new BooleanValue(logic);
   }

   private BooleanValue (Boolean logic) {

      this.logic = logic;
   }

   public Boolean getBoolean () {

      return logic;
   }

   @Override
   public ValueType getType () {

      return ValueType.BOOLEAN;
   }

   @Override
   public boolean isNull () {

      return (logic == null);
   }

   @Override
   public int compareTo (Value value) {

      if (!ValueType.BOOLEAN.equals(value.getType())) {
         throw new TypeMismatchException();
      }

      if (isNull()) {

         return (value.isNull()) ? 0 : -1;
      }
      else if (value.isNull()) {

         return 1;
      }
      else {

         return logic.compareTo((((BooleanValue)value).getBoolean()));
      }
   }

   @Override
   public String forScript () {

      return toString();
   }

   public String toString () {

      return (logic == null) ? "null" : logic.toString();
   }
}


