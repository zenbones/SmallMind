/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.smallmind.nutsnbolts.lang.ClassLoaderAwareCache;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;

public class BeanUtility {

  private static final ClassLoaderAwareCache<MethodKey, Method> GETTER_MAP = new ClassLoaderAwareCache<>(methodKey -> methodKey.getMethodClass().getClassLoader());
  private static final ClassLoaderAwareCache<MethodKey, Method> SETTER_MAP = new ClassLoaderAwareCache<>(methodKey -> methodKey.getMethodClass().getClassLoader());
  private static final ClassLoaderAwareCache<MethodKey, Method> METHOD_MAP = new ClassLoaderAwareCache<>(methodKey -> methodKey.getMethodClass().getClassLoader());

  public static Class<?> getParameterClass (Method setterMethod) {

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

  public static String asGetterName (String name) {

    StringBuilder getterBuilder = new StringBuilder(name);

    getterBuilder.setCharAt(0, Character.toUpperCase(getterBuilder.charAt(0)));
    getterBuilder.insert(0, "get");

    return getterBuilder.toString();
  }

  public static String asIsName (String name) {

    StringBuilder isBuilder = new StringBuilder(name);

    isBuilder.setCharAt(0, Character.toUpperCase(isBuilder.charAt(0)));
    isBuilder.insert(0, "is");

    return isBuilder.toString();
  }

  public static String asSetterName (String name) {

    StringBuilder setterBuilder = new StringBuilder(name);

    setterBuilder.setCharAt(0, Character.toUpperCase(setterBuilder.charAt(0)));
    setterBuilder.insert(0, "set");

    return setterBuilder.toString();
  }

  public static Object executeGet (Object target, String fieldPath, boolean nullable)
    throws BeanAccessException, BeanInvocationException {

    Method getterMethod;
    Object currentTarget;
    String[] pathComponents;

    // Split the method into dot-notated segments
    pathComponents = fieldPath.split("\\.", -1);
    currentTarget = target;

    try {
      // Every segment but the last is taken as a getter method
      for (int count = 0; count < pathComponents.length - 1; count++) {
        if ((currentTarget = (getterMethod = acquireGetterMethod(currentTarget, pathComponents[count])).invoke(currentTarget)) == null) {
          if (!nullable) {
            throw new BeanAccessException("The 'getter' method(%s) in chain(%s) returned a 'null' component", getterMethod.getName(), fieldPath);
          }

          return null;
        }
      }

      // As this executes a 'get' the last segment is taken as a getter
      return acquireGetterMethod(currentTarget, pathComponents[pathComponents.length - 1]).invoke(currentTarget);
    } catch (BeanAccessException beanAccessException) {
      throw beanAccessException;
    } catch (Exception exception) {
      throw new BeanInvocationException(exception);
    }
  }

  public static Object executeSet (Object target, String fieldPath, Object value)
    throws BeanAccessException, BeanInvocationException {

    Object currentTarget;
    String[] pathComponents;

    // Split the method into dot-notated segments
    pathComponents = fieldPath.split("\\.", -1);
    currentTarget = traverseComponents(target, fieldPath, pathComponents);

    try {
      // As this executes a 'set' the last segment is taken as a setter
      return acquireSetterMethod(currentTarget, pathComponents[pathComponents.length - 1], value).invoke(currentTarget, value);
    } catch (Exception exception) {
      throw new BeanInvocationException(exception);
    }
  }

  public static Object execute (Object target, String methodPath, Object... values)
    throws BeanAccessException, BeanInvocationException {

    Object currentTarget;
    String[] pathComponents;

    // Split the method into dot-notated segments
    pathComponents = methodPath.split("\\.", -1);
    currentTarget = traverseComponents(target, methodPath, pathComponents);

    try {
      return acquireMethod(currentTarget, pathComponents[pathComponents.length - 1], values).invoke(currentTarget, values);
    } catch (Exception exception) {
      throw new BeanInvocationException(exception);
    }
  }

  private static Object traverseComponents (Object target, String methodName, String... pathComponents)
    throws BeanAccessException, BeanInvocationException {

    Object currentTarget = target;

    if ((pathComponents != null) && (pathComponents.length > 0)) {
      try {
        for (int count = 0; count < pathComponents.length - 1; count++) {

          Method getterMethod;

          if ((getterMethod = acquireGetterMethod(currentTarget, pathComponents[count])) == null) {
            throw new BeanAccessException("Missing 'getter' for method(%s) in chain(%s)", pathComponents[count], methodName);
          } else if ((currentTarget = getterMethod.invoke(currentTarget)) == null) {
            throw new BeanAccessException("The 'getter' method(%s) in chain(%s) returned a 'null' component", getterMethod.getName(), methodName);
          }
        }
      } catch (BeanAccessException beanAccessException) {
        throw beanAccessException;
      } catch (Exception exception) {
        throw new BeanInvocationException(exception);
      }
    }

    return currentTarget;
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
        getterMethod = getMethod(target, asGetterName(name));
      } catch (NoSuchMethodException noGetterException) {
        try {
          // If not, is there a boolean version 'isXXX'
          getterMethod = getMethod(target, asIsName(name));
          if (!(Boolean.class.equals(getterMethod.getReturnType()) || boolean.class.equals(getterMethod.getReturnType()))) {
            throw new BeanAccessException("Found an 'is' method(%s) in class(%s), but it doesn't return a 'boolean' type", getterMethod.getName(), target.getClass().getName());
          }
        } catch (NoSuchMethodException noIsException) {
          throw new BeanAccessException("No 'getter' method(%s or %s) found in class(%s)", asGetterName(name), asIsName(name), target.getClass().getName());
        }
      }

      GETTER_MAP.put(methodKey, getterMethod);
    }

