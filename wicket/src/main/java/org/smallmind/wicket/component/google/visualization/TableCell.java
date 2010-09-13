package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class TableCell extends TableElement {

   private Value value;
   private String formattedValue;

   protected TableCell (Value value) {

      this(value, null);
   }

   protected TableCell (Value value, String formattedValue) {

      if (value == null) {
         throw new IllegalArgumentException("The value must not be null");
      }

      this.value = value;
      this.formattedValue = formattedValue;
   }

   public synchronized ValueType getType () {

      return value.getType();
   }

   public synchronized void setValue (Value value) {

      setValue(value, null);
   }

   public synchronized void setValue (Value value, String formattedValue) {

      if (!this.value.getType().equals(value.getType())) {
         throw new TypeMismatchException("%s != %s", this.value.getType(), value.getType());
      }

      this.value = value;
      this.formattedValue = formattedValue;
   }

   public synchronized Value getValue () {

      return value;
   }

   public synchronized String getFormattedValue () {

      return formattedValue;
   }
}
