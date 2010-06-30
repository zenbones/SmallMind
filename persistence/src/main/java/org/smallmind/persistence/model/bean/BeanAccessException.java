package org.smallmind.persistence.model.bean;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class BeanAccessException extends FormattedException {

   public BeanAccessException () {

      super();
   }

   public BeanAccessException (String message, Object... args) {

      super(message, args);
   }

   public BeanAccessException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public BeanAccessException (Throwable throwable) {

      super(throwable);
   }
}
