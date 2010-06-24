package org.smallmind.persistence.orm.type;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class TypeFormatException extends FormattedException {

   public TypeFormatException () {

      super();
   }

   public TypeFormatException (String message, Object... args) {

      super(message, args);
   }

   public TypeFormatException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public TypeFormatException (Throwable throwable) {

      super(throwable);
   }

}
