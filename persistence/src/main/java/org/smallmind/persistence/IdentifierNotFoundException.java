package org.smallmind.persistence;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class IdentifierNotFoundException extends FormattedException {

   public IdentifierNotFoundException () {

      super();
   }

   public IdentifierNotFoundException (String message, Object... args) {

      super(message, args);
   }

   public IdentifierNotFoundException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public IdentifierNotFoundException (Throwable throwable) {

      super(throwable);
   }
}
