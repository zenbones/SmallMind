package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class TableCell extends TableElement {

   private Value value;

   protected TableCell (Value value) {

      if (value == null) {
         throw new IllegalArgumentException("The value must not be null");
      }

      this.value = value;
   }

   public synchronized ValueType getType () {

      return value.getType();
   }

   public synchronized void setValue (Value value) {

      if (!this.value.getType().equals(value.getType())) {
         throw new TypeMismatchException("%s != %s", this.value.getType(), value.getType());
      }

      this.value = value;
   }

   public synchronized Value getValue () {

      return value;
   }
}
