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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;

public class FieldUtility {

  private static final AlphaNumericComparator<FieldAccessor> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>(FieldAccessor::getName);
  private static final ConcurrentHashMap<Class<?>, FieldAccessor[]> FIELD_ACCESSOR_MAP = new ConcurrentHashMap<>();

  public static FieldAccessor getFieldAccessor (Class<?> clazz, String name) {

    for (FieldAccessor fieldAccessor : getFieldAccessors(clazz)) {
      if (fieldAccessor.getField().getName().equals(name)) {

        return fieldAccessor;
      }
    }

    return null;
  }

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
