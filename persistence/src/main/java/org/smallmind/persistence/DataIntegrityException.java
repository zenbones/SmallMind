package org.smallmind.persistence;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class DataIntegrityException extends FormattedRuntimeException {

   public DataIntegrityException () {

      super();
   }

   public DataIntegrityException (String message, Object... args) {

      super(message, args);
   }

   public DataIntegrityException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public DataIntegrityException (Throwable throwable) {

      super(throwable);
   }
}