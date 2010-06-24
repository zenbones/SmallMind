package org.smallmind.cloud.namespace.java.backingStore;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.smallmind.cloud.namespace.java.ContextNamePair;
import org.smallmind.cloud.namespace.java.JavaName;

public abstract class NameTranslator {

   private ContextCreator contextCreator;

   public NameTranslator (ContextCreator contextCreator) {

      this.contextCreator = contextCreator;
   }

   public ContextCreator getContextCreator () {

      return contextCreator;
   }

   public ContextNamePair fromInternalNameToExternalContext (DirContext internalContext, Name internalName)
      throws NamingException {

      if (internalContext == null) {
         if ((internalName.size() == 0) || (!internalName.get(0).equals("java:"))) {
            throw new NamingException("No starting context from which to resolve (" + internalName + ")");
         }

         try {
            return new ContextNamePair(contextCreator.getInitialContext(), fromInternalNameToExternalName(internalName.getSuffix(1)));
         }
         catch (NamingException namingException) {
            throw namingException;
         }
         catch (Exception e) {

            NamingException namingException;

            namingException = new NamingException(e.getMessage());
            namingException.setRootCause(e);

            throw namingException;
         }
      }
      else {
         return new ContextNamePair(internalContext, fromInternalNameToExternalName(internalName));
      }
   }

   public abstract JavaName fromInternalNameToExternalName (Name internalName)
      throws InvalidNameException;

   public abstract String fromExternalNameToExternalString (JavaName internalName);

   public abstract String fromAbsoluteExternalStringToInternalString (String externalName)
      throws InvalidNameException;

   public abstract String fromExternalStringToInternalString (String externalName)
      throws InvalidNameException;

}
