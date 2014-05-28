/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class DurableFields {

  private static final ConcurrentHashMap<Class<? extends Durable>, Field[]> FIELD_MAP = new ConcurrentHashMap<>();

  public static Field getField (Class<? extends Durable> durableClass, String name) {

    for (Field durableField : getFields(durableClass)) {
      if (durableField.getName().equals(name)) {

        return durableField;
      }
    }

    return null;
  }

  public static Field[] getFields (final Class<? extends Durable> durableClass) {

    Field[] fields;

    if ((fields = FIELD_MAP.get(durableClass)) == null) {
      synchronized (durableClass) {
        if ((fields = FIELD_MAP.get(durableClass)) == null) {
          Class<?> currentClass = durableClass;
          LinkedList<Field> fieldList = new LinkedList<>();

          do {
            for (Field field : currentClass.getDeclaredFields()) {
              if (!(field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))) {
                field.setAccessible(true);
                fieldList.add(field);
              }
            }

          } while ((currentClass = currentClass.getSuperclass()) != null);

          Collections.sort(fieldList, new Comparator<Field>() {

            public int compare (Field field1, Field field2) {

              return field1.getName().compareToIgnoreCase(field2.getName());
            }
          });

          fields = new Field[fieldList.size()];
          fieldList.toArray(fields);

          FIELD_MAP.put(durableClass, fields);
        }
      }
    }

    return fields;
  }
}
