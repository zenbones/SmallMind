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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.reflection.type.TypeInference;
import org.smallmind.persistence.orm.ORMInitializationException;

/**
 * Base {@link ManagedDao} implementation that captures generic id and durable types at
 * construction time and supplies helpers for id conversion and metric identification.
 *
 * @param <I> the identifier type handled by the DAO
 * @param <D> the managed durable entity type
 */
public abstract class AbstractManagedDao<I extends Serializable & Comparable<I>, D extends Durable<I>> implements ManagedDao<I, D> {

  private final TypeInference idTypeInference = new TypeInference();
  private final TypeInference durableTypeInference = new TypeInference();
  private final AtomicReference<Method> fromStringMethodRef = new AtomicReference<>();
  private final String metricSource;

  /**
   * Creates a managed DAO base instance and records type information from the subclass.
   *
   * @param metricSource a name used to attribute metrics emitted by this DAO
   */
  public AbstractManagedDao (String metricSource) {

    this.metricSource = metricSource;

    List<Class<?>> typeArguments = GenericUtility.getTypeArgumentsOfSubclass(AbstractManagedDao.class, this.getClass());

    if (typeArguments.size() == 2) {
      if (typeArguments.get(0) != null) {
        idTypeInference.addPossibility(typeArguments.get(0));
      }
      if (typeArguments.get(1) != null) {
        durableTypeInference.addPossibility(typeArguments.get(1));
      }
    }
  }

  /**
   * Provides the metric source name specified for this DAO.
   *
   * @return the metric source identifier
   */
  public String getMetricSource () {

    return metricSource;
  }

  /**
   * Retrieves the durable class captured from the subclass' generic parameters.
   *
   * @return the managed durable type
   */
  public Class<D> getManagedClass () {

    return durableTypeInference.getInference();
  }

  /**
   * Retrieves the identifier class captured from the subclass' generic parameters.
   *
   * @return the managed identifier type
   */
  public Class<I> getIdClass () {

    return idTypeInference.getInference();
  }

  /**
   * Extracts the identifier from a durable instance.
   *
   * @param durable the durable whose id should be returned
   * @return the durable id, which may be {@code null} for transient instances
   */
  public I getId (D durable) {

    return durable.getId();
  }

  /**
   * Converts a string representation to the appropriate identifier type. Primitive types,
   * their boxed equivalents, enums and {@link Identifier} implementations are supported
   * out of the box. Override when custom parsing is required.
   *
   * @param value the string containing the identifier
   * @return the parsed identifier value
   * @throws ORMInitializationException if the identifier cannot be parsed or no conversion strategy exists
   */
  public I getIdFromString (String value) {

    Class<I> idClass = getIdClass();

    if (String.class.equals(idClass)) {

      return idClass.cast(value);
    }
    if (idClass.isEnum()) {

      return (I)(Enum.valueOf((Class<? extends Enum>)idClass, value));
    }
    if (long.class.equals(idClass) || (Long.class.equals(idClass))) {

      return idClass.cast(Long.parseLong(value));
    }
    if (boolean.class.equals(idClass) || (Boolean.class.equals(idClass))) {

      return idClass.cast(Boolean.parseBoolean(value));
    }
    if (int.class.equals(idClass) || (Integer.class.equals(idClass))) {

      return idClass.cast(Integer.parseInt(value));
    }
    if (double.class.equals(idClass) || (Double.class.equals(idClass))) {

      return idClass.cast(Double.parseDouble(value));
    }
    if (float.class.equals(idClass) || (Float.class.equals(idClass))) {

      return idClass.cast(Float.parseFloat(value));
    }
    if (char.class.equals(idClass) || (Character.class.equals(idClass))) {

      return idClass.cast(value.charAt(0));
    }
    if (short.class.equals(idClass) || (Short.class.equals(idClass))) {

      return idClass.cast(Short.parseShort(value));
    }
    if (byte.class.equals(idClass) || (Byte.class.equals(idClass))) {

      return idClass.cast(Byte.parseByte(value));
    }

    if (Identifier.class.isAssignableFrom(idClass)) {
      try {

        Method fromStringMethod;

        if ((fromStringMethod = fromStringMethodRef.get()) == null) {
          fromStringMethod = idClass.getMethod("fromString", String.class);
          if (!Modifier.isStatic(fromStringMethod.getModifiers())) {
            throw new ORMInitializationException("The fromString() method in the identifier class(%s) needs to be declared static", idClass.getName());
          }

          fromStringMethodRef.compareAndSet(null, fromStringMethod);
        }

        return idClass.cast(fromStringMethod.invoke(null, value));
      } catch (Exception exception) {
        throw new ORMInitializationException(exception);
      }
    }

    throw new ORMInitializationException("Id class is neither a String, an Enum, a primitive type, nor a primitive wrapper, and does not implement Identifier, so you need to override getIdFromString(String value)");
  }
}
