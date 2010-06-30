package org.smallmind.persistence.orm;

public abstract class TransactionPostProcess {

   private TransactionEndState endState;
   private ProcessPriority priority;

   public TransactionPostProcess (TransactionEndState endState, ProcessPriority priority) {

      this.endState = endState;
      this.priority = priority;
   }

   public abstract void process ()
      throws Exception;

   public TransactionEndState getEndState () {

      return endState;
   }

   public ProcessPriority getPriority () {

      return priority;
   }
}
