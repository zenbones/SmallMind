package org.smallmind.nagios;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class NagiosException extends FormattedException {

   public NagiosException () {

      super();
   }

   public NagiosException (String message, Object... args) {

      super(message, args);
   }

   public NagiosException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public NagiosException (Throwable throwable) {

      super(throwable);
   }
}