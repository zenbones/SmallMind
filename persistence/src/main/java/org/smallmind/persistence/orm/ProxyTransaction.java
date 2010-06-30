package org.smallmind.persistence.orm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;

public abstract class ProxyTransaction {

   private static final ProcessPriorityComparator PROCESS_PRIORITY_COMPARATOR = new ProcessPriorityComparator();

   private ProxySession proxySession;
   private LinkedList<TransactionPostProcess> postProcessList;
   private String uuidAsString;
   private boolean rollbackOnly = false;

   public ProxyTransaction (ProxySession proxySession) {

      this.proxySession = proxySession;

      uuidAsString = UUID.randomUUID().toString();
   }

   public abstract void flush ();

   public abstract void commit ();

   public abstract void rollback ();

   public abstract boolean isCompleted ();

   public String getUniqueId () {

      return uuidAsString;
   }

   public ProxySession getSession () {

      return proxySession;
   }

   public void setRollbackOnly () {

      rollbackOnly = true;
   }

   public boolean isRollbackOnly () {

      return rollbackOnly;
   }

   private LinkedList<TransactionPostProcess> getPostProcessList () {

      return (postProcessList == null) ? postProcessList = new LinkedList<TransactionPostProcess>() : postProcessList;
   }

   public void addPostProcess (TransactionPostProcess postProcess) {

      getPostProcessList().add(postProcess);
   }

   protected void applyPostProcesses (TransactionEndState endState)
      throws TransactionPostProcessException {

      TransactionPostProcessException postProcessException = null;

      Collections.sort(getPostProcessList(), PROCESS_PRIORITY_COMPARATOR);
      for (TransactionPostProcess postProcess : getPostProcessList()) {
         if (postProcess.getEndState().equals(TransactionEndState.ANY) || postProcess.getEndState().equals(endState)) {
            try {
               postProcess.process();
            }
            catch (Exception exception) {
               if (postProcessException == null) {
                  postProcessException = new TransactionPostProcessException(exception);
               }
               else {
                  postProcessException.addSubsequentCause(exception);
               }
            }
         }
      }

      if (postProcessException != null) {
         throw postProcessException;
      }
   }

   public int hashCode () {

      return uuidAsString.hashCode();
   }

   public boolean equals (Object obj) {

      return (obj instanceof ProxyTransaction) && uuidAsString.equals(((ProxyTransaction)obj).getUniqueId());
   }
}
