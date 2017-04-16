/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.reflection.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.reflection.type.converter.DefaultStringConverterFactory;
import org.smallmind.nutsnbolts.reflection.type.converter.StringConversionException;
import org.smallmind.nutsnbolts.reflection.type.converter.StringConverter;
import org.smallmind.nutsnbolts.reflection.type.converter.StringConverterFactory;

public class BeanUtility {

  private static final ConcurrentHashMap<MethodKey, Method> GETTER_MAP = new ConcurrentHashMap<MethodKey, Method>();
  private static final ConcurrentHashMap<MethodKey, MethodTool> SETTER_MAP = new ConcurrentHashMap<MethodKey, MethodTool>();

  public static Object convertFromString (StringConverterFactory stringConverterFactory, Class conversionClass, String value, boolean nullable)
    throws StringConversionException, BeanInvocationException {

    if (value == null) {
      if (!nullable) {
        throw new NullPointerException("Null value in a non-nullable conversion");
      }

      return null;
    }

    return stringConverterFactory.getStringConverter(conversionClass).convert(value);
  }

  public static Object executeGet (Object target, String methodName, boolean nullable)
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
          if (!nullable) {
            throw new BeanAccessException("The 'getter' method(%s) in chain(%s) returned a 'null' component", getterMethod.getName(), methodName);
          }

          return null;
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

  public static Class<?> executeSet (Object target, String methodName, String value)
    throws BeanAccessException, BeanInvocationException {

    return executeSet(DefaultStringConverterFactory.getInstance(), target, methodName, value);
  }

  public static Class<?> executeSet (StringConverterFactory stringConverterFactory, Object target, String methodName, String value)
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

      return setterTool.getConverter().getType();
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
    throws StringConversionException, BeanAccessException {

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
