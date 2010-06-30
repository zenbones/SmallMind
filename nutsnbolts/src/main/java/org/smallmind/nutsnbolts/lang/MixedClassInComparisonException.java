package org.smallmind.nutsnbolts.lang;

public class MixedClassInComparisonException extends FormattedRuntimeException {

   public MixedClassInComparisonException () {

      super();
   }

   public MixedClassInComparisonException (String message, Object... args) {

      super(message, args);
   }

   public MixedClassInComparisonException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public MixedClassInComparisonException (Throwable throwable) {

      super(throwable);
   }
}