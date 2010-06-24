package org.smallmind.cloud.transport;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

public class FauxMethod implements Serializable {

   private Class[] signature;
   private Class returnType;
   private String name;

   public FauxMethod (Method method) {

      name = method.getName();
      returnType = method.getReturnType();
      signature = method.getParameterTypes();
   }

   public String getName () {
      return name;
   }

   public Class getReturnType () {
      return returnType;
   }

   public Class[] getSignature () {
      return signature;
   }

   public int hashCode () {

      int hashCode;

      hashCode = name.hashCode();
      hashCode = hashCode ^ returnType.getName().hashCode();

      for (Class parameterType : signature) {
         hashCode = hashCode ^ parameterType.getName().hashCode();
      }

      return hashCode;
   }

   public boolean equals (Object obj) {

      return (obj instanceof FauxMethod) && name.equals(((FauxMethod)obj).getName()) && returnType.equals(((FauxMethod)obj).getReturnType()) && Arrays.equals(signature, ((FauxMethod)obj).getSignature());
   }
}
