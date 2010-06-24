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

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

      if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
         for (RollbackAwareBoundarySet<ProxyTransaction> transactionSet : transactionSetStack) {
            for (ProxyTransaction proxyTransaction : transactionSet) {
               if (dataSource == null) {
                  if (proxyTransaction.getSession().getDataSource() == null) {

                     return true;
                  }
               }
               else if (dataSource.equals(proxyTransaction.getSession().getDataSource())) {

                  return true;
               }
            }
         }
      }

      return false;
   }

   public static boolean addTransaction (ProxySession proxySession) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;
      RollbackAwareBoundarySet<ProxyTransaction> transactionSet;

      if (((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) && (!transactionSetStack.isEmpty())) {
         if ((transactionSet = transactionSetStack.getLast()).allows(proxySession)) {
            NonTransactionalState.removeSession(proxySession);
            transactionSet.add(proxySession.beginTransaction());

            return true;
         }
      }

      return false;
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
   }
}
