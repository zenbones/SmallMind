package org.smallmind.nutsnbolts.email;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class SMTPMailException extends FormattedException {

   public SMTPMailException () {

      super();
   }

   public SMTPMailException (String message, Object... args) {

      super(message, args);
   }

   public SMTPMailException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public SMTPMailException (Throwable throwable) {

      super(throwable);
   }
}
