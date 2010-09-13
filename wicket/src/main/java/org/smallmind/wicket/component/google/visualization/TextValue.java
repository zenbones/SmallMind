package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class TextValue extends Value {

   private static TextValue NULL_VALUE = new TextValue(null);

   private String text;

   public static TextValue asNull () {

      return NULL_VALUE;
   }

   public static TextValue create (String text) {

      return (text == null) ? NULL_VALUE : new TextValue(text);
   }

   private TextValue (String text) {

      this.text = text;
   }

   public String getText () {

      return text;
   }

   @Override
   public ValueType getType () {

      return ValueType.TEXT;
   }

   @Override
   public boolean isNull () {

      return (text == null);
   }

   @Override
   public int compareTo (Value value) {

      if (!ValueType.TEXT.equals(value.getType())) {
         throw new TypeMismatchException();
      }

      if (isNull()) {

         return (value.isNull()) ? 0 : -1;
      }
      else if (value.isNull()) {

         return 1;
      }
      else {

         return text.compareTo((((TextValue)value).getText()));
      }
   }

   public String forScript () {

      return (text == null) ? "null" : '\'' + text + '\'';
   }

   public String toString () {

      return (text == null) ? "null" : text;
   }
}


