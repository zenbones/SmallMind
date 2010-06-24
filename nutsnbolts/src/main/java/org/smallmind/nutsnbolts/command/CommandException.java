package org.smallmind.nutsnbolts.command;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class CommandException extends FormattedException {

   public CommandException () {

      super();
   }

   public CommandException (String message, Object... args) {

      super(message, args);
   }

   public CommandException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public CommandException (Throwable throwable) {

      super(throwable);
   }
}

