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
import java.util.LinkedList;
import java.util.Map;
import org.smallmind.nutsnbolts.lang.ClassLoaderAwareCache;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;

/**
 * Static helpers for reflective JavaBean interaction, providing cached getter, setter, and method
 * lookup as well as dotted-path traversal for nested property access. Lookups are cached against the target's
 * class, under keys and values made only of JDK types, so the cache keeps neither that class nor this module's
 * class loader reachable. Setters and other methods are cached by name, and the overload whose parameter types
 * match the arguments is chosen on every call.
 */
public class BeanUtility {

  private static final ClassLoaderAwareCache<Map.Entry<Class<?>, String>, Method> GETTER_MAP = new ClassLoaderAwareCache<>(Map.Entry::getKey);
  private static final ClassLoaderAwareCache<Map.Entry<Class<?>, String>, Method[]> CANDIDATE_METHOD_MAP = new ClassLoaderAwareCache<>(Map.Entry::getKey);

  /**
   * Returns the parameter type that should be used when invoking the given setter, preferring the
   * class declared by a {@link TypeHint} annotation on the first parameter over the reflected type.
   *
   * @param setterMethod the setter method to inspect
   * @return the {@link TypeHint}-declared class if present, or the method's declared parameter type
   */
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

  /**
   * Converts a lower-camel-case field name to a {@code getXxx} getter name.
   *
   * @param name the field or property name, e.g. {@code firstName}
   * @return the corresponding getter name, e.g. {@code getFirstName}
   */
  public static String asGetterName (String name) {

    StringBuilder getterBuilder = new StringBuilder(name);

    getterBuilder.setCharAt(0, Character.toUpperCase(getterBuilder.charAt(0)));
    getterBuilder.insert(0, "get");

    return getterBuilder.toString();
  }

  /**
   * Converts a lower-camel-case boolean field name to an {@code isXxx} accessor name.
   *
   * @param name the boolean field or property name, e.g. {@code active}
   * @return the corresponding accessor name, e.g. {@code isActive}
   */
  public static String asIsName (String name) {

    StringBuilder isBuilder = new StringBuilder(name);

    isBuilder.setCharAt(0, Character.toUpperCase(isBuilder.charAt(0)));
    isBuilder.insert(0, "is");

    return isBuilder.toString();
  }

  /**
   * Converts a lower-camel-case field name to a {@code setXxx} setter name.
   *
   * @param name the field or property name, e.g. {@code firstName}
   * @return the corresponding setter name, e.g. {@code setFirstName}
   */
  public static String asSetterName (String name) {

    StringBuilder setterBuilder = new StringBuilder(name);

    setterBuilder.setCharAt(0, Character.toUpperCase(setterBuilder.charAt(0)));
    setterBuilder.insert(0, "set");

    return setterBuilder.toString();
  }

  /**
   * Reads a property value from {@code target} by following a dotted getter path.
   * Each segment except the last must resolve to a non-null intermediate object;
   * the final segment provides the returned value.
   *
   * @param target    the root object from which navigation starts
   * @param fieldPath a dotted path of property names, e.g. {@code "address.city"}
   * @param nullable  when {@code true}, a {@code null} intermediate value causes early return of {@code null};
   *                  when {@code false}, a {@code null} intermediate causes a {@link BeanAccessException}
   * @return the value at the end of the property path, or {@code null} if an intermediate was null and nullable
   * @throws BeanAccessException     if a getter is absent or returns {@code null} when {@code nullable} is false
   * @throws BeanInvocationException if any getter along the path throws an exception
   */
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

  /**
   * Writes a value by following a dotted setter path rooted at {@code target}.
   * All segments except the last are resolved as getter calls to reach the owning object;
   * the final segment is resolved as a setter call.
   *
   * @param target    the root object from which navigation starts
   * @param fieldPath a dotted path of property names ending in the property to set, e.g. {@code "address.city"}
   * @param value     the value to assign to the final property
   * @return the return value of the setter invocation, typically {@code null}
   * @throws BeanAccessException     if the setter method cannot be found
   * @throws BeanInvocationException if any accessor along the path throws an exception
   */
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

