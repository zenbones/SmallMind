package org.smallmind.persistence.cache.aop;

import org.smallmind.nutsnbolts.lang.FormattedError;

public class CacheAutomationError extends FormattedError {

   public CacheAutomationError () {

      super();
   }

   public CacheAutomationError (String message, Object... args) {

      super(message, args);
   }

   public CacheAutomationError (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public CacheAutomationError (Throwable throwable) {

      super(throwable);
   }
}
