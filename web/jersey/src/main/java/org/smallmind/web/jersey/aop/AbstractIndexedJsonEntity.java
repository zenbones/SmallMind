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
package org.smallmind.web.jersey.aop;

import java.lang.reflect.Constructor;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Base JsonEntity implementation that resolves parameters by their index within a backing argument array.
 * Subclasses typically expose ordered method arguments for AOP interception and binding.
 */
public abstract class AbstractIndexedJsonEntity implements JsonEntity {

  private static final Class[] NO_ARG_SIGNATURE = new Class[0];

  private Object[] arguments;

  /**
   * Constructs an entity with no backing arguments. Parameter lookup will return {@code null}.
   */
  public AbstractIndexedJsonEntity () {

  }

  /**
   * Constructs an entity with the supplied ordered arguments.
   *
   * @param arguments the argument array in invocation order; may be {@code null} if not available
   */
  public AbstractIndexedJsonEntity (Object[] arguments) {

    this.arguments = arguments;
  }

  /**
   * Retrieves the backing argument array.
   *
   * @return the ordered invocation arguments, or {@code null} if not set
   */
  public Object[] getArguments () {

    return arguments;
  }

  /**
   * Updates the stored invocation arguments.
   *
   * @param arguments the ordered invocation arguments to use for parameter extraction
   */
  public void setArguments (Object[] arguments) {

    this.arguments = arguments;
  }

  /**
   * Resolves a parameter by parsing the supplied key as a zero-based index into the argument array and converting
   * the value to the requested type, applying any {@link XmlJavaTypeAdapter} present on the parameter.
   *
   * @param key                  the string index of the desired argument
   * @param clazz                the target type to convert to
   * @param parameterAnnotations annotations present on the parameter being resolved
   * @return the converted argument value, or {@code null} if the index is out of bounds
   * @throws ParameterProcessingException if the key cannot be parsed, the index is negative, adapter construction fails,
   *                                      or conversion fails
   */
  @Override
  public <T> T getParameter (String key, Class<T> clazz, ParameterAnnotations parameterAnnotations) {

    int index;

    try {
      index = Integer.parseInt(key);
    } catch (NumberFormatException numberFormatException) {
      throw new ParameterProcessingException(numberFormatException);
    }

    if (index < 0) {
      throw new ParameterProcessingException("Entity argument index must be >= 0");
    } else if (index >= arguments.length) {

      return null;
    } else {

      XmlJavaTypeAdapter xmlJavaTypeAdapter;

      if ((xmlJavaTypeAdapter = parameterAnnotations.getAnnotation(XmlJavaTypeAdapter.class)) != null) {
        try {

          XmlAdapter xmlAdapter;
          Constructor<? extends XmlAdapter> adapterConstructor;

          if ((adapterConstructor = xmlJavaTypeAdapter.value().getConstructor(NO_ARG_SIGNATURE)) == null) {
            throw new ParameterProcessingException("XmlAdapter of type(%s) must have a no arg constructor", xmlJavaTypeAdapter.value().getName());
          }

          xmlAdapter = adapterConstructor.newInstance();

          return JsonCodec.convert(xmlAdapter.unmarshal(JsonCodec.convert(arguments[index], GenericUtility.getTypeArgumentsOfSubclass(XmlAdapter.class, xmlAdapter.getClass()).get(0))), clazz);
        } catch (Exception exception) {
          throw new ParameterProcessingException(exception);
        }
      } else {

        return JsonCodec.convert(arguments[index], clazz);
      }
    }
  }
}