  /**
   * Navigates a dotted path on {@code target} to reach the owning object, then invokes the final
   * segment as a method call with the supplied argument values.
   *
   * @param target     the root object from which navigation starts
   * @param methodPath a dotted path where all but the last segment are getters and the last is the method name
   * @param values     the arguments to pass to the target method
   * @return the value returned by the invoked method
   * @throws BeanAccessException     if the target method cannot be found on the owning object
   * @throws BeanInvocationException if any accessor or the final method throws an exception
   */
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

  /**
   * Walks all path components except the last by invoking getter methods, returning the object
   * that owns the final component.
   *
   * @param target         the root object from which traversal begins
   * @param methodName     the full dotted path string, used for error messages
   * @param pathComponents the individual segments of the dotted path
   * @return the intermediate object that declares the final path segment
   * @throws BeanAccessException     if any intermediate getter is missing or returns {@code null}
   * @throws BeanInvocationException if any intermediate getter throws an exception
   */
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

  /**
   * Returns a cached getter method for the named property on the target's class, looking up and caching
   * it if not yet known.
   *
   * @param target the object whose class should be searched for the getter
   * @param name   the property name to look up via {@code getXxx} or {@code isXxx}
   * @return the resolved getter {@link Method}
   * @throws BeanAccessException if neither a {@code getXxx} nor an {@code isXxx} method can be found
   */
  private static Method acquireGetterMethod (Object target, String name)
    throws BeanAccessException {

    Method getterMethod;
    Map.Entry<Class<?>, String> methodKey;

    methodKey = Map.entry(target.getClass(), name);
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

  /**
   * Returns the setter method for the named property on the target's class whose parameter type matches the
   * value's class.
   *
   * @param target the object whose class should be searched for the setter
   * @param name   the property name to look up via {@code setXxx}
   * @param value  the value that will be passed to the setter, used for type matching
   * @return the resolved setter {@link Method}
   * @throws BeanAccessException if a compatible {@code setXxx} method cannot be found
   */
  private static Method acquireSetterMethod (Object target, String name, Object value)
    throws BeanAccessException {

    Method setterMethod;

    if ((setterMethod = findMethod(target, asSetterName(name), value.getClass())) == null) {
      throw new BeanAccessException("No 'setter' method(%s) found in class(%s)", asSetterName(name), target.getClass().getName());
    }

    return setterMethod;
  }

  /**
   * Returns the method with the given name on the target's class whose parameter types match the classes of
   * the argument values.
   *
   * @param target the object whose class should be searched for the method
   * @param name   the method name to look up
   * @param values the argument values to be passed, used for parameter type matching; may be empty
   * @return the resolved {@link Method}
   * @throws BeanAccessException if no method matching the name and argument types can be found
   */
  private static Method acquireMethod (Object target, String name, Object... values)
    throws BeanAccessException {

    Method method;
    Class[] parameterTypes = new Class[(values == null) ? 0 : values.length];

    if ((values != null) && (values.length > 0)) {
      for (int parameterIndex = 0; parameterIndex < values.length; parameterIndex++) {
        parameterTypes[parameterIndex] = values[parameterIndex].getClass();
      }
    }
    if ((method = findMethod(target, name, parameterTypes)) == null) {
      throw new BeanAccessException("No method(%s) for parameter types(%s) found in class(%s)", name, Arrays.toString(parameterTypes), target.getClass().getName());
    }

    return method;
  }

  /**
   * Searches the target's class for a public non-static method with the given name and
   * compatible parameter types, among the candidates cached for that name.
   *
   * @param target         the object whose class is searched
   * @param name           the exact method name
   * @param parameterTypes the expected parameter types in declaration order
   * @return the matching {@link Method}, made accessible as described by {@link #accessibleMethod(Object, Method)},
   * or {@code null} if none is found
   */
  private static Method findMethod (Object target, String name, Class... parameterTypes) {

    for (Method method : acquireCandidateMethods(target, name)) {
      if (hasParameterTypes(method, parameterTypes)) {

        return method;
      }
    }

    return null;
  }

  /**
   * Returns every public non-static method with the given name on the target's class, each made accessible as
   * described by {@link #accessibleMethod(Object, Method)}, looking them up and caching them if not yet known.
   *
   * @param target the object whose class is searched
   * @param name   the exact method name
   * @return the candidate methods, possibly empty
   */
  private static Method[] acquireCandidateMethods (Object target, String name) {

    Method[] candidateMethods;
    Map.Entry<Class<?>, String> methodKey = Map.entry(target.getClass(), name);

    if ((candidateMethods = CANDIDATE_METHOD_MAP.get(methodKey)) == null) {

      LinkedList<Method> candidateMethodList = new LinkedList<>();

      for (Method method : target.getClass().getMethods()) {
        if (method.getName().equals(name) && (!Modifier.isStatic(method.getModifiers()))) {
          candidateMethodList.add(accessibleMethod(target, method));
        }
      }

      CANDIDATE_METHOD_MAP.put(methodKey, candidateMethods = candidateMethodList.toArray(new Method[0]));
    }

    return candidateMethods;
  }

  /**
   * Retrieves the public method with the given name and parameter types from the target's class,
   * throwing {@link NoSuchMethodException} if the method is static.
   *
   * @param target         the object whose class is inspected
   * @param name           the exact method name
   * @param parameterTypes the expected parameter types in declaration order
   * @return the matching non-static {@link Method}, made accessible as described by {@link #accessibleMethod(Object, Method)}
   * @throws NoSuchMethodException if the method does not exist or is static
   */
  private static Method getMethod (Object target, String name, Class... parameterTypes)
    throws NoSuchMethodException {

    Method method;

    if (!Modifier.isStatic((method = target.getClass().getMethod(name, parameterTypes)).getModifiers())) {

      return accessibleMethod(target, method);
    }

    throw new NoSuchMethodException();
  }

  /**
   * Returns {@code method} when this class can invoke it on {@code target}. Otherwise, because the method is
   * declared by a class that is not public or whose package is not exported to this module (as with many
   * collection and library implementation classes), returns the same method as declared by an accessible
   * superclass or interface of the target's class. If there is no such declaration, {@code method} is returned
   * unchanged and invoking it fails with {@link IllegalAccessException}.
   *
   * @param target the object the method will be invoked on
   * @param method the public method found on the target's class
   * @return an accessible declaration of the method, or {@code method} itself
   */
  private static Method accessibleMethod (Object target, Method method) {

    Method accessibleMethod;

    if (method.canAccess(target)) {

      return method;
    } else if ((accessibleMethod = findAccessibleMethod(target, target.getClass(), method.getName(), method.getParameterTypes())) != null) {

      return accessibleMethod;
    }

    return method;
  }

  /**
   * Walks {@code type}, its interfaces, and its superclasses looking for a declaration of the named public method
   * that this class can invoke on {@code target}.
   *
   * @param target         the object the method will be invoked on
   * @param type           the class or interface to search, or {@code null} at the top of the hierarchy
   * @param name           the exact method name
   * @param parameterTypes the exact parameter types
   * @return an accessible declaration of the method, or {@code null} if none is found
   */
  private static Method findAccessibleMethod (Object target, Class<?> type, String name, Class<?>[] parameterTypes) {

    Method method;

    if (type == null) {

      return null;
    }

    try {
      if ((method = type.getMethod(name, parameterTypes)).canAccess(target)) {

        return method;
      }
    } catch (NoSuchMethodException noSuchMethodException) {

      return null;
    }

    for (Class<?> interfaceType : type.getInterfaces()) {
      if ((method = findAccessibleMethod(target, interfaceType, name, parameterTypes)) != null) {

        return method;
      }
    }

    return findAccessibleMethod(target, type.getSuperclass(), name, parameterTypes);
  }

  /**
   * Returns {@code true} when the method's parameter count and types are compatible with
   * the supplied types, accounting for primitive/wrapper equivalence.
   *
   * @param method         the method whose signature should be evaluated
   * @param parameterTypes the desired parameter types to match against; may be empty or {@code null}
   * @return {@code true} if the method accepts parameters that are essentially the same as those provided
   */
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
}
