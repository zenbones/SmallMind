package org.smallmind.persistence.orm.aop;

import java.util.LinkedList;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;

public class TransactionalState {

   private static final ThreadLocal<LinkedList<RollbackAwareBoundarySet<ProxyTransaction>>> TRANSACTION_SET_STACK_LOCAL = new ThreadLocal<LinkedList<RollbackAwareBoundarySet<ProxyTransaction>>>();

   public static boolean isInTransaction () {

      return isInTransaction(null);
   }

   public static boolean isInTransaction (String dataSource) {

      return currentTransaction(dataSource) != null;
   }

   public static ProxyTransaction currentTransaction (String dataSource) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

      if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
         for (RollbackAwareBoundarySet<ProxyTransaction> transactionSet : transactionSetStack) {
            for (ProxyTransaction proxyTransaction : transactionSet) {
               if (dataSource == null) {
                  if (proxyTransaction.getSession().getDataSource() == null) {

                     return proxyTransaction;
                  }
               }
               else if (dataSource.equals(proxyTransaction.getSession().getDataSource())) {

                  return proxyTransaction;
               }
            }
         }
      }

      return null;
   }

   public static boolean withinBoundary (ProxySession proxySession) {

      return withinBoundary(proxySession.getDataSource());
   }

   public static boolean withinBoundary (String dataSource) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

      if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
         for (RollbackAwareBoundarySet<ProxyTransaction> transactionSet : transactionSetStack) {
            if (transactionSet.allows(dataSource)) {
               return true;
            }
         }
      }

      return false;
   }

   public static RollbackAwareBoundarySet<ProxyTransaction> obtainBoundary (ProxySession proxySession) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

      if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
         for (RollbackAwareBoundarySet<ProxyTransaction> transactionSet : transactionSetStack) {
            if (transactionSet.allows(proxySession)) {
               if (NonTransactionalState.containsSession(proxySession)) {
                  throw new StolenTransactionError("Attempt to steal the session - a non-transactional boundary is already enforced");
               }

               return transactionSet;
            }
         }
      }

      return null;
   }

   protected static void startBoundary (Transactional transactional) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

      if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) {
         TRANSACTION_SET_STACK_LOCAL.set(transactionSetStack = new LinkedList<RollbackAwareBoundarySet<ProxyTransaction>>());
      }

      transactionSetStack.addLast(new RollbackAwareBoundarySet<ProxyTransaction>(transactional.dataSources(), transactional.implicit(), transactional.rollbackOnly()));
   }

   protected static void commitBoundary ()
      throws TransactionError {

      commitBoundary(null);
   }

   protected static void commitBoundary (Throwable throwable)
      throws TransactionError {

      if ((throwable == null) || (!(throwable instanceof TransactionError)) || ((TRANSACTION_SET_STACK_LOCAL.get() != null) && (TRANSACTION_SET_STACK_LOCAL.get().size() != ((TransactionError)throwable).getClosure()))) {

         LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;
         RollbackAwareBoundarySet<ProxyTransaction> transactionSet;
         IncompleteTransactionError incompleteTransactionError = null;

         if (((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) || transactionSetStack.isEmpty()) {
            throw new TransactionBoundaryError(0, "No transaction boundary has been enforced");
         }

         try {
            for (ProxyTransaction proxyTransaction : transactionSet = transactionSetStack.removeLast()) {
               try {
                  if (transactionSet.isRollbackOnly() || proxyTransaction.isRollbackOnly()) {
                     proxyTransaction.rollback();
                  }
                  else {
                     proxyTransaction.commit();
                  }
               }
               catch (Throwable unexpectedThrowable) {
                  if (incompleteTransactionError == null) {
                     incompleteTransactionError = new IncompleteTransactionError(transactionSetStack.size(), unexpectedThrowable);
                  }
               }
            }

            if (incompleteTransactionError != null) {
               throw incompleteTransactionError;
            }
         }
         finally {
            if (transactionSetStack.isEmpty()) {
               TRANSACTION_SET_STACK_LOCAL.remove();
            }
         }
      }
      else {
         TRANSACTION_SET_STACK_LOCAL.remove();
      }
   }

   protected static void rollbackBoundary (Throwable throwable)
      throws TransactionError {

      if ((throwable == null) || (!(throwable instanceof TransactionError)) || ((TRANSACTION_SET_STACK_LOCAL.get() != null) && (TRANSACTION_SET_STACK_LOCAL.get().size() != ((TransactionError)throwable).getClosure()))) {

         LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;
         IncompleteTransactionError incompleteTransactionError = null;

         if (((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) || transactionSetStack.isEmpty()) {
            throw new TransactionBoundaryError(0, throwable, "No transaction boundary has been enforced");
         }

         try {
            for (ProxyTransaction proxyTransaction : transactionSetStack.removeLast()) {
               try {
                  proxyTransaction.rollback();
               }
               catch (Throwable unexpectedThrowable) {
                  if (incompleteTransactionError == null) {
                     incompleteTransactionError = new IncompleteTransactionError(transactionSetStack.size(), unexpectedThrowable);
                  }
               }
            }

            if (incompleteTransactionError != null) {
               throw incompleteTransactionError;
            }
         }
         finally {
            if (transactionSetStack.isEmpty()) {
               TRANSACTION_SET_STACK_LOCAL.remove();
            }
         }
      }
      else {
         TRANSACTION_SET_STACK_LOCAL.remove();
      }
   }
}
