package org.smallmind.nutsnbolts.util;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class PropertyExpanderException extends FormattedException {

   public PropertyExpanderException () {

      super();
   }

   public PropertyExpanderException (String message, Object... args) {

      super(message, args);
   }

   public PropertyExpanderException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public PropertyExpanderException (Throwable throwable) {

      super(throwable);
   }
}