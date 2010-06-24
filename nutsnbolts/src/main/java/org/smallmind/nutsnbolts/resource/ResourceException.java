package org.smallmind.nutsnbolts.resource;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ResourceException extends FormattedException {

   public ResourceException () {

      super();
   }

   public ResourceException (String message, Object... args) {

      super(message, args);
   }

   public ResourceException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ResourceException (Throwable throwable) {

      super(throwable);
   }
}
