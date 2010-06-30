package org.smallmind.persistence.orm.sql;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class PooledConnectionException extends FormattedException {

   public PooledConnectionException () {

      super();
   }

   public PooledConnectionException (String message, Object... args) {

      super(message, args);
   }

   public PooledConnectionException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public PooledConnectionException (Throwable throwable) {

      super(throwable);
   }
}
