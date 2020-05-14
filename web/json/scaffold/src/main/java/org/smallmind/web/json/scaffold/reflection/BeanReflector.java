/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.web.json.scaffold.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class BeanReflector {

  public Object get (Object target, String methodChain)
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse(methodChain);

    if (components[components.length - 1].getArguments() != null) {
      throw new BeanAccessException("The last path component in the chain must not specify any arguments");
    }

    return applySubscripts(executeGetter(traverse(target, components), components[components.length - 1].getName()), components[components.length - 1].getSubscripts());
  }

  public Object set (Object target, String methodChain, Object value)
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse(methodChain);

    if (components[components.length - 1].getArguments() != null) {
      throw new BeanAccessException("The last path component in the chain must not specify any arguments");
    }

    return executeSetter(traverse(target, components), components[components.length - 1].getName(), components[components.length - 1].getSubscripts(), value);
  }

  public Object apply (Object target, String methodChain, Object... values)
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse(methodChain);

    if (components[components.length - 1].getArguments() != null) {
      throw new BeanAccessException("The last path component in the chain must not specify any arguments");
    }

    return applySubscripts(executeMethod(traverse(target, components), components[components.length - 1].getName(), values), components[components.length - 1].getSubscripts());
  }

  private Object traverse (Object target, PathComponent[] components)
    throws BeanAccessException {

    Object currentTarget = target;

    for (int index = 0; index < components.length - 1; index++) {
      currentTarget = executeGetterOrMethod(currentTarget, components[index]);
    }

    return currentTarget;
  }

  private Object executeGetterOrMethod (Object target, PathComponent component)
    throws BeanAccessException {

    if (component.getArguments() != null) {

      return applySubscripts(executeMethod(target, component.getName(), component.getArguments()), component.getSubscripts());
    } else {

      return applySubscripts(executeGetter(target, component.getName()), component.getSubscripts());
    }
  }

  private Object executeGetter (Object target, String name)
    throws BeanAccessException {

    if (target == null) {
      throw new BeanAccessException("Can not execute getter(%s) on a 'null' target", name);
    } else {

      Method method = null;

      try {
        // Is there a method with a proper getter name 'getXXX'
        method = target.getClass().getMethod(BeanUtility.asGetterName(name));
      } catch (NoSuchMethodException noGetterException) {
        try {
          // If not, is there a boolean version 'isXXX'
          method = target.getClass().getMethod(BeanUtility.asIsName(name));
          if (!(Boolean.class.equals(method.getReturnType()) || boolean.class.equals(method.getReturnType()))) {
            throw new BeanAccessException("Found an 'is' method(%s) in class(%s), but it doesn't return a 'boolean' type", method.getName(), target.getClass().getName());
          }
        } catch (NoSuchMethodException noIsException) {
          // no method matches
        }
      }

      if (method != null) {
        try {

          return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException exception) {
          throw new BeanAccessException(exception);
        }
      } else {

        Field field;

        try {
          field = target.getClass().getField(name);
        } catch (NoSuchFieldException noSuchFieldException) {
          throw new BeanAccessException("No 'getter' method(%s or %s) or field(%s) found in class(%s)", BeanUtility.asGetterName(name), BeanUtility.asIsName(name), name, target.getClass().getName());
        }

        try {
          return field.get(target);
        } catch (IllegalAccessException illegalAccessException) {
          throw new BeanAccessException(illegalAccessException);
        }
      }
    }
  }

  private Object executeSetter (Object target, String name, int[] subscripts, Object value)
    throws BeanAccessException {

    if (target == null) {
      throw new BeanAccessException("Can not execute setter(%s) on a 'null' target", name);
    } else {

      String setterName = BeanUtility.asSetterName(name);

      for (Method method : target.getClass().getMethods()) {
        if (setterName.equals(method.getName()) && (method.getParameterCount() == 1)) {
          if ((subscripts != null) && (subscripts.length > 0)) {
            try {
              method.invoke(target, applyIndexedValue(target, name, subscripts, value));
            } catch (IllegalAccessException | InvocationTargetException exception) {
              throw new BeanAccessException(exception);
            }

            return null;
          } else {
            try {

              return method.invoke(target, (value == null) ? null : JsonCodec.convert(value, method.getParameterTypes()[0]));
            } catch (IllegalAccessException | InvocationTargetException exception) {
              throw new BeanAccessException(exception);
            }
          }
        }
      }
    }

    Field field;

    try {
      field = target.getClass().getField(name);
    } catch (NoSuchFieldException noSuchFieldException) {
      throw new BeanAccessException("No 'setter' method(%s) or field(%s) found in class(%s)", BeanUtility.asSetterName(name), name, target.getClass().getName());
    }

    if ((subscripts != null) && (subscripts.length > 0)) {
      try {
        field.set(target, applyIndexedValue(target, name, subscripts, value));
      } catch (IllegalAccessException illegalAccessException) {
        throw new BeanAccessException(illegalAccessException);
      }
    } else {
      try {
        field.set(target, (value == null) ? null : JsonCodec.convert(value, field.getType()));
      } catch (IllegalAccessException illegalAccessException) {
        throw new BeanAccessException(illegalAccessException);
      }
    }

    return null;
  }

  private Object applyIndexedValue (Object target, String name, int[] subscripts, Object value)
    throws BeanAccessException {

    Object currentValue = executeGetter(target, name);
    Object penultimateValue = applySubscripts(currentValue, Arrays.copyOf(subscripts, subscripts.length - 1));

    if (penultimateValue == null) {
      throw new BeanAccessException("Unable to apply the %s subscript(%d) to a 'null' value", indexToNth(subscripts.length - 1), subscripts[subscripts.length - 1]);
    } else {

      Class<?> penultimateComponentType;

      if ((penultimateComponentType = penultimateValue.getClass().getComponentType()) == null) {
        throw new BeanAccessException("Unable to apply the %s subscript(%d) to value type(%s)", indexToNth(subscripts.length - 1), subscripts[subscripts.length - 1], penultimateValue.getClass());
      } else {
        Array.set(penultimateValue, subscripts[subscripts.length - 1], (value == null) ? null : JsonCodec.convert(value, penultimateComponentType));

        return currentValue;
      }
    }
  }

  private Object executeMethod (Object target, String name, Object[] arguments)
    throws BeanAccessException {

    if (target == null) {
      throw new BeanAccessException("Can not execute method(%s) on a 'null' target", name);
    } else {
      for (Method method : target.getClass().getMethods()) {
        if (method.getName().equals(name) && (method.getParameterCount() == ((arguments == null) ? 0 : arguments.length))) {

          Object[] parameterValues = new Object[method.getParameterCount()];
          boolean matched = true;

          if ((arguments != null) && (arguments.length > 0)) {
            try {

              int index = 0;

              for (Class<?> parameterType : method.getParameterTypes()) {
                parameterValues[index] = (arguments[index] == null) ? null : JsonCodec.convert(arguments[index], parameterType);
                index++;
              }
            } catch (IllegalArgumentException illegalArgumentException) {
              matched = false;
            }
          }

          if (matched) {
            try {

              return method.invoke(target, parameterValues);
            } catch (IllegalAccessException | InvocationTargetException exception) {
              throw new BeanAccessException(exception);
            }
          }
        }
      }

      try {
        throw new BeanAccessException("No method(name=%s, arguments=%s) found in class(%s)", name, JsonCodec.writeAsString(arguments), target.getClass().getName());
      } catch (JsonProcessingException jsonProcessingException) {
        throw new BeanAccessException(jsonProcessingException);
      }
    }
  }

  private Object applySubscripts (Object value, int[] subscripts)
    throws BeanAccessException {

    Object indexedValue = value;

    if ((subscripts != null) && (subscripts.length > 0)) {
      for (int index = 0; index < subscripts.length; index++) {
        if (indexedValue == null) {
          throw new BeanAccessException("Unable to apply the %s subscript(%d) to a 'null' value", indexToNth(index), subscripts[index]);
        } else if (!indexedValue.getClass().isArray()) {
          throw new BeanAccessException("Unable to apply the %s subscript(%d) to value type(%s)", indexToNth(index), subscripts[index], indexedValue.getClass());
        }

        try {
          indexedValue = Array.get(indexedValue, subscripts[index]);
        } catch (Exception exception) {
          throw new BeanAccessException(exception, "Unable to apply the %s subscript(%d)", indexToNth(index), subscripts[index]);
        }
      }
    }

    return indexedValue;
  }

  private String indexToNth (int index) {

    switch (index) {
      case 0:
        return "1st";
      case 1:
        return "2nd";
      case 2:
        return "3rd";
      default:
        return (index + 1) + "th";
    }
  }
}
