package org.smallmind.persistence.orm;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.lang.FormattedException;

public class TransactionPostProcessException extends FormattedException {

   LinkedList<Throwable> subsequentList = new LinkedList<Throwable>();

   public TransactionPostProcessException () {

      super();
   }

   public TransactionPostProcessException (String message, Object... args) {

      super(message, args);
   }

   public TransactionPostProcessException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public TransactionPostProcessException (Throwable throwable) {

      super(throwable);
   }

   public Throwable getFirstCause () {

      return getCause();
   }

   public synchronized void addSubsequentCause (Throwable throwable) {

      subsequentList.add(throwable);
   }

   public synchronized Throwable[] getSubsequentCauses () {

      Throwable[] throwables;

      throwables = new Throwable[subsequentList.size()];
      subsequentList.toArray(throwables);

      return throwables;
   }
}