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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Bundles a {@link Field} with its optional getter and setter methods so that values can be read and
 * written through bean-style accessors when they exist, falling back to direct field access otherwise.
 */
public class FieldAccessor {

  private final Method getterMethod;
  private final Method setterMethod;
  private final Field field;

  /**
   * Constructs an accessor for the given field with optional companion getter and setter methods.
   *
   * @param field        the reflected field this accessor wraps
   * @param getterMethod the getter method for this field, or {@code null} to use direct field access for reads
   * @param setterMethod the setter method for this field, or {@code null} to use direct field access for writes
   */
  public FieldAccessor (Field field, Method getterMethod, Method setterMethod) {

    this.field = field;
    this.getterMethod = getterMethod;
    this.setterMethod = setterMethod;
  }

  /**
   * Returns the name of the underlying field.
   *
   * @return the field name as declared in source code
   */
  public String getName () {

    return field.getName();
  }

  /**
   * Returns the erased type of the underlying field.
   *
   * @return the raw {@link Class} of the field
   */
  public Class<?> getType () {

    return field.getType();
  }

  /**
   * Returns the generic type of the underlying field, preserving type parameter information.
   *
   * @return the generic {@link Type} of the field
   */
  public Type getGenericType () {

    return field.getGenericType();
  }

  /**
   * Returns the reflected {@link Field} this accessor wraps.
   *
   * @return the underlying {@link Field} object
   */
  public Field getField () {

    return field;
  }

  /**
   * Reads this field's value from the given target, using the getter method when one is available
   * and falling back to direct field access otherwise.
   *
   * @param target the object instance from which the value should be read
   * @return the current field value
   * @throws IllegalAccessException    if the getter or field is not accessible
   * @throws InvocationTargetException if the getter method throws an exception
   */
  public Object get (Object target)
    throws IllegalAccessException, InvocationTargetException {

    return (getterMethod != null) ? getterMethod.invoke(target) : field.get(target);
  }

  /**
   * Writes a value to this field on the given target, using the setter method when one is available
   * and falling back to direct field assignment otherwise.
   *
   * @param target the object instance whose field value should be updated
   * @param value  the new value to assign
   * @throws IllegalAccessException    if the setter or field is not accessible
   * @throws InvocationTargetException if the setter method throws an exception
   */
  public void set (Object target, Object value)
    throws IllegalAccessException, InvocationTargetException {

    if (setterMethod != null) {
      setterMethod.invoke(target, value);
    } else {
      field.set(target, value);
    }
  }
}
