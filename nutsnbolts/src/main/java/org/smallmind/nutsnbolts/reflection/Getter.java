/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class Getter implements Serializable {

  private static final Object[] NO_PARAMETERS = new Object[0];

  private final Class attributeClass;
  private final Method method;
  private final String attributeName;
  private final boolean is;
  private Boolean bob;

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
