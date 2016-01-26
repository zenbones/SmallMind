/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

public abstract class AnnotationLiteral<A extends Annotation> implements Annotation, Serializable {

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  private Class<A> annotationType;

  protected AnnotationLiteral () {

    this.annotationType = getAnnotationType(getClass());
  }

  public Class<? extends Annotation> annotationType () {

    return annotationType;
  }

  private Class<A> getAnnotationType (Class<?> definedClazz) {

    Type superClazz = definedClazz.getGenericSuperclass();
    Class<A> clazz;

    if (superClazz.equals(Object.class)) {
      throw new RuntimeException("Super class must be A parametrized type!");
    } else if (superClazz instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType)superClazz;
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

  @Override
  public boolean equals (Object other) {

    Method[] methods = (Method[])AccessController.doPrivileged(new PrivilegedAction() {

      public Object run () {

        return annotationType.getDeclaredMethods();
      }
    });

    if (other == this) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if (other instanceof Annotation) {
      Annotation annotOther = (Annotation)other;
      if (this.annotationType().equals(annotOther.annotationType())) {
        for (Method method : methods) {
          Object value = callMethod(this, method);
          Object annotValue = callMethod(annotOther, method);

          if ((value == null && annotValue != null) || (value != null && annotValue == null)) {
            return false;
          }

          if (value == null && annotValue == null) {
            continue;
          }

          Class<?> valueClass = value.getClass();
          Class<?> annotValueClass = annotValue.getClass();

          if (valueClass.isPrimitive() && annotValueClass.isPrimitive()) {
            if ((valueClass != Float.TYPE && annotValue != Float.TYPE)
                  || (valueClass != Double.TYPE && annotValue != Double.TYPE)) {
              if (value != annotValue) {
                return false;
              }
            }
          } else if (valueClass.isArray() && annotValueClass.isArray()) {
            Class<?> type = valueClass.getComponentType();
            if (type.isPrimitive()) {
              if (Long.TYPE == type) {
                if (!Arrays.equals(((Long[])value), (Long[])annotValue)) {
                  return false;
                }
              } else if (Integer.TYPE == type) {
                if (!Arrays.equals(((Integer[])value), (Integer[])annotValue)) {
                  return false;
                }
              } else if (Short.TYPE == type) {
                if (!Arrays.equals(((Short[])value), (Short[])annotValue)) {
                  return false;
                }
              } else if (Double.TYPE == type) {
                if (!Arrays.equals(((Double[])value), (Double[])annotValue)) {
                  return false;
                }
              } else if (Float.TYPE == type) {
                if (!Arrays.equals(((Float[])value), (Float[])annotValue)) {
                  return false;
                }
              } else if (Boolean.TYPE == type) {
                if (!Arrays.equals(((Boolean[])value), (Boolean[])annotValue)) {
                  return false;
                }
              } else if (Byte.TYPE == type) {
                if (!Arrays.equals(((Byte[])value), (Byte[])annotValue)) {
                  return false;
                }
              } else if (Character.TYPE == type) {
                if (!Arrays.equals(((Character[])value), (Character[])annotValue)) {
                  return false;
                }
              }
            } else {
              if (!Arrays.equals(((Object[])value), (Object[])annotValue)) {
                return false;
              }
            }
          } else if (value != null && annotValue != null) {
            if (!value.equals(annotValue)) {
              return false;
            }
          }
        }

        return true;
      }
    }

    return false;
  }

  private Object callMethod (Object instance, Method method) {

    boolean access = method.isAccessible();

    try {
      if (!method.isAccessible()) {
        AccessController.doPrivileged(new PrivilegedActionForAccessibleObject(method, true));
      }

      return method.invoke(instance, EMPTY_OBJECT_ARRAY);
    } catch (Exception e) {
      throw new RuntimeException("Exception in method call : " + method.getName(), e);
    } finally {
      AccessController.doPrivileged(new PrivilegedActionForAccessibleObject(method, access));
    }
  }

  @Override
  public int hashCode () {

    Method[] methods = (Method[])AccessController.doPrivileged(new PrivilegedAction() {

      public Object run () {

        return annotationType.getDeclaredMethods();
      }
    });

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

  @Override
  public String toString () {

    Method[] methods = (Method[])AccessController.doPrivileged(new PrivilegedAction() {

      public Object run () {

        return annotationType.getDeclaredMethods();
      }
    });
    StringBuilder sb = new StringBuilder("@" + annotationType().getName() + "(");
    int lenght = methods.length;

    for (int i = 0; i < lenght; i++) {
      // Member name
      sb.append(methods[i].getName()).append("=");

      // Member value
      sb.append(callMethod(this, methods[i]));

      if (i < lenght - 1) {
        sb.append(",");
      }
    }

    sb.append(")");

    return sb.toString();
  }

  protected static class PrivilegedActionForAccessibleObject implements PrivilegedAction<Object> {

    AccessibleObject object;
    boolean flag;

    protected PrivilegedActionForAccessibleObject (AccessibleObject object, boolean flag) {

      this.object = object;
      this.flag = flag;
    }

    public Object run () {

      object.setAccessible(flag);
      return null;
    }
  }
}

