package org.smallmind.persistence.cache;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class CacheOperationException extends FormattedRuntimeException {

   public CacheOperationException () {

      super();
   }

   public CacheOperationException (String message, Object... args) {

      super(message, args);
   }

   public CacheOperationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public CacheOperationException (Throwable throwable) {

      super(throwable);
   }
}
