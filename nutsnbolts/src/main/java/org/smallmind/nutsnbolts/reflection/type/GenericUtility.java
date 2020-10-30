/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.nutsnbolts.reflection.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericUtility {

  public static List<Class<?>> getTypeArguments (Class<?> baseClass, Class<?> childClass) {

    Map<Type, Type> resolvedTypes = new HashMap<>();
    Type type = childClass;

    while (!getClass(type).equals(baseClass)) {
      if (type instanceof Class) {
        type = ((Class<?>)type).getGenericSuperclass();
      } else {

        ParameterizedType parameterizedType = (ParameterizedType)type;
        Class<?> rawType = (Class<?>)parameterizedType.getRawType();
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();

        for (int i = 0; i < actualTypeArguments.length; i++) {
          resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
        }

        if (!rawType.equals(baseClass)) {
          type = rawType.getGenericSuperclass();
        }
      }
    }

    Type[] actualTypeArguments;

    if (type instanceof Class) {
      actualTypeArguments = ((Class<?>)type).getTypeParameters();
    } else {
      actualTypeArguments = ((ParameterizedType)type).getActualTypeArguments();
    }

    List<Class<?>> typeArgumentsAsClasses = new ArrayList<>();

    for (Type baseType : actualTypeArguments) {
      while (resolvedTypes.containsKey(baseType)) {
        baseType = resolvedTypes.get(baseType);
      }
      typeArgumentsAsClasses.add(getClass(baseType));
    }

    return typeArgumentsAsClasses;
  }

  public static Class<?> getClass (Type type) {

    if (type instanceof Class) {

      return (Class<?>)type;
    } else if (type instanceof ParameterizedType) {

      return getClass(((ParameterizedType)type).getRawType());
    } else if (type instanceof GenericArrayType) {

      Type componentType = ((GenericArrayType)type).getGenericComponentType();
      Class<?> componentClass = getClass(componentType);

      if (componentClass != null) {
        return Array.newInstance(componentClass, 0).getClass();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
