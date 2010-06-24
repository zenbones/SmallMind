package org.smallmind.nutsnbolts.spring;

import org.springframework.beans.BeansException;

public class RuntimeBeansException extends BeansException {

   public RuntimeBeansException (String message, Object... args) {

      super(String.format(message, args));
   }

   public RuntimeBeansException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public RuntimeBeansException (Throwable throwable) {

      super(throwable.getMessage(), throwable);
   }
}
