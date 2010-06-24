package org.smallmind.nutsnbolts.lang;

import java.util.LinkedList;

public class MultipleException extends FormattedException {

   private LinkedList<Exception> causeList;

   public MultipleException () {

      super();

      causeList = new LinkedList<Exception>();
   }

   public MultipleException (String message, Object... args) {

      super(message, args);

      causeList = new LinkedList<Exception>();
   }

   public MultipleException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);

      causeList = new LinkedList<Exception>();
   }

   public MultipleException (Throwable throwable) {

      super(throwable);

      causeList = new LinkedList<Exception>();
   }

   public void addException (Exception exception) {

      causeList.add(exception);
   }

   public Exception[] getExceptions () {

      Exception[] causes;

      causes = new Exception[causeList.size()];
      causeList.toArray(causes);

      return causes;
   }
}
