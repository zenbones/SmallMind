package org.smallmind.wicket;

import org.apache.wicket.WicketRuntimeException;

public class FormattedWicketRuntimeException extends WicketRuntimeException {

   public FormattedWicketRuntimeException () {

      super();
   }

   public FormattedWicketRuntimeException (String message, Object... args) {

      super(String.format(message, args));
   }

   public FormattedWicketRuntimeException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public FormattedWicketRuntimeException (Throwable throwable) {

      super(throwable);
   }
}