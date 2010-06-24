package org.smallmind.cloud.transport.messaging.service;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ServiceException extends FormattedException {

   public ServiceException () {

      super();
   }

   public ServiceException (String message, Object... args) {

      super(message, args);
   }

   public ServiceException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ServiceException (Throwable throwable) {

      super(throwable);
   }
}
