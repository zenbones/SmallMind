package org.smallmind.cloud.namespace.java;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import org.smallmind.cloud.namespace.java.backingStore.ContextCreator;
import org.smallmind.cloud.namespace.java.backingStore.NameTranslator;
import org.smallmind.cloud.namespace.java.backingStore.NamingConnectionDetails;
import org.smallmind.nutsnbolts.util.StringUtilities;

public class javaURLContextFactory implements ObjectFactory {

   private static final Class[] CONTEXT_CREATOR_SIGNATURE = new Class[] {NamingConnectionDetails.class};
   private static final Class[] NAME_TRANSLATOR_SIGNATURE = new Class[] {ContextCreator.class};

   public Object getObjectInstance (Object obj, Name name, Context nameCtx, Hashtable environment)
      throws Exception {

      ContextCreator contextCreator;
      NameTranslator nameTranslator;
      Class contextCreatorClass;
      Class nameTranslatorClass;
      Constructor contextCreatorConstructor;
      Constructor nameTranslatorConstructor;
      String backingStore;
      boolean modifiable = false;
      boolean pooled = false;

      if (obj == null) {
         if (environment.containsKey(JavaContext.CONTEXT_MODIFIABLE)) {
            if ((environment.get(JavaContext.CONTEXT_MODIFIABLE)).equals("true")) {
               modifiable = true;
            }
         }
         if (environment.containsKey(JavaContext.POOLED_CONNECTION)) {
            if ((environment.get(JavaContext.POOLED_CONNECTION)).equals("true")) {
               pooled = true;
            }
         }

         backingStore = (String)environment.get(JavaContext.CONTEXT_STORE);

         contextCreatorClass = Class.forName(ContextCreator.class.getPackage().getName() + '.' + backingStore + '.' + StringUtilities.toDisplayCase(backingStore) + ContextCreator.class.getSimpleName());
         contextCreatorConstructor = contextCreatorClass.getConstructor(CONTEXT_CREATOR_SIGNATURE);
         contextCreator = (ContextCreator)contextCreatorConstructor.newInstance((NamingConnectionDetails)environment.get(JavaContext.CONNECTION_DETAILS));

         nameTranslatorClass = Class.forName(NameTranslator.class.getPackage().getName() + '.' + backingStore + '.' + StringUtilities.toDisplayCase(backingStore) + NameTranslator.class.getSimpleName());
         nameTranslatorConstructor = nameTranslatorClass.getConstructor(NAME_TRANSLATOR_SIGNATURE);
         nameTranslator = (NameTranslator)nameTranslatorConstructor.newInstance(contextCreator);

         return new JavaContext(nameTranslator, (Hashtable<String, Object>)environment, modifiable, pooled);
      }

      return null;
   }
}
