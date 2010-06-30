package org.smallmind.persistence.orm.aop;

import java.util.LinkedList;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;

public class NonTransactionalState {

   private static final ThreadLocal<LinkedList<BoundarySet<ProxySession>>> SESSION_SET_STACK_LOCAL = new ThreadLocal<LinkedList<BoundarySet<ProxySession>>>();

   public static boolean isInSession () {

      return isInSession(null);
   }

   public static boolean isInSession (String dataSource) {

      return currentSession(dataSource) != null;
   }

   public static ProxySession currentSession (String dataSource) {

      LinkedList<BoundarySet<ProxySession>> sessionSetStack;
      ProxyTransaction currentTransaction;

      if ((currentTransaction = TransactionalState.currentTransaction(dataSource)) != null) {

         return currentTransaction.getSession();
      }

      if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) != null) {
         for (BoundarySet<ProxySession> sessionSet : sessionSetStack) {
            for (ProxySession proxySession : sessionSet) {
               if (dataSource == null) {
                  if (proxySession.getDataSource() == null) {

                     return proxySession;
                  }
               }
               else if (dataSource.equals(proxySession.getDataSource())) {

                  return proxySession;
               }
            }
         }
      }

      return null;
   }

   protected static boolean containsSession (ProxySession proxySession) {

      LinkedList<BoundarySet<ProxySession>> sessionSetStack;

      if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) != null) {
         for (BoundarySet<ProxySession> sessionSet : sessionSetStack) {
            if (sessionSet.contains(proxySession)) {
               return true;
            }
         }
      }

      return false;
   }

   public static BoundarySet<ProxySession> obtainBoundary (ProxySession proxySession) {

      LinkedList<BoundarySet<ProxySession>> sessionSetStack;

      if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) != null) {
         for (BoundarySet<ProxySession> sessionSet : sessionSetStack) {
            if (sessionSet.allows(proxySession)) {

               return sessionSet;
            }
         }
      }

      return null;
   }

   protected static void startBoundary (NonTransactional nonTransactional) {

      LinkedList<BoundarySet<ProxySession>> sessionSetStack;

      if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) == null) {
         SESSION_SET_STACK_LOCAL.set(sessionSetStack = new LinkedList<BoundarySet<ProxySession>>());
      }

      sessionSetStack.addLast(new BoundarySet<ProxySession>(nonTransactional.dataSources(), nonTransactional.implicit()));
   }

   protected static void endBoundary (Throwable throwable)
      throws SessionError {

      if ((throwable == null) || (!(throwable instanceof SessionError)) || ((SESSION_SET_STACK_LOCAL.get() != null) && (SESSION_SET_STACK_LOCAL.get().size() != ((SessionError)throwable).getClosure()))) {

         LinkedList<BoundarySet<ProxySession>> sessionSetStack;
         UnexpectedSessionError unexpectedSessionError = null;

         if (((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) == null) || sessionSetStack.isEmpty()) {
            throw new SessionBoundaryError(0, throwable, "No session boundary has been enforced");
         }

         try {
            for (ProxySession proxySession : sessionSetStack.removeLast()) {
               try {
                  proxySession.close();
               }
               catch (Throwable unexpectedThrowable) {
                  if (unexpectedSessionError == null) {
                     unexpectedSessionError = new UnexpectedSessionError(sessionSetStack.size(), unexpectedThrowable);
                  }
               }
            }

            if (unexpectedSessionError != null) {
               throw unexpectedSessionError;
            }
         }
         finally {
            if (sessionSetStack.isEmpty()) {
               SESSION_SET_STACK_LOCAL.remove();
            }
         }
      }
      else {
         SESSION_SET_STACK_LOCAL.remove();
      }
   }
}
