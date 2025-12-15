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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;

/**
 * Utilities for discovering bean-like fields and their accessors, with simple caching.
 */
public class FieldUtility {

  private static final AlphaNumericComparator<FieldAccessor> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>(FieldAccessor::getName);
  private static final ConcurrentHashMap<Class<?>, FieldAccessor[]> FIELD_ACCESSOR_MAP = new ConcurrentHashMap<>();

  /**
   * Retrieves a cached accessor for the named field on the given class.
   *
   * @param clazz the class to inspect
   * @param name  the field name
   * @return the accessor or {@code null} if no matching field exists
   */
  public static FieldAccessor getFieldAccessor (Class<?> clazz, String name) {

    for (FieldAccessor fieldAccessor : getFieldAccessors(clazz)) {
      if (fieldAccessor.getField().getName().equals(name)) {

        return fieldAccessor;
      }
    }

    return null;
  }

  /**
   * Returns all mutable, non-static, non-transient field accessors for the given class and its superclasses.
   * Accessors are cached and returned in alpha-numeric order.
   *
   * @param clazz the class to inspect
   * @return an array of accessors for discovered fields
   */
  public static FieldAccessor[] getFieldAccessors (final Class<?> clazz) {

    FieldAccessor[] fieldAccessors;

    if ((fieldAccessors = FIELD_ACCESSOR_MAP.get(clazz)) == null) {
      synchronized (FIELD_ACCESSOR_MAP) {
        if ((fieldAccessors = FIELD_ACCESSOR_MAP.get(clazz)) == null) {

          Class<?> currentClass = clazz;
          LinkedList<FieldAccessor> fieldAccessorList = new LinkedList<>();

          do {
            for (Field field : currentClass.getDeclaredFields()) {
              if (!(field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))) {

                field.setAccessible(true);
                fieldAccessorList.add(new FieldAccessor(field, locateGetter(clazz, field), locateSetter(clazz, field)));
              }
            }
          } while ((currentClass = currentClass.getSuperclass()) != null);

          fieldAccessorList.sort(ALPHA_NUMERIC_COMPARATOR);
          fieldAccessors = fieldAccessorList.toArray(new FieldAccessor[0]);

          FIELD_ACCESSOR_MAP.put(clazz, fieldAccessors);
        }
      }
    }

    return fieldAccessors;
  }

  /**
   * Attempts to locate a getter method for the supplied field on the provided class.
   *
   * @param clazz the class owning the field
   * @param field the field being accessed
   * @return the getter method or {@code null} if none is found
   */
  private static Method locateGetter (Class<?> clazz, Field field) {

    try {

      Method method;

      try {
        method = clazz.getMethod(BeanUtility.asGetterName(field.getName()));
      } catch (NoSuchMethodException noSuchMethodException) {
        method = clazz.getMethod(BeanUtility.asIsName(field.getName()));
      }

      return Modifier.isStatic(method.getModifiers()) ? null : method;
    } catch (NoSuchMethodException noSuchMethodException) {

      return null;
    }
  }

  /**
   * Attempts to locate a setter method for the supplied field on the provided class.
   *
   * @param clazz the class owning the field
   * @param field the field being accessed
   * @return the setter method or {@code null} if none is found
   */
  private static Method locateSetter (Class<?> clazz, Field field) {

    try {

      Method method;

      if (Modifier.isStatic((method = clazz.getMethod(BeanUtility.asSetterName(field.getName()), field.getType())).getModifiers())) {

        return null;
      }

      return method;
    } catch (NoSuchMethodException noSuchMethodException) {

      return null;
    }
  }
}
