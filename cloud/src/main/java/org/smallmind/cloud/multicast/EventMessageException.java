package org.smallmind.cloud.multicast;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class EventMessageException extends FormattedException {

   public EventMessageException () {

      super();
   }

   public EventMessageException (String message, Object... args) {

      super(message, args);
   }

   public EventMessageException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public EventMessageException (Throwable throwable) {

      super(throwable);
   }
}

