package org.smallmind.persistence.orm;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class ProxyTransactionException extends FormattedRuntimeException {

   public ProxyTransactionException () {

      super();
   }

   public ProxyTransactionException (String message, Object... args) {

      super(message, args);
   }

   public ProxyTransactionException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ProxyTransactionException (Throwable throwable) {

      super(throwable);
   }
}
