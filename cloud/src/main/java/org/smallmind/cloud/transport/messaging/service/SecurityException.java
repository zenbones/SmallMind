package org.smallmind.cloud.transport.messaging.service;

public class SecurityException extends ServiceException {

   public SecurityException () {

      super();
   }

   public SecurityException (String message, Object... args) {

      super(message, args);
   }

   public SecurityException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public SecurityException (Throwable throwable) {

      super(throwable);
   }
}