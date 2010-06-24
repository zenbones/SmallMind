package org.smallmind.persistence.orm.aop;

import org.smallmind.nutsnbolts.lang.FormattedError;

public abstract class SessionError extends FormattedError {

   private int closure;

   public SessionError (int closure) {

      super();

      this.closure = closure;
   }

   public SessionError (int closure, String message, Object... args) {

      super(message, args);

      this.closure = closure;
   }

   public SessionError (int closure, Throwable throwable, String message, Object... args) {

      super(throwable, message, args);

      this.closure = closure;
   }

   public SessionError (int closure, Throwable throwable) {

      super(throwable);

      this.closure = closure;
   }

   public int getClosure () {

      return closure;
   }
}
