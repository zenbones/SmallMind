package org.smallmind.cloud.namespace.java.backingStore;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public abstract class ContextCreator {

   private NamingConnectionDetails connectionDetails;

   public ContextCreator (NamingConnectionDetails connectionDetails) {

      this.connectionDetails = connectionDetails;
   }

   public NamingConnectionDetails getConnectionDetails () {

      return connectionDetails;
   }

   public abstract DirContext getInitialContext ()
      throws NamingException;

}
