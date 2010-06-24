package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;

public class ProxyUtility {

   private static final HashMap<String, Method> METHOD_MAP = new HashMap<String, Method>();
   private static final HashMap<String, Class> SIGNATURE_MAP = new HashMap<String, Class>();

   public static Object invoke (Object proxy, InvocationHandler invocationHandler, boolean isSubclass, String methodCode, String methodName, String resultSignature, String[] signatures, Object... args)
      throws Throwable {

      Method proxyMethod;

      if ((proxyMethod = METHOD_MAP.get(methodCode)) == null) {
         synchronized (METHOD_MAP) {
            if ((proxyMethod = METHOD_MAP.get(methodCode)) == null) {

               Class methodContainer = (isSubclass) ? proxy.getClass().getSuperclass() : proxy.getClass();

               METHOD_MAP.put(methodCode, proxyMethod = methodContainer.getMethod(methodName, assembleSignature(signatures)));
            }
         }
      }

      if (invocationHandler == null) {
         switch (resultSignature.charAt(0)) {
            case 'V':
               return null;
            case 'Z':
               return false;
            case 'B':
               return 0;
            case 'C':
               return (char)0;
            case 'S':
               return 0;
            case 'I':
               return 0;
            case 'J':
               return 0L;
            case 'F':
               return 0.0F;
            case 'D':
               return 0.0D;
            case 'L':
               return null;
            case '[':
               return null;
            default:
               throw new ByteCodeManipulationException("Unknown format for result signature(%s)", resultSignature);
         }
      }

      return invocationHandler.invoke(proxy, proxyMethod, args);
   }

   private static Class[] assembleSignature (String[] signatures) {

      Class[] parsedSignature;
      LinkedList<Class> parsedList;

      parsedList = new LinkedList<Class>();
      for (String signature : signatures) {
         switch (signature.charAt(0)) {
            case 'Z':
               parsedList.add(boolean.class);
            case 'B':
               parsedList.add(byte.class);
            case 'C':
               parsedList.add(char.class);
            case 'S':
               parsedList.add(short.class);
            case 'I':
               parsedList.add(int.class);
            case 'J':
               parsedList.add(long.class);
            case 'F':
               parsedList.add(float.class);
            case 'D':
               parsedList.add(double.class);
            case 'L':
               parsedList.add(getObjectType(signature.substring(1, signature.length() - 1).replace('/', '.')));
               break;
            case '[':
               parsedList.add(getObjectType(signature.replace('/', '.')));
               break;
            default:
               throw new ByteCodeManipulationException("Unknown format for parameter signature(%s)", signature);
         }
      }

      parsedSignature = new Class[parsedList.size()];
      parsedList.toArray(parsedSignature);

      return parsedSignature;
   }

   private static Class getObjectType (String type) {

      Class objectType;

      if ((objectType = SIGNATURE_MAP.get(type)) == null) {
         synchronized (SIGNATURE_MAP) {
            if ((objectType = SIGNATURE_MAP.get(type)) == null) {
               try {
                  SIGNATURE_MAP.put(type, objectType = Class.forName(type));
               }
               catch (ClassNotFoundException classNotFoundException) {
                  throw new ByteCodeManipulationException(classNotFoundException);
               }
            }
         }
      }

      return objectType;
   }
}
