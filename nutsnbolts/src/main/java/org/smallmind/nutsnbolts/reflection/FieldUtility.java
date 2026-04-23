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
 * Static helpers that discover and cache {@link FieldAccessor} descriptors for the non-static,
 * non-transient, non-synthetic fields of a class and its superclasses.
 */
public class FieldUtility {

  private static final AlphaNumericComparator<FieldAccessor> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>(FieldAccessor::getName);
  private static final ConcurrentHashMap<Class<?>, FieldAccessor[]> FIELD_ACCESSOR_MAP = new ConcurrentHashMap<>();

  /**
   * Returns the cached {@link FieldAccessor} for the field with the given name on the supplied class.
   *
   * @param clazz the class to inspect
   * @param name  the exact field name to look up
   * @return the matching accessor, or {@code null} if no such field exists
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
   * Returns all non-static, non-transient, non-synthetic field accessors for the given class and every
   * superclass up to but not including {@link Object}, sorted alpha-numerically by field name.
   * Results are cached for subsequent calls.
   *
   * @param clazz the class whose fields should be discovered
   * @return a sorted, cached array of {@link FieldAccessor} objects for the discovered fields
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
   * Attempts to find either a {@code getXxx} or {@code isXxx} public non-static method for the field.
   *
   * @param clazz the class to search for getter methods
   * @param field the field for which a getter is sought
   * @return the getter {@link Method}, or {@code null} if neither convention yields a match
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
   * Attempts to find a {@code setXxx} public non-static method for the field that accepts the field's type.
   *
   * @param clazz the class to search for setter methods
   * @param field the field for which a setter is sought
   * @return the setter {@link Method}, or {@code null} if none is found
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
