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
 * Value type representing the business (natural) key of a {@link Durable}, derived from the
 * fields declared in the {@link NaturalKeys} annotation. Resolved field references are cached
 * per durable class to minimize repeated reflection overhead.
 *
 * @param <D> the durable type whose natural key this instance represents
 */
public class NaturalKey<D extends Durable<? extends Comparable>> {

  private static final ConcurrentHashMap<Class<? extends Durable>, Field[]> NATURAL_KEY_MAP = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Class<? extends Durable>, Field[]> NON_KEY_MAP = new ConcurrentHashMap<>();

  private final Class<? extends Durable> durableClass;
  private final Object[] naturalKeyFieldValues;

  /**
   * Constructs a natural key by reading the {@link NaturalKeys}-annotated field values from
   * the given durable instance.
   *
   * @param durable the durable from which to extract key field values
   * @throws RuntimeException if a key field cannot be accessed reflectively
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
   * Constructs a natural key from explicit values, verifying that each value's type matches
   * the corresponding {@link NaturalKeys}-annotated field on {@code durableClass}.
   *
   * @param durableClass          the durable class that defines the natural key
   * @param naturalKeyFieldValues the key field values in the same order as declared in {@link NaturalKeys}
   * @throws PersistenceException if any value's type does not match the expected field type
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
   * Returns all fields of the given durable class that are not part of its natural key.
   * Results are cached per class after the first call.
   *
   * @param durableClass the durable class to inspect
   * @return an array of non-key fields, which may be empty
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
   * Resolves and returns the fields that form the natural key for the given durable class,
   * reading them from the {@link NaturalKeys} annotation. Results are cached per class.
   *
   * @param durableClass the durable class whose natural key fields are needed
   * @return the key fields in the order declared in {@link NaturalKeys#value()}
   * @throws PersistenceException if the {@link NaturalKeys} annotation is absent or names an unknown field
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
   * Returns the durable class associated with this natural key.
   *
   * @return the durable class
   */
  public Class<? extends Durable> getDurableClass () {

    return durableClass;
  }

  /**
   * Returns the key field values held by this natural key instance.
   *
   * @return the field values in the order they appear in {@link NaturalKeys#value()}
   */
  public Object[] getNaturalKeyFieldValues () {

    return naturalKeyFieldValues;
  }

  /**
   * Returns a hash code derived by XOR-ing the hash codes of all key field values.
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
   * Returns {@code true} when {@code obj} is a {@code NaturalKey} for the same durable class
   * and its field values are equal to this instance's field values.
   *
   * @param obj the object to compare
   * @return {@code true} when the two natural keys are equivalent
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof NaturalKey) && ((NaturalKey)obj).getDurableClass().equals(durableClass) && Arrays.equals(((NaturalKey)obj).getNaturalKeyFieldValues(), naturalKeyFieldValues);
  }
}
