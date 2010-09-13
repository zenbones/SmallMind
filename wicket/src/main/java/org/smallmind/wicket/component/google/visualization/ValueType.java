package org.smallmind.wicket.component.google.visualization;

public enum ValueType {

   TEXT("string") {

      @Override
      public Value asNull () {

         return TextValue.asNull();
      }
   },
   NUMBER("number") {

      @Override
      public Value asNull () {

         return NumberValue.asNull();
      }
   },
   BOOLEAN("boolean") {

      @Override
      public Value asNull () {

         return BooleanValue.asNull();
      }
   },
   DATE("date") {

      @Override
      public Value asNull () {

         return DateValue.asNull();
      }
   },
   DATETIME("datetime") {

      @Override
      public Value asNull () {

         return DateTimeValue.asNull();
      }
   },
   TIMEOFDAY("timeofday") {

      @Override
      public Value asNull () {

         return TimeOfDayValue.asNull();
      }
   };

   private String scriptVersion;

   private ValueType (String scriptVersion) {

      this.scriptVersion = scriptVersion;
   }

   public String getScriptVersion () {

      return scriptVersion;
   }

   public abstract Value asNull ();
}
