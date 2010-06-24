package org.smallmind.cloud.namespace.java;

import java.util.Hashtable;
import javax.naming.InvalidNameException;
import javax.naming.directory.DirContext;
import org.smallmind.cloud.namespace.java.backingStore.NameTranslator;

public class NamingEnumerationUtilities {

   protected static String convertName (String name, NameTranslator nameTranslator)
      throws InvalidNameException {

      return nameTranslator.fromExternalStringToInternalString(name);
   }

   protected static String convertClassName (String className, Class internalDirContextClass) {

      if (className != null) {
         if (className.equals(internalDirContextClass.getName())) {

            return JavaContext.class.getName();
         }
      }

      return className;
   }

   protected static Object convertObject (Object boundObject, Class internalDirContextClass, Hashtable<String, Object> environment, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

      if (boundObject != null) {
         if (boundObject.getClass().equals(internalDirContextClass)) {

            return new JavaContext(environment, (DirContext)boundObject, nameTranslator, nameParser, modifiable);
         }
      }
      return boundObject;
   }

}