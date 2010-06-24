package org.smallmind.cloud.transport;

import java.io.Serializable;
import org.smallmind.nutsnbolts.context.Context;

public class InvocationSignal implements Serializable {

   private Context[] contexts;
   private FauxMethod fauxMethod;
   private Object[] args;

   public InvocationSignal (Context[] contexts, FauxMethod fauxMethod, Object[] args) {

      this.contexts = contexts;
      this.fauxMethod = fauxMethod;
      this.args = args;
   }

   public boolean containsContexts () {

      return (contexts != null) && (contexts.length > 0);
   }

   public Context[] getContexts () {

      return contexts;
   }

   public FauxMethod getFauxMethod () {

      return fauxMethod;
   }

   public Object[] getArgs () {

      return args;
   }
}
