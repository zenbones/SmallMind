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
package org.smallmind.nutsnbolts.lang;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.PrivilegedAction;
import java.util.Arrays;

/**
 * Base class for programmatic annotation literals similar to CDI's {@code AnnotationLiteral}.
 * Subclasses declare their annotation type via a generic parameter and can implement annotation
 * methods directly while inheriting the default {@link Annotation} contract implementations.
 *
 * @param <A> the annotation type represented by the literal
 */
public abstract class AnnotationLiteral<A extends Annotation> implements Annotation, Serializable {

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  private final Class<A> annotationType;

  /**
   * Determines and stores the annotation type from the concrete subclass' generic signature.
   */
  protected AnnotationLiteral () {

    this.annotationType = getAnnotationType(getClass());
  }

  /**
   * {@inheritDoc}
   */
  public Class<? extends Annotation> annotationType () {

    return annotationType;
  }

  /**
   * Walks the inheritance hierarchy to discover the concrete annotation type parameter.
   *
   * @param definedClazz the subclass from which to start discovery
   * @return the resolved annotation type
   * @throws RuntimeException if the annotation type cannot be determined
   */
  private Class<A> getAnnotationType (Class<?> definedClazz) {

    Type superClazz = definedClazz.getGenericSuperclass();
    Class<A> clazz;

    if (superClazz.equals(Object.class)) {
      throw new RuntimeException("Super class must be A parametrized type!");
    } else if (superClazz instanceof ParameterizedType paramType) {
      Type[] actualArgs = paramType.getActualTypeArguments();

      if (actualArgs.length == 1) {
        //Actual annotation type
        Type type = actualArgs[0];

        if (type instanceof Class) {
          clazz = (Class<A>)type;
          return clazz;
        } else {
          throw new RuntimeException("Not a class type!");
        }
      } else {
        throw new RuntimeException("More than one parametric type!");
      }
    } else {
      return getAnnotationType((Class<?>)superClazz);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Compares annotation members for equality according to the {@link Annotation} contract.
   */
  @Override
  public boolean equals (Object obj) {

    Method[] methods = annotationType.getDeclaredMethods();

    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }

    if (obj instanceof Annotation other) {

      if (this.annotationType().equals(other.annotationType())) {
        for (Method method : methods) {

          Object thisValue = callMethod(this, method);
          Object otherValue = callMethod(other, method);

          if ((thisValue != null) || (otherValue != null)) {
            if (thisValue == null || otherValue == null) {

              return false;
            }

            Class<?> thisValueClass = thisValue.getClass();
            Class<?> otherValueClass = otherValue.getClass();

            if (thisValueClass.isPrimitive() && otherValueClass.isPrimitive()) {
              if (thisValue != otherValue) {
                return false;
              }
            } else if (thisValueClass.isArray() && otherValueClass.isArray()) {
              Class<?> type = thisValueClass.getComponentType();
              if (type.isPrimitive()) {
                if (Long.TYPE == type) {
                  if (!Arrays.equals(((Long[])thisValue), (Long[])otherValue)) {
                    return false;
                  }
                } else if (Integer.TYPE == type) {
                  if (!Arrays.equals(((Integer[])thisValue), (Integer[])otherValue)) {
                    return false;
                  }
                } else if (Short.TYPE == type) {
                  if (!Arrays.equals(((Short[])thisValue), (Short[])otherValue)) {
                    return false;
                  }
                } else if (Double.TYPE == type) {
                  if (!Arrays.equals(((Double[])thisValue), (Double[])otherValue)) {
                    return false;
                  }
                } else if (Float.TYPE == type) {
                  if (!Arrays.equals(((Float[])thisValue), (Float[])otherValue)) {
                    return false;
                  }
                } else if (Boolean.TYPE == type) {
                  if (!Arrays.equals(((Boolean[])thisValue), (Boolean[])otherValue)) {
                    return false;
                  }
                } else if (Byte.TYPE == type) {
                  if (!Arrays.equals(((Byte[])thisValue), (Byte[])otherValue)) {
                    return false;
                  }
                } else if (Character.TYPE == type) {
                  if (!Arrays.equals(((Character[])thisValue), (Character[])otherValue)) {
                    return false;
                  }
                }
              } else {
                if (!Arrays.equals(((Object[])thisValue), (Object[])otherValue)) {
                  return false;
                }
              }
            } else {
              if (!thisValue.equals(otherValue)) {
                return false;
              }
            }
          }
        }

        return true;
      }
    }

    return false;
  }

  /**
   * Invokes an annotation member method using privileged access when necessary.
   *
   * @param instance the annotation instance
   * @param method   the method to invoke
   * @return the returned value
   * @throws RuntimeException if the method cannot be invoked
   */
  private Object callMethod (Object instance, Method method) {

    boolean access = method.canAccess(instance);

    try {
      if (!access) {
        new PrivilegedActionForAccessibleObject(method, true);
      }

      return method.invoke(instance, EMPTY_OBJECT_ARRAY);
    } catch (Exception e) {
      throw new RuntimeException("Exception in method call : " + method.getName(), e);
    } finally {
      new PrivilegedActionForAccessibleObject(method, access);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode () {

    Method[] methods = annotationType.getDeclaredMethods();

    int hashCode = 0;
    for (Method method : methods) {
      // Member name
      int name = 127 * method.getName().hashCode();

      // Member value
      Object object = callMethod(this, method);
      int value = 0;
      if (object.getClass().isArray()) {
        Class<?> type = object.getClass().getComponentType();
        if (type.isPrimitive()) {
          if (Long.TYPE == type) {
            value = Arrays.hashCode((Long[])object);
          } else if (Integer.TYPE == type) {
            value = Arrays.hashCode((Integer[])object);
          } else if (Short.TYPE == type) {
            value = Arrays.hashCode((Short[])object);
          } else if (Double.TYPE == type) {
            value = Arrays.hashCode((Double[])object);
          } else if (Float.TYPE == type) {
            value = Arrays.hashCode((Float[])object);
          } else if (Boolean.TYPE == type) {
            value = Arrays.hashCode((Long[])object);
          } else if (Byte.TYPE == type) {
            value = Arrays.hashCode((Byte[])object);
          } else if (Character.TYPE == type) {
            value = Arrays.hashCode((Character[])object);
          }
        } else {
          value = Arrays.hashCode((Object[])object);
        }
      } else {
        value = object.hashCode();
      }

      hashCode += name ^ value;
    }
    return hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString () {

    Method[] methods = annotationType.getDeclaredMethods();
    StringBuilder sb = new StringBuilder("@" + annotationType().getName() + "(");
    int length = methods.length;

    for (int i = 0; i < length; i++) {
      // Member name
      sb.append(methods[i].getName()).append("=");
      // Member value
      sb.append(callMethod(this, methods[i]));
      if (i < length - 1) {
        sb.append(",");
      }
    }
    sb.append(")");

    return sb.toString();
  }

  protected static class PrivilegedActionForAccessibleObject implements PrivilegedAction<Object> {

    AccessibleObject object;
    boolean flag;

    /**
     * Adjusts accessibility on an {@link AccessibleObject} under a privileged action.
     *
     * @param object the reflective object to modify
     * @param flag   whether the object should be accessible
     */
    private PrivilegedActionForAccessibleObject (AccessibleObject object, boolean flag) {

      this.object = object;
      this.flag = flag;
    }

    /**
     * {@inheritDoc}
     */
    public Object run () {

      object.setAccessible(flag);
      return null;
    }
  }
}
