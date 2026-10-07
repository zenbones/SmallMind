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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;

/**
 * Bundles a {@link Field} with the method handles that read and write it, through its bean-style getter and
 * setter when they exist and directly otherwise. The handles carry the access of the
 * {@link java.lang.invoke.MethodHandles.Lookup} that created them, whether {@link FieldUtility} created them or
 * the caller of {@link #FieldAccessor(Field, MethodHandle, MethodHandle)} did, so an accessor grants no access
 * beyond what that lookup already had.
 */
public class FieldAccessor {

  private static final MethodType GETTER_TYPE = MethodType.methodType(Object.class, Object.class);
  private static final MethodType SETTER_TYPE = MethodType.methodType(void.class, Object.class, Object.class);

  private final Field field;
  private final MethodHandle getterHandle;
  private final MethodHandle setterHandle;
  private final boolean getterMethod;
  private final boolean setterMethod;

  /**
   * Constructs an accessor for the given field from handles the caller created with its own lookup. Each handle
   * must be a direct method handle, as returned by the {@code unreflect}, {@code unreflectGetter},
   * {@code unreflectSetter}, {@code findVirtual}, {@code findGetter}, or {@code findSetter} methods of a
   * {@link java.lang.invoke.MethodHandles.Lookup}, and not one adapted by {@code asType}, {@code bindTo}, or a
   * {@link MethodHandles} combinator. Whether each handle invokes a method or accesses the field directly is read
   * from the handle itself, and decides whether failures surface wrapped in {@link InvocationTargetException}.
   * <p>
   * A getter handle must read {@code field} directly, or invoke an instance method that takes no arguments and
   * returns a value. A setter handle must write {@code field} directly, or invoke an instance method that takes
   * one argument whose type accepts the field's type, allowing for boxing; its result, if any, is discarded. The
   * receiver of a method must be an interface, or a class related by inheritance to the class declaring the field.
   * A getter method's return type is not compared with the field's type, because a getter may present the field
   * as another type.
   *
   * @param field        the non-static field this accessor wraps
   * @param getterHandle the handle that reads the field, through its getter method or directly
   * @param setterHandle the handle that writes the field, through its setter method or directly, or {@code null}
   *                     for an accessor that cannot write the field
   * @throws IllegalArgumentException if {@code field} is static, {@code getterHandle} is {@code null}, or either
   *                                  handle is not a direct method handle or does not match {@code field} as
   *                                  described above
   */
  public FieldAccessor (Field field, MethodHandle getterHandle, MethodHandle setterHandle) {

    Member getterMember;

    if (Modifier.isStatic(field.getModifiers())) {
      throw new IllegalArgumentException("The field(" + describeField(field) + ") must not be 'static'");
    }
    if (getterHandle == null) {
      throw new IllegalArgumentException("The field(" + describeField(field) + ") requires a getter handle");
    }

    getterMember = revealMember(field, getterHandle, "getter");
    if (getterMember instanceof Field) {
      if (!(getterMember.equals(field) && (getterHandle.type().parameterCount() == 1))) {
        throw new IllegalArgumentException("The getter handle for the field(" + describeField(field) + ") must read that field, not access " + getterMember);
      }
    } else if (getterMember instanceof Method) {
      if (Modifier.isStatic(getterMember.getModifiers()) || (getterHandle.type().parameterCount() != 1) || (getterHandle.type().returnType() == void.class)) {
        throw new IllegalArgumentException("The getter handle for the field(" + describeField(field) + ") must invoke an instance method that takes no arguments and returns a value, not " + getterMember);
      }
    } else {
      throw new IllegalArgumentException("The getter handle for the field(" + describeField(field) + ") must read the field or invoke a method, not " + getterMember);
    }
    checkReceiver(field, getterHandle, "getter");

    if (setterHandle != null) {

      Member setterMember = revealMember(field, setterHandle, "setter");

      if (setterMember instanceof Field) {
        if (!(setterMember.equals(field) && (setterHandle.type().parameterCount() == 2) && (setterHandle.type().returnType() == void.class))) {
          throw new IllegalArgumentException("The setter handle for the field(" + describeField(field) + ") must write that field, not access " + setterMember);
        }
      } else if (setterMember instanceof Method) {
        if (Modifier.isStatic(setterMember.getModifiers()) || (setterHandle.type().parameterCount() != 2) || (!TypeUtility.isEssentiallyTheSameAs(setterHandle.type().parameterType(1), field.getType()))) {
          throw new IllegalArgumentException("The setter handle for the field(" + describeField(field) + ") must invoke an instance method that takes one argument accepting the type(" + field.getType().getName() + "), not " + setterMember);
        }
      } else {
        throw new IllegalArgumentException("The setter handle for the field(" + describeField(field) + ") must write the field or invoke a method, not " + setterMember);
      }
      checkReceiver(field, setterHandle, "setter");

      this.setterHandle = setterHandle.asType(SETTER_TYPE);
      this.setterMethod = setterMember instanceof Method;
    } else {
      this.setterHandle = null;
      this.setterMethod = false;
    }

    this.field = field;
    this.getterHandle = getterHandle.asType(GETTER_TYPE);
    this.getterMethod = getterMember instanceof Method;
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
   * Returns the reflected {@link Field} this accessor wraps. The field is not made accessible; read and
   * write it through {@link #get(Object)} and {@link #set(Object, Object)}.
   *
   * @return the underlying {@link Field} object
   */
  public Field getField () {

    return field;
  }

  /**
   * Reads this field's value from the given target, using the getter method when one is available
   * and reading the field directly otherwise.
   *
   * @param target the object instance from which the value should be read
   * @return the current field value
   * @throws IllegalAccessException    never; access is checked when the accessor is created, and the clause is
   *                                   kept so that existing callers compile unchanged
   * @throws InvocationTargetException if the getter method throws anything, which becomes the cause
   * @throws ClassCastException        if the field is read directly and {@code target} is not an instance of
   *                                   the class declaring it
   * @throws NullPointerException      if the field is read directly and {@code target} is {@code null}
   */
  public Object get (Object target)
    throws IllegalAccessException, InvocationTargetException {

    try {
      return (Object)getterHandle.invokeExact(target);
    } catch (RuntimeException | Error exception) {
      if (getterMethod) {
        throw new InvocationTargetException(exception);
      }

      throw exception;
    } catch (Throwable throwable) {
      throw new InvocationTargetException(throwable);
    }
  }

  /**
   * Writes a value to this field on the given target, using the setter method when one is available
   * and writing the field directly otherwise.
   *
   * @param target the object instance whose field value should be updated
   * @param value  the new value to assign
   * @throws IllegalAccessException    if this accessor has no setter handle, as for a {@code final} field
   *                                   without a setter method
   * @throws InvocationTargetException if the setter method throws anything, which becomes the cause
   * @throws ClassCastException        if the field is written directly and {@code target} or {@code value}
   *                                   has the wrong type
   * @throws NullPointerException      if the field is written directly and {@code target} is {@code null}, or
   *                                   the field is primitive and {@code value} is {@code null}
   */
  public void set (Object target, Object value)
    throws IllegalAccessException, InvocationTargetException {

    if (setterHandle == null) {
      throw new IllegalAccessException("The field(" + field.getName() + ") of class(" + field.getDeclaringClass().getName() + ") " + (Modifier.isFinal(field.getModifiers()) ? "is 'final' and has no setter" : "has no setter"));
    }

    try {
      setterHandle.invokeExact(target, value);
    } catch (RuntimeException | Error exception) {
      if (setterMethod) {
        throw new InvocationTargetException(exception);
      }

      throw exception;
    } catch (Throwable throwable) {
      throw new InvocationTargetException(throwable);
    }
  }

  /**
   * Resolves the field, method, or constructor that a direct method handle accesses.
   *
   * @param field  the field the handle is meant for, named in the failure message
   * @param handle the handle to resolve
   * @param role   {@code getter} or {@code setter}, named in the failure message
   * @return the member the handle accesses
   * @throws IllegalArgumentException if {@code handle} is not a direct method handle
   */
  private static Member revealMember (Field field, MethodHandle handle, String role) {

    try {
      return MethodHandles.reflectAs(Member.class, handle);
    } catch (IllegalArgumentException illegalArgumentException) {
      throw new IllegalArgumentException("The " + role + " handle for the field(" + describeField(field) + ") must be a direct method handle, as returned by the unreflect, unreflectGetter, unreflectSetter, findVirtual, findGetter, or findSetter methods of a lookup", illegalArgumentException);
    }
  }

  /**
   * Verifies that the receiver a handle takes, its first parameter, could be an instance of the class declaring
   * the field. An interface always could, because a subclass of the declaring class might implement it.
   *
   * @param field  the field the handle is meant for
   * @param handle the handle, which takes at least one parameter
   * @param role   {@code getter} or {@code setter}, named in the failure message
   * @throws IllegalArgumentException if the receiver is a class unrelated to the class declaring the field
   */
  private static void checkReceiver (Field field, MethodHandle handle, String role) {

    Class<?> receiverClass = handle.type().parameterType(0);

    if (!(receiverClass.isInterface() || receiverClass.isAssignableFrom(field.getDeclaringClass()) || field.getDeclaringClass().isAssignableFrom(receiverClass))) {
      throw new IllegalArgumentException("The " + role + " handle for the field(" + describeField(field) + ") takes a receiver of type(" + receiverClass.getName() + "), which is unrelated to the class declaring the field");
    }
  }

  /**
   * Names a field for an exception message.
   *
   * @param field the field to describe
   * @return the binary name of the declaring class, a dot, and the field name
   */
  private static String describeField (Field field) {

    return field.getDeclaringClass().getName() + "." + field.getName();
  }
}
