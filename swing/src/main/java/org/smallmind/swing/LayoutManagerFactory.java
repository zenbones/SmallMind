package org.smallmind.swing;

import java.awt.LayoutManager;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;

public class LayoutManagerFactory {

   private static final Class[] NO_SIGNATURE = new Class[0];
   private static final Object[] NO_PARAMETERS = new Object[0];
   private static final HashMap<InstanceKey, LayoutManager> LAYOUT_MANAGER_MAP = new HashMap<InstanceKey, LayoutManager>();

   public static LayoutManager getLayoutManager (Class<? extends LayoutManager> layoutManagerClass)
      throws LayoutManagerConstructionException {

      return getLayoutManager(layoutManagerClass, NO_SIGNATURE, NO_PARAMETERS);
   }

   public static LayoutManager getLayoutManager (Class<? extends LayoutManager> layoutManagerClass, Class[] signature, Object[] parameters)
      throws LayoutManagerConstructionException {

      LayoutManager layoutManager;
      Constructor<? extends LayoutManager> layoutManagerConstructor;
      InstanceKey instanceKey;

      instanceKey = new LayoutManagerFactory.InstanceKey(layoutManagerClass, signature, parameters);
      if ((layoutManager = LAYOUT_MANAGER_MAP.get(instanceKey)) == null) {
         try {
            layoutManagerConstructor = layoutManagerClass.getConstructor(signature);
            layoutManager = layoutManagerConstructor.newInstance(parameters);
         }
         catch (Exception e) {
            throw new LayoutManagerConstructionException(e);
         }

         LAYOUT_MANAGER_MAP.put(instanceKey, layoutManager);
      }

      return layoutManager;
   }

   private static class InstanceKey {

      private Class layoutManagerClass;
      private Class[] signature;
      private Object[] parameters;

      public InstanceKey (Class layoutManagerClass, Class[] signature, Object[] parameters) {

         this.layoutManagerClass = layoutManagerClass;
         this.signature = signature;
         this.parameters = parameters;
      }

      public Class getLayoutManagerClass () {

         return layoutManagerClass;
      }

      public Class[] getSignature () {

         return signature;
      }

      public Object[] getParameters () {

         return parameters;
      }

      public int hashCode () {

         return layoutManagerClass.hashCode();
      }

      public boolean equals (Object obj) {

         if (obj instanceof InstanceKey) {
            if (layoutManagerClass.equals(((InstanceKey)obj).getLayoutManagerClass())) {
               if (Arrays.equals(signature, ((InstanceKey)obj).getSignature())) {
                  if (Arrays.equals(parameters, ((InstanceKey)obj).getParameters())) {
                     return true;
                  }
               }
            }
         }

         return false;
      }

   }

}
