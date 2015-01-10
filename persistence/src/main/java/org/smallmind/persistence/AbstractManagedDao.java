/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.reflection.type.TypeInference;
import org.smallmind.persistence.orm.ORMInitializationException;

public abstract class AbstractManagedDao<I extends Serializable & Comparable<I>, D extends Durable<I>> implements ManagedDao<I, D> {

  private final TypeInference idTypeInference = new TypeInference();
  private final TypeInference durableTypeInference = new TypeInference();
  private final AtomicReference<Method> fromStringMethodRef = new AtomicReference<Method>();
  private final String metricSource;

  public AbstractManagedDao (String metricSource) {

    this.metricSource = metricSource;

    List<Class<?>> typeArguments = GenericUtility.getTypeArguments(AbstractManagedDao.class, this.getClass());

    if (typeArguments.size() == 2) {
      if (typeArguments.get(0) != null) {
        idTypeInference.addPossibility(typeArguments.get(0));
      }
      if (typeArguments.get(1) != null) {
        durableTypeInference.addPossibility(typeArguments.get(1));
      }
    }
  }

  public String getMetricSource () {

    return metricSource;
  }

  public Class<D> getManagedClass () {

    return durableTypeInference.getInference();
  }

  public Class<I> getIdClass () {

    return idTypeInference.getInference();
  }

  public I getId (D durable) {

    return durable.getId();
  }

  public I getIdFromString (String value) {

    Class<I> idClass = getIdClass();

    if (String.class.equals(idClass)) {

      return idClass.cast(value);
    }
    if (idClass.isEnum()) {

      return idClass.cast(Enum.valueOf(idClass.asSubclass(Enum.class), value));
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
      }
      catch (Exception exception) {
        throw new ORMInitializationException(exception);
      }
    }

    throw new ORMInitializationException("Id class is neither a String, an Enum, a primitive type, nor a primitive wrapper, and does not implement Identifier, so you need to override getIdFromString(String value)");
  }

}
