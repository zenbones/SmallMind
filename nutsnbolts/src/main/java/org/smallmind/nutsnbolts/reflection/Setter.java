package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Setter {

   private Class attributeClass;
   private Method method;
   private String attributeName;

   public Setter (Method method)
      throws ReflectionContractException {

      this.method = method;

      if (method.getReturnType() != Void.class) {
         throw new ReflectionContractException("Setter for attribute (%s) must return void", attributeName);
      }

      if (!(method.getName().startsWith("set") && (method.getName().length() > 3) && Character.isUpperCase(method.getName().charAt(3)))) {
         throw new ReflectionContractException("The declared name of a setter method must start with 'set' followed by a camel case attribute name");
      }
      attributeName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);

      if (method.getParameterTypes().length != 1) {
         throw new ReflectionContractException("Setter for attribute (%s) must declare a single parameter", attributeName);
      }
      attributeClass = method.getParameterTypes()[0];
   }

   public String getAttributeName () {

      return attributeName;
   }

   public Class getAttributeClass () {

      return attributeClass;
   }

   public Object invoke (Object target, Object value)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

      Object[] parameter = {value};

      return method.invoke(target, parameter);
   }

}
