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
package org.smallmind.nutsnbolts.reflection.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericUtility {

  public static Class<?> findTypeArgument (Class<?> objectClass, TypeVariable<?> typeVariable) {

    Class<?> currentClass = objectClass;

    while (!Object.class.equals(currentClass)) {

      Type superclassType;

      if ((superclassType = currentClass.getGenericSuperclass()) instanceof ParameterizedType) {

        int argumentIndex = 0;

        for (Type argumentType : ((ParameterizedType)superclassType).getActualTypeArguments()) {
          if (argumentType instanceof TypeVariable) {
            if (typeVariable.equals(argumentType)) {

              List<Class<?>> reifiedArgumentList;

              if ((reifiedArgumentList = GenericUtility.getTypeArgumentsOfSubclass(currentClass.getSuperclass(), objectClass)).size() > argumentIndex) {

                return reifiedArgumentList.get(argumentIndex);
              }
            }

            argumentIndex++;
          }
        }
      }

      currentClass = currentClass.getSuperclass();
    }

    return null;
  }

  public static List<Class<?>> getTypeArgumentsOfSubclass (Class<?> baseClass, Class<?> childClass) {

    Map<Type, Type> resolvedTypes = new HashMap<>();
    Type type = childClass;

    while (!getClass(type).equals(baseClass)) {
      if (Object.class.equals(type)) {
        throw new TypeInferenceException("The child(%s) does not inherit from the base(%s)", childClass.getName(), baseClass.getName());
      } else if (type instanceof Class) {
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

      Class<?> typeArgumentAsClass;

      while (resolvedTypes.containsKey(baseType)) {
        baseType = resolvedTypes.get(baseType);
      }

      if ((typeArgumentAsClass = getClass(baseType)) == null) {
        throw new UnexpectedGenericDeclaration("The type(%s) has no known conversion to a concrete type", baseType.getTypeName());
      } else {
        typeArgumentsAsClasses.add(typeArgumentAsClass);
      }
    }

    return typeArgumentsAsClasses;
  }

  public static List<Class<?>> getTypeArgumentsOfImplementation (Class<?> objectClass, Class<?> targetInterface) {

    Class<?> currentClass = objectClass;

    do {
      for (Type interfaceType : currentClass.getGenericInterfaces()) {
        if (interfaceType instanceof Class) {
          if (targetInterface.isAssignableFrom((Class<?>)interfaceType)) {

            return Collections.emptyList();
          }
        } else if (interfaceType instanceof ParameterizedType) {

          Type rawType;

          if (((rawType = ((ParameterizedType)interfaceType).getRawType()) instanceof Class) && targetInterface.isAssignableFrom((Class<?>)rawType)) {

            List<Class<?>> typeArgumentsAsClasses = new ArrayList<>();
            Type[] actualTypeArguments = ((ParameterizedType)interfaceType).getActualTypeArguments();

            for (Type actualTypeArgument : actualTypeArguments) {

              Class<?> typeArgumentAsClass;

              if ((typeArgumentAsClass = GenericUtility.getClass(actualTypeArgument)) == null) {
                throw new UnexpectedGenericDeclaration("The type(%s) has no known conversion to a concrete type", actualTypeArgument.getTypeName());
              } else {
                typeArgumentsAsClasses.add(typeArgumentAsClass);
              }
            }

            return typeArgumentsAsClasses;
          }
        }
      }
    } while ((currentClass = currentClass.getSuperclass()) != null);

    throw new TypeInferenceException("The class(%s) does not implement the interface(%s)", objectClass.getName(), targetInterface.getName());
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
