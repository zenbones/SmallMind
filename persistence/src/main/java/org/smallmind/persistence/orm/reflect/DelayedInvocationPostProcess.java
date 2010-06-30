package org.smallmind.persistence.orm.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcess;

public class DelayedInvocationPostProcess extends TransactionPostProcess {

   private Object delayedTarget;
   private Method delayedMethod;
   private Object[] delayedArguments;

   public DelayedInvocationPostProcess (TransactionEndState endState, ProcessPriority priority, Object delayedTarget, Method delayedMethod, Object... delayedArguments) {

      super(endState, priority);

      this.delayedTarget = delayedTarget;
      this.delayedMethod = delayedMethod;
      this.delayedArguments = delayedArguments;
   }

   public void process ()
      throws Exception {

      try {
         delayedMethod.invoke(delayedTarget, delayedArguments);
      }
      catch (InvocationTargetException invocationTargetException) {
         if ((invocationTargetException.getCause() != null) && (invocationTargetException.getCause() instanceof Exception)) {
            throw (Exception)invocationTargetException.getCause();
         }
      }
   }
}

