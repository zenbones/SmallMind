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
package org.smallmind.persistence;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;

/**
 * Represents the natural (business) key of a {@link Durable} using the fields declared
 * in its {@link NaturalKeys} annotation. Instances cache resolved fields for efficient
 * key extraction and comparison.
 *
 * @param <D> the durable type that owns the natural key
 */
public class NaturalKey<D extends Durable<? extends Comparable>> {

  private static final ConcurrentHashMap<Class<? extends Durable>, Field[]> NATURAL_KEY_MAP = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Class<? extends Durable>, Field[]> NON_KEY_MAP = new ConcurrentHashMap<>();

  private final Class<? extends Durable> durableClass;
  private final Object[] naturalKeyFieldValues;

  /**
   * Builds a natural key by extracting the annotated fields from the supplied durable.
   *
   * @param durable the durable instance from which to read key values
   * @throws RuntimeException if the fields cannot be accessed reflectively
   */
  public NaturalKey (D durable) {

    Field[] naturalKeyFields;
    int index = 0;

    durableClass = durable.getClass();
    naturalKeyFieldValues = new Object[(naturalKeyFields = getNaturalKeyFields(durableClass)).length];

    try {
      for (Field naturalKeyField : naturalKeyFields) {
        naturalKeyFieldValues[index++] = naturalKeyField.get(durable);
      }
    } catch (IllegalAccessException illegalAccessException) {
      throw new RuntimeException(illegalAccessException);
    }
  }

  /**
   * Builds a natural key from explicit values, validating the order and type against the
   * {@link NaturalKeys} definition on the durable class.
   *
   * @param durableClass          the durable class that defines the natural key fields
   * @param naturalKeyFieldValues the values of the natural key fields, in declaration order
   * @throws PersistenceException if the supplied values do not match the expected types
   */
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

  /**
   * Returns the fields of a durable that are not part of the natural key.
   *
   * @param durableClass the durable class to inspect
   * @return an array of non-key fields, possibly empty
   */
  public static Field[] getNonKeyFields (Class<? extends Durable> durableClass) {

    Field[] nonKeyFields;

    if ((nonKeyFields = NON_KEY_MAP.get(durableClass)) == null) {

      Field[] naturalKeyFields = getNaturalKeyFields(durableClass);
      FieldAccessor[] durableFieldAccessors = FieldUtility.getFieldAccessors(durableClass);

      nonKeyFields = new Field[durableFieldAccessors.length - naturalKeyFields.length];

      if (nonKeyFields.length > 0) {

        int index = 0;

        for (FieldAccessor durableFieldAccessor : durableFieldAccessors) {

          boolean matched = false;

          for (Field naturalKeyField : naturalKeyFields) {
            if (durableFieldAccessor.getField().equals(naturalKeyField)) {
              matched = true;
              break;
            }
          }

          if (!matched) {
            nonKeyFields[index++] = durableFieldAccessor.getField();
          }
        }
      }

      NON_KEY_MAP.put(durableClass, nonKeyFields);
    }

    return nonKeyFields;
  }

  /**
   * Resolves the fields that make up the natural key for the supplied durable class.
   *
   * @param durableClass the durable class to inspect
   * @return an array of key fields in the order declared by {@link NaturalKeys}
   * @throws PersistenceException if the annotation is missing or references unknown fields
   */
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
            } catch (NoSuchFieldException n) {
            }
          } while ((currentField != null) && ((currentClass = currentClass.getSuperclass()) != null));

          if (currentField == null) {
            throw new NoSuchFieldException(fieldName);
          }
        } catch (NoSuchFieldException noSuchFieldException) {
          throw new PersistenceException(noSuchFieldException);
        }

        NATURAL_KEY_MAP.put(durableClass, naturalKeyFields);
      }
    }

    return naturalKeyFields;
  }

  /**
   * Returns the durable class that owns this natural key.
   *
   * @return the durable class
   */
  public Class<? extends Durable> getDurableClass () {

    return durableClass;
  }

  /**
   * Returns the natural key field values captured for this key.
   *
   * @return the field values in declaration order
   */
  public Object[] getNaturalKeyFieldValues () {

    return naturalKeyFieldValues;
  }

  /**
   * Computes a hash code by XOR-ing the hash codes of the individual key field values.
   *
   * @return the hash code for this natural key
   */
  @Override
  public int hashCode () {

    int hashCode = 0;

    for (Object obj : naturalKeyFieldValues) {
      hashCode ^= obj.hashCode();
    }

    return hashCode;
  }

  /**
   * Compares this natural key to another for equality based on durable class and field values.
   *
   * @param obj the object to compare
   * @return {@code true} when both represent the same durable natural key
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof NaturalKey) && ((NaturalKey)obj).getDurableClass().equals(durableClass) && Arrays.equals(((NaturalKey)obj).getNaturalKeyFieldValues(), naturalKeyFieldValues);
  }
}
