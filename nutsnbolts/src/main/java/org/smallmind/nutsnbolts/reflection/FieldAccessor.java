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
 * Convenient wrapper for a field and its associated getter/setter, if present.
 */
public class FieldAccessor {

  private final Method getterMethod;
  private final Method setterMethod;
  private final Field field;

  /**
   * Creates an accessor for the supplied field and companion methods.
   *
   * @param field        the backing field
   * @param getterMethod the resolved getter, or {@code null} if access should go through the field
   * @param setterMethod the resolved setter, or {@code null} if access should go through the field
   */
  public FieldAccessor (Field field, Method getterMethod, Method setterMethod) {

    this.field = field;
    this.getterMethod = getterMethod;
    this.setterMethod = setterMethod;
  }

  /**
   * @return the field name
   */
  public String getName () {

    return field.getName();
  }

  /**
   * @return the raw field type
   */
  public Class<?> getType () {

    return field.getType();
  }

  /**
   * @return the generic field type
   */
  public Type getGenericType () {

    return field.getGenericType();
  }

  /**
   * @return the backing {@link Field}
   */
  public Field getField () {

    return field;
  }

  /**
   * Reads the field value from the supplied target via getter when possible.
   *
   * @param target the instance to read from
   * @return the field value
   * @throws IllegalAccessException    if access to the field or method is denied
   * @throws InvocationTargetException if the getter throws an exception
   */
  public Object get (Object target)
    throws IllegalAccessException, InvocationTargetException {

    return (getterMethod != null) ? getterMethod.invoke(target) : field.get(target);
  }

  /**
   * Writes a value to the supplied target, preferring the setter when available.
   *
   * @param target the instance to mutate
   * @param value  the value to apply
   * @throws IllegalAccessException    if access to the field or method is denied
   * @throws InvocationTargetException if the setter throws an exception
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
