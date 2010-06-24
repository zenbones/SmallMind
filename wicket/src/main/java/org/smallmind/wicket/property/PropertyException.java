package org.smallmind.wicket.property;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class PropertyException extends FormattedException {

   public PropertyException () {

      super();
   }

   public PropertyException (String message, Object... args) {

      super(message, args);
   }

   public PropertyException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public PropertyException (Throwable throwable) {

      super(throwable);
   }
}