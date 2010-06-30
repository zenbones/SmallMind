package org.smallmind.persistence.model.bean;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class BeanInvocationException extends FormattedException {

   public BeanInvocationException () {

      super();
   }

   public BeanInvocationException (String message, Object... args) {

      super(message, args);
   }

   public BeanInvocationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public BeanInvocationException (Throwable throwable) {

      super(throwable);
   }
}