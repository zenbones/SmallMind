package org.smallmind.nutsnbolts.naming;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public class ContextUtilities {

   public static void ensureContext (DirContext dirContext, String namingPath)
      throws NamingException {

      StringBuilder pathSoFar;
      String[] pathArray;
      int count;

      pathArray = namingPath.split("/", -1);
      pathSoFar = new StringBuilder();
      for (count = 0; count < pathArray.length; count++) {
         if (pathSoFar.length() > 0) {
            pathSoFar.append('/');
         }
         pathSoFar.append(pathArray[count]);
         try {
            dirContext.lookup(pathSoFar.toString());
         }
         catch (NameNotFoundException n) {
            dirContext.createSubcontext(pathSoFar.toString());
         }
      }
   }

}