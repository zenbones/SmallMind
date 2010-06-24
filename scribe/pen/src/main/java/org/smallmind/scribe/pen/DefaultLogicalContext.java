package org.smallmind.scribe.pen;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultLogicalContext implements LogicalContext {

   private StackTraceElement contextElement;
   private AtomicBoolean filled = new AtomicBoolean(false);

   public boolean isFilled () {

      return filled.get();
   }

   public void fillIn () {

      setContextElement();
   }

   public String getClassName () {

      setContextElement();

      return contextElement.getClassName();
   }

   public String getMethodName () {

      setContextElement();

      return contextElement.getMethodName();
   }

   public String getFileName () {

      setContextElement();

      return contextElement.getFileName();
   }

   public boolean isNativeMethod () {

      setContextElement();

      return contextElement.isNativeMethod();
   }

   public int getLineNumber () {

      setContextElement();

      return contextElement.getLineNumber();
   }

   public void setContextElement () {

      if (!filled.get()) {
         synchronized (this) {
            if (!filled.get()) {

               boolean primed = false;

               for (StackTraceElement currentElement : Thread.currentThread().getStackTrace()) {
                  if (primed) {
                     if (!willPrime(currentElement.getClassName())) {
                        contextElement = currentElement;
                        break;
                     }
                  }
                  else {
                     primed = willPrime(currentElement.getClassName());
                  }
               }

               if (!primed || (contextElement == null)) {
                  throw new IllegalStateException("The logging call context was not found");
               }

               filled.set(true);
            }
         }
      }
   }

   private static boolean willPrime (String className) {

      return LoggerManager.isLoggingClass(className);
   }
}