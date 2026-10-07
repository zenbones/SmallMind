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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a JavaBean-style setter method, parsing its attribute name and validating its signature
 * on construction so callers can safely invoke it and query its metadata.
 */
public class Setter implements Serializable {

  private final Class<?> attributeClass;
  private transient Method method;
  private final String attributeName;

  /**
   * Wraps the supplied method as a setter, parsing the attribute name from the method name and
   * verifying that it returns void and accepts exactly one parameter.
   *
   * @param method the public setter method to wrap; must start with {@code set} followed by a
   *               capitalised attribute name
   * @throws ReflectionContractException if the method name or signature violates setter conventions
   */
  public Setter (Method method)
    throws ReflectionContractException {

    this.method = method;

    if (!(method.getName().startsWith("set") && (method.getName().length() > 3) && Character.isUpperCase(method.getName().charAt(3)))) {
      throw new ReflectionContractException("The declared name of a setter method must start with 'set' followed by a camel case attribute name");
    }

    attributeName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);

    if (!(method.getReturnType().equals(Void.class) || method.getReturnType().equals(void.class))) {
      throw new ReflectionContractException("Setter for attribute (%s) must return void", attributeName);
    }
    if (method.getParameterTypes().length != 1) {
      throw new ReflectionContractException("Setter for attribute (%s) must declare a single parameter", attributeName);
    }

    attributeClass = method.getParameterTypes()[0];
  }

  /**
   * Returns the bean property name derived from the setter method name.
   *
   * @return the lower-camel-case attribute name, e.g. {@code firstName} for {@code setFirstName}
   */
  public String getAttributeName () {

    return attributeName;
  }

  /**
   * Returns the type of the single parameter accepted by the setter.
   *
   * @return the raw {@link Class} of the setter's parameter
   */
  public Class<?> getAttributeClass () {

    return attributeClass;
  }

  /**
   * Invokes the underlying setter on the supplied target object with the given value.
   *
   * @param target the object on which the setter should be called
   * @param value  the new value to assign to the property
   * @return the return value of the method invocation (typically {@code null} for setters)
   * @throws IllegalAccessException    if the underlying method is not accessible
   * @throws IllegalArgumentException  if {@code target} or {@code value} is incompatible
   * @throws InvocationTargetException if the setter throws a checked or unchecked exception
   */
  public Object invoke (Object target, Object value)
    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

    Object[] parameter = {value};

    return method.invoke(target, parameter);
  }

  /**
   * Writes the default fields followed by the declaring class and name of the wrapped method, which is
   * not itself serializable.
   *
   * @param objectOutputStream the stream to write to
   * @throws IOException if the stream cannot be written
   */
  @Serial
  private void writeObject (ObjectOutputStream objectOutputStream)
    throws IOException {

    objectOutputStream.defaultWriteObject();
    objectOutputStream.writeObject(method.getDeclaringClass());
    objectOutputStream.writeObject(method.getName());
  }

  /**
   * Reads the default fields, then re-resolves the wrapped method from its declaring class and name.
   *
   * @param objectInputStream the stream to read from
   * @throws IOException            if the stream cannot be read, or the method no longer exists
   * @throws ClassNotFoundException if the declaring class cannot be resolved
   */
  @Serial
  private void readObject (ObjectInputStream objectInputStream)
    throws IOException, ClassNotFoundException {

    Class<?> declaringClass;
    String methodName;

    objectInputStream.defaultReadObject();
    declaringClass = (Class<?>)objectInputStream.readObject();
    methodName = (String)objectInputStream.readObject();

    try {
      method = declaringClass.getDeclaredMethod(methodName, attributeClass);
    } catch (NoSuchMethodException noSuchMethodException) {

      InvalidObjectException invalidObjectException = new InvalidObjectException("Missing method(" + methodName + ") in class(" + declaringClass.getName() + ")");

      invalidObjectException.initCause(noSuchMethodException);
      throw invalidObjectException;
    }
  }
}
