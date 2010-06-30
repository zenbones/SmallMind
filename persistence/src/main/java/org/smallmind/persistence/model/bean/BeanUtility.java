package org.smallmind.persistence.model.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.persistence.model.type.PrimitiveType;
import org.smallmind.persistence.model.type.converter.DefaultStringConverterFactory;
import org.smallmind.persistence.model.type.converter.StringConverter;
import org.smallmind.persistence.model.type.converter.StringConverterFactory;

public class BeanUtility {

   private static final ConcurrentHashMap<MethodKey, Method> GETTER_MAP = new ConcurrentHashMap<MethodKey, Method>();
   private static final ConcurrentHashMap<MethodKey, MethodTool> SETTER_MAP = new ConcurrentHashMap<MethodKey, MethodTool>();

   public static Object convertFromString (StringConverterFactory stringConverterFactory, Class conversionClass, String value)
      throws BeanAccessException, BeanInvocationException {

      return stringConverterFactory.getStringConverter(conversionClass).convert(value);
   }

   public static Object executeGet (Object target, String methodName)
      throws BeanAccessException, BeanInvocationException {

      Method getterMethod;
      Object currentTarget;
      String[] methodComponents;

      // Split the method into dot-notated segments
      methodComponents = methodName.split("\\.", -1);
      currentTarget = target;

      try {
         // Every segment but the last is taken as a getter method
         for (int count = 0; count < methodComponents.length - 1; count++) {
            if ((currentTarget = (getterMethod = acquireGetterMethod(currentTarget, methodComponents[count])).invoke(currentTarget)) == null) {
               throw new BeanAccessException("The 'getter' method(%s) in chain(%s) returned a 'null' component", getterMethod.getName(), methodName);
            }
         }

         // As this executes a 'get' the last segment is taken as a getter
         return acquireGetterMethod(currentTarget, methodComponents[methodComponents.length - 1]).invoke(currentTarget);
      }
      catch (BeanAccessException beanAccessException) {
         throw beanAccessException;
      }
      catch (Exception exception) {
         throw new BeanInvocationException(exception);
      }
   }

   public static PrimitiveType executeSet (Object target, String methodName, String value)
      throws BeanAccessException, BeanInvocationException {

      return executeSet(DefaultStringConverterFactory.getInstance(), target, methodName, value);
   }

   public static PrimitiveType executeSet (StringConverterFactory stringConverterFactory, Object target, String methodName, String value)
      throws BeanAccessException, BeanInvocationException {

      MethodTool setterTool;
      Method getterMethod;
      Object currentTarget;
      String[] methodComponents;

      // Split the method into dot-notated segments
      methodComponents = methodName.split("\\.", -1);
      currentTarget = target;

      try {
         // Every segment but the last is taken as a getter method
         for (int count = 0; count < methodComponents.length - 1; count++) {
            if ((currentTarget = (getterMethod = acquireGetterMethod(currentTarget, methodComponents[count])).invoke(currentTarget)) == null) {
               throw new BeanAccessException("The 'getter' method(%s) in chain(%s) returned a 'null' component", getterMethod.getName(), methodName);
            }
         }

         // As this executes a 'set' the last segment is taken as a setter, and setters are stored with a String converter that returns the setter's proper parameter type
         setterTool = acquireSetterTool(stringConverterFactory, currentTarget, methodComponents[methodComponents.length - 1]);
         setterTool.getMethod().invoke(currentTarget, ((value == null) || (value.length() == 0)) ? null : setterTool.getConverter().convert(value));

         return setterTool.getConverter().getPrimitiveType();
      }
      catch (BeanAccessException beanAccessException) {
         throw beanAccessException;
      }
      catch (Exception exception) {
         throw new BeanInvocationException(exception);
      }
   }

