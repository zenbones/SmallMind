package org.smallmind.cloud.namespace.java.backingStore.ldap;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.smallmind.cloud.namespace.java.backingStore.ContextCreator;
import org.smallmind.cloud.namespace.java.backingStore.NamingConnectionDetails;

public class LdapContextCreator extends ContextCreator {

   public static void insureContext (DirContext dirContext, String namingPath)
      throws NamingException {

      StringBuilder pathSoFar;
      String[] pathArray;

      pathArray = namingPath.split(",", -1);
      pathSoFar = new StringBuilder();
      for (int count = pathArray.length - 1; count >= 0; count--) {
         if (pathSoFar.length() > 0) {
            pathSoFar.insert(0, ',');
         }
         pathSoFar.insert(0, pathArray[count]);
         try {
            dirContext.lookup(pathSoFar.toString());
         }
         catch (NameNotFoundException n) {
            dirContext.createSubcontext(pathSoFar.toString());
         }
      }
   }

   public LdapContextCreator (NamingConnectionDetails connectionDetails) {

      super(connectionDetails);
   }

   public String getRoot () {

      return getConnectionDetails().getRootNamespace();
   }

   public DirContext getInitialContext ()
      throws NamingException {

      InitialDirContext initLdapContext;
      DirContext rootLdapContext;
      Hashtable<String, String> env;
      String rootUrl;

      env = new Hashtable<String, String>();
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, getConnectionDetails().getUserName());
      env.put(Context.SECURITY_CREDENTIALS, getConnectionDetails().getPassword());

      initLdapContext = new InitialDirContext(env);
      rootUrl = "ldap://" + getConnectionDetails().getHost() + ":" + getConnectionDetails().getPort() + "/" + getConnectionDetails().getRootNamespace();

      try {
         rootLdapContext = (DirContext)initLdapContext.lookup(rootUrl);
      }
      catch (NamingException originalNamingException) {

         NamingException informationalNamingException;

         informationalNamingException = new NamingException(rootUrl);
         informationalNamingException.initCause(originalNamingException);

         throw informationalNamingException;
      }

      return rootLdapContext;
   }
}