    return getterMethod;
  }

  private static Method acquireSetterMethod (Object target, String name, Object value)
    throws BeanAccessException {

    Method setterMethod;
    MethodKey methodKey;

    methodKey = new MethodKey(target.getClass(), name);
    // Check if we've already got it
    if ((setterMethod = SETTER_MAP.get(methodKey)) == null) {
      if ((setterMethod = findMethod(target, asSetterName(name), value.getClass())) == null) {
        throw new BeanAccessException("No 'setter' method(%s) found in class(%s)", asSetterName(name), target.getClass().getName());
      }
      SETTER_MAP.put(methodKey, setterMethod);
    }

    return setterMethod;
  }

  private static Method acquireMethod (Object target, String name, Object... values)
    throws BeanAccessException {

    Method method;
    MethodKey methodKey;

    methodKey = new MethodKey(target.getClass(), name);
    // Check if we've already got it
    if ((method = METHOD_MAP.get(methodKey)) == null) {

      Class[] parameterTypes = new Class[(values == null) ? 0 : values.length];

      if ((values != null) && (values.length > 0)) {
        for (int parameterIndex = 0; parameterIndex < values.length; parameterIndex++) {
          parameterTypes[parameterIndex] = values[parameterIndex].getClass();
        }
      }
      if ((method = findMethod(target, name, parameterTypes)) == null) {
        throw new BeanAccessException("No method(%s) for parameter types(%s) found in class(%s)", name, Arrays.toString(parameterTypes), target.getClass().getName());
      }
      METHOD_MAP.put(methodKey, method);
    }

    return method;
  }

  private static Method findMethod (Object target, String name, Class... parameterTypes) {

    for (Method method : target.getClass().getMethods()) {
      if (method.getName().equals(name) && (!Modifier.isStatic(method.getModifiers())) && hasParameterTypes(method, parameterTypes)) {

        return method;
      }
    }

    return null;
  }

  private static Method getMethod (Object target, String name, Class... parameterTypes)
    throws NoSuchMethodException {

    Method method;

    if (!Modifier.isStatic((method = target.getClass().getMethod(name, parameterTypes)).getModifiers())) {

      return method;
    }

    throw new NoSuchMethodException();
  }

  private static boolean hasParameterTypes (Method method, Class... parameterTypes) {

    if (method.getParameterCount() != ((parameterTypes == null) ? 0 : parameterTypes.length)) {

      return false;
    }
    if (method.getParameterCount() > 0) {

      int index = 0;

      for (Class<?> parameterType : method.getParameterTypes()) {
        if (!TypeUtility.isEssentiallyTheSameAs(parameterType, parameterTypes[index++])) {

          return false;
        }
      }
    }

    return true;
  }

  private static class MethodKey {

    private final Class<?> methodClass;
    private final String methodName;

    private MethodKey (Class<?> methodClass, String methodName) {

      this.methodClass = methodClass;
      this.methodName = methodName;
    }

    private Class<?> getMethodClass () {

      return methodClass;
    }

    private String getMethodName () {

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