   private static Method acquireGetterMethod (Object target, String name)
      throws BeanAccessException {

      Method getterMethod;
      MethodKey methodKey;

      methodKey = new MethodKey(target.getClass(), name);
      // Check if we've already got it
      if ((getterMethod = GETTER_MAP.get(methodKey)) == null) {
         try {
            // Is there a method with a proper getter name 'getXXX'
            getterMethod = target.getClass().getMethod(asGetterName(name));
         }
         catch (NoSuchMethodException noGetterException) {
            try {
               // If not, is there a boolean version 'isXXX'
               getterMethod = target.getClass().getMethod(asIsName(name));
               if (!(Boolean.class.equals(getterMethod.getReturnType()) || boolean.class.equals(getterMethod.getReturnType()))) {
                  throw new BeanAccessException("Found an 'is' method(%s) on class(%s), but it doesn't return a 'boolean' type", getterMethod.getName(), target.getClass().getName());
               }
            }
            catch (NoSuchMethodException noIsException) {
               throw new BeanAccessException("No 'getter' method(%s or %s) found on class(%s)", asGetterName(name), asIsName(name), target.getClass().getName());
            }
         }

         GETTER_MAP.put(methodKey, getterMethod);
      }

      return getterMethod;
   }

   private static MethodTool acquireSetterTool (StringConverterFactory stringConverterFactory, Object target, String name)
      throws BeanAccessException {

      MethodTool setterTool;
      MethodKey methodKey;
      String setterName = asSetterName(name);

      methodKey = new MethodKey(target.getClass(), name);
      // Check if we've already got it
      if ((setterTool = SETTER_MAP.get(methodKey)) == null) {
         // Look for a properly named method
         for (Method possibleMethod : target.getClass().getMethods()) {
            // Make sure the setter takes a single parameter, and get the String converter for it
            if (possibleMethod.getName().equals(setterName) && (possibleMethod.getParameterTypes().length == 1)) {
               SETTER_MAP.put(methodKey, setterTool = new MethodTool(possibleMethod, stringConverterFactory.getStringConverter(getParameterClass(possibleMethod))));
               break;
            }
         }
      }

      if (setterTool == null) {
         throw new BeanAccessException("No 'setter' method(%s) found on class(%s)", setterName, target.getClass().getName());
      }

      return setterTool;
   }

   private static Class getParameterClass (Method setterMethod) {

      Annotation[][] parameterAnnotations;

      if ((parameterAnnotations = setterMethod.getParameterAnnotations()).length > 0) {
         for (Annotation annotation : parameterAnnotations[0]) {
            if (annotation instanceof TypeHint) {

               return ((TypeHint)annotation).value();
            }
         }
      }

      return setterMethod.getParameterTypes()[0];
   }

   private static String asGetterName (String name) {

      StringBuilder getterBuilder = new StringBuilder(name);

      getterBuilder.setCharAt(0, Character.toUpperCase(getterBuilder.charAt(0)));
      getterBuilder.insert(0, "get");

      return getterBuilder.toString();
   }

   private static String asIsName (String name) {

      StringBuilder isBuilder = new StringBuilder(name);

      isBuilder.setCharAt(0, Character.toUpperCase(isBuilder.charAt(0)));
      isBuilder.insert(0, "is");

      return isBuilder.toString();
   }

   private static String asSetterName (String name) {

      StringBuilder setterBuilder = new StringBuilder(name);

      setterBuilder.setCharAt(0, Character.toUpperCase(setterBuilder.charAt(0)));
      setterBuilder.insert(0, "set");

      return setterBuilder.toString();
   }

   private static class MethodTool {

      private Method method;
      private StringConverter converter;

      private MethodTool (Method method, StringConverter converter) {

         this.method = method;
         this.converter = converter;
      }

      public Method getMethod () {

         return method;
      }

      public StringConverter getConverter () {

         return converter;
      }
   }

   private static class MethodKey {

      private Class methodClass;
      private String methodName;

      private MethodKey (Class methodClass, String methodName) {

         this.methodClass = methodClass;
         this.methodName = methodName;
      }

      public Class getMethodClass () {

         return methodClass;
      }

      public String getMethodName () {

         return methodName;
      }

      @Override
      public int hashCode () {

         return methodClass.hashCode() ^ methodName.hashCode();
      }

      @Override
      public boolean equals (Object obj) {

         return (obj instanceof MethodKey) && methodClass.equals(((MethodKey)obj).getMethodClass()) && methodName.equals(((MethodKey)obj).getMethodName());
      }
   }
}
