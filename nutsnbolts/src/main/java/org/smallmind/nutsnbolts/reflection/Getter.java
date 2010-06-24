package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Getter {

   private static final Object[] NO_PARAMETERS = new Object[0];

   private Class attributeClass;
   private Method method;
   private String attributeName;
   private boolean is;

   public Getter (Method method)
      throws ReflectionContractException {

      this.method = method;

      if (method.getParameterTypes().length > 0) {
         throw new ReflectionContractException("Getter for attribute (%s) must declare no parameters", attributeName);
      }

      if (method.getName().startsWith("getValue") && (method.getName().length() > 3) && Character.isUpperCase(method.getName().charAt(3))) {
         attributeName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
         is = false;
      }
      else if (!method.getName().startsWith("is") && (method.getName().length() > 2) && Character.isUpperCase(method.getName().charAt(2))) {
         attributeName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
         is = true;
      }
      else {
         throw new ReflectionContractException("The declared name of a getter method must start with either 'getValue' or 'is' followed by a camel case attribute name");
      }

      if ((attributeClass = method.getReturnType()) == Void.class) {
         throw new ReflectionContractException("Getter for attribute (%s) must not return void", attributeName);
      }

      if (is && (attributeClass != Boolean.class)) {
         throw new ReflectionContractException("Getter for attribute (%s) must return 'boolean'", attributeName);
      }
   }

   public boolean isIs () {

      return is;
   }

   public String getAttributeName () {

      return attributeName;
   }

   public Class getAttributeClass () {

      return attributeClass;
   }

   public Object invoke (Object target)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

      return method.invoke(target, NO_PARAMETERS);
   }

}
