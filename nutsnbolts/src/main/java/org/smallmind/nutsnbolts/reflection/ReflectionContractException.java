package org.smallmind.nutsnbolts.reflection;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ReflectionContractException extends FormattedException {

   public ReflectionContractException () {

      super();
   }

   public ReflectionContractException (String message, Object... args) {

      super(message, args);
   }

   public ReflectionContractException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ReflectionContractException (Throwable throwable) {

      super(throwable);
   }
}

