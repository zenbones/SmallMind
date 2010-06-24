package org.smallmind.nutsnbolts.context;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ContextException extends FormattedException {

   public ContextException () {

      super();
   }

   public ContextException (String message, Object... args) {

      super(message, args);
   }

   public ContextException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ContextException (Throwable throwable) {

      super(throwable);
   }
}