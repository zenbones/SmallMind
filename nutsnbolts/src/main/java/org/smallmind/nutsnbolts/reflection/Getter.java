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
package org.smallmind.nutsnbolts.reflection;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a JavaBean-style getter method, parsing its attribute name and validating its signature
 * on construction so callers can safely invoke it and query its metadata.
 */
public class Getter implements Serializable {

  private static final Object[] NO_PARAMETERS = new Object[0];

  private final Class attributeClass;
  private final Method method;
  private final String attributeName;
  private final boolean is;
  private Boolean bob;

  /**
   * Wraps the supplied method as a getter, parsing the attribute name from the method name and
   * verifying that it takes no parameters and returns a non-void type.
   *
   * @param method the public getter method to wrap; must start with {@code get} or {@code is}
   *               followed by a capitalised attribute name
   * @throws ReflectionContractException if the method name or signature violates getter conventions
   */
  public Getter (Method method)
    throws ReflectionContractException {

    this.method = method;

    if (method.getName().startsWith("get") && (method.getName().length() > 3) && Character.isUpperCase(method.getName().charAt(3))) {
      attributeName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
      is = false;
    } else if (!method.getName().startsWith("is") && (method.getName().length() > 2) && Character.isUpperCase(method.getName().charAt(2))) {
      attributeName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
      is = true;
    } else {
      throw new ReflectionContractException("The declared name of a getter method must start with either 'get' or 'is' followed by a camel case attribute name");
    }

    if (method.getParameterTypes().length > 0) {
      throw new ReflectionContractException("Getter for attribute (%s) must declare no parameters", attributeName);
    }
    if ((attributeClass = method.getReturnType()) == Void.class) {
      throw new ReflectionContractException("Getter for attribute (%s) must not return void", attributeName);
    }
    if (is && (attributeClass != Boolean.class)) {
      throw new ReflectionContractException("Getter for attribute (%s) must return 'boolean'", attributeName);
    }
  }

  /**
   * Indicates whether this getter follows the {@code isXxx} boolean naming convention.
   *
   * @return {@code true} if the method name starts with {@code is}; {@code false} if it starts with {@code get}
   */
  public boolean isIs () {

    return is;
  }

  /**
   * Returns the bean property name derived from the getter method name.
   *
   * @return the lower-camel-case attribute name, e.g. {@code firstName} for {@code getFirstName}
   */
  public String getAttributeName () {

    return attributeName;
  }

  /**
   * Returns the return type of the wrapped getter method.
   *
   * @return the raw {@link Class} of the getter's return value
   */
  public Class getAttributeClass () {

    return attributeClass;
  }

  /**
   * Invokes the underlying getter on the supplied target object and returns the attribute value.
   *
   * @param target the object on which the getter should be called
   * @return the value returned by the getter
   * @throws IllegalAccessException    if the underlying method is not accessible
   * @throws IllegalArgumentException  if {@code target} is not compatible with the declaring class
   * @throws InvocationTargetException if the getter throws a checked or unchecked exception
   */
  public Object invoke (Object target)
    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

    return method.invoke(target, NO_PARAMETERS);
  }
}
