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
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;

public class NaturalKey<D extends Durable<? extends Comparable>> {

  private static ConcurrentHashMap<Class<? extends Durable>, Field[]> NATURAL_KEY_MAP = new ConcurrentHashMap<Class<? extends Durable>, Field[]>();
  private static ConcurrentHashMap<Class<? extends Durable>, Field[]> NON_KEY_MAP = new ConcurrentHashMap<Class<? extends Durable>, Field[]>();

  private Class<? extends Durable> durableClass;
  private Object[] naturalKeyFieldValues;

  public static Field[] getNonKeyFields (Class<? extends Durable> durableClass) {

    Field[] nonKeyFields;

    if ((nonKeyFields = NON_KEY_MAP.get(durableClass)) == null) {

      Field[] naturalKeyFields = getNaturalKeyFields(durableClass);
      Field[] durableFields = DurableFields.getFields(durableClass);

      nonKeyFields = new Field[durableFields.length - naturalKeyFields.length];

      if (nonKeyFields.length > 0) {

        int index = 0;

        for (Field durableField : durableFields) {

          boolean matched = false;

          for (Field naturalKeyField : naturalKeyFields) {
            if (durableField.equals(naturalKeyField)) {
              matched = true;
              break;
            }
          }

          if (!matched) {
            nonKeyFields[index++] = durableField;
          }
        }
      }

      NON_KEY_MAP.put(durableClass, nonKeyFields);
    }

    return nonKeyFields;
  }

  public static Field[] getNaturalKeyFields (Class<? extends Durable> durableClass) {

    Field[] naturalKeyFields;

    if ((naturalKeyFields = NATURAL_KEY_MAP.get(durableClass)) == null) {

      NaturalKeys naturalKeys;
      int index = 0;

      if ((naturalKeys = durableClass.getAnnotation(NaturalKeys.class)) == null) {
        throw new PersistenceException("Missing required annotation(%s)", NaturalKeys.class.getSimpleName());
      }

      naturalKeyFields = new Field[naturalKeys.value().length];
      for (String fieldName : naturalKeys.value()) {
        try {

          Class<?> currentClass = durableClass;
          Field currentField = null;

          do {
            try {
              currentField = currentClass.getDeclaredField(fieldName);
              currentField.setAccessible(true);
              naturalKeyFields[index++] = currentField;
            }
            catch (NoSuchFieldException n) {
            }
          } while ((currentField != null) && ((currentClass = currentClass.getSuperclass()) != null));

          if (currentField == null) {
            throw new NoSuchFieldException(fieldName);
          }
        }
        catch (NoSuchFieldException noSuchFieldException) {
          throw new PersistenceException(noSuchFieldException);
        }

        NATURAL_KEY_MAP.put(durableClass, naturalKeyFields);
      }
    }

    return naturalKeyFields;
  }

  public NaturalKey (D durable) {

    Field[] naturalKeyFields;
    int index = 0;

    durableClass = durable.getClass();
    naturalKeyFieldValues = new Object[(naturalKeyFields = getNaturalKeyFields(durableClass)).length];

    try {
      for (Field naturalKeyField : naturalKeyFields) {
        naturalKeyFieldValues[index++] = naturalKeyField.get(durable);
      }
    }
    catch (IllegalAccessException illegalAccessException) {
      throw new RuntimeException(illegalAccessException);
    }
  }

  public NaturalKey (Class<D> durableClass, Object... naturalKeyFieldValues) {

    this.durableClass = durableClass;
    this.naturalKeyFieldValues = naturalKeyFieldValues;

    int index = 0;

    for (Field naturalKeyField : getNaturalKeyFields(durableClass)) {
      if (!TypeUtility.isEssentiallyTheSameAs(naturalKeyField.getType(), naturalKeyFieldValues[index++].getClass())) {
        throw new PersistenceException("Field values must match both order and type of the durables' natural keys(%s)", Arrays.toString(getNaturalKeyFields(durableClass)));
      }
    }
  }

  public Class<? extends Durable> getDurableClass () {

    return durableClass;
  }

  public Object[] getNaturalKeyFieldValues () {

    return naturalKeyFieldValues;
  }

  @Override
  public int hashCode () {

    int hashCode = 0;

    for (Object obj : naturalKeyFieldValues) {
      hashCode ^= obj.hashCode();
    }

    return hashCode;
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof NaturalKey) && ((NaturalKey)obj).getDurableClass().equals(durableClass) && Arrays.equals(((NaturalKey)obj).getNaturalKeyFieldValues(), naturalKeyFieldValues);
  }
}
