package org.smallmind.nutsnbolts.util;

import java.util.HashMap;

public class InstanceLocator {

   private static final HashMap<String, Object> INSTANCE_MAP = new HashMap<String, Object>();

   public static Object instantiate (String packageName, String interfaceName, String name, boolean required)
      throws DotNotationException {

      return instantiate(null, packageName, interfaceName, name, required);
   }

   public static Object instantiate (String className, boolean required)
      throws DotNotationException {

      return instantiate(null, className, required);
   }

   public static Object instantiate (ClassLoader classLoader, String packageName, String interfaceName, String name, boolean required)
      throws DotNotationException {

      String className;

      className = packageName + "." + name + interfaceName;
      return instantiate(classLoader, className, required);
   }

   public static Object instantiate (ClassLoader classLoader, String className, boolean required)
      throws DotNotationException {

      Object instance;

      synchronized (INSTANCE_MAP) {
         if (INSTANCE_MAP.containsKey(className)) {
            return INSTANCE_MAP.get(className);
         }
      }
      try {
         if (classLoader != null) {
            instance = Class.forName(className, true, classLoader).newInstance();
         }
         else {
            instance = Class.forName(className).newInstance();
         }
      }
      catch (ClassNotFoundException classNotFoundException) {
         if (required) {
            throw new DotNotationException(classNotFoundException);
         }
         else {
            instance = null;
         }
      }
      catch (Exception exception) {
         throw new DotNotationException(exception);
      }
      synchronized (INSTANCE_MAP) {
         INSTANCE_MAP.put(className, instance);
      }
      return instance;
   }

}
