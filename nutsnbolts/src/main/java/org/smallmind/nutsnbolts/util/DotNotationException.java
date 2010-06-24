package org.smallmind.nutsnbolts.util;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class DotNotationException extends FormattedException {

   public DotNotationException () {

      super();
   }

   public DotNotationException (String message, Object... args) {

      super(message, args);
   }

   public DotNotationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public DotNotationException (Throwable throwable) {

      super(throwable);
   }
}

