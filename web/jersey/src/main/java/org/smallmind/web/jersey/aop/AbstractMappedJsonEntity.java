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
import java.util.Map;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Base JsonEntity implementation that exposes parameters from a named argument map.
 * Entries are converted on demand, optionally passing through any {@link XmlJavaTypeAdapter} associated with the parameter.
 */
public abstract class AbstractMappedJsonEntity implements JsonEntity {

  private static final Class[] NO_ARG_SIGNATURE = new Class[0];

  private Map<String, Object> arguments;

  /**
   * Constructs an empty entity with no argument map.
   */
  public AbstractMappedJsonEntity () {

  }

  /**
   * Constructs an entity that will resolve parameters from the supplied argument map.
   *
   * @param arguments map keyed by parameter name holding argument values
   */
  public AbstractMappedJsonEntity (Map<String, Object> arguments) {

    this.arguments = arguments;
  }

  /**
   * Returns the backing map of parameter values.
   *
   * @return map of argument values keyed by name, or {@code null} if none supplied
   */
  public Map<String, Object> getArguments () {

    return arguments;
  }

  /**
   * Replaces the argument map used for parameter lookup.
   *
   * @param arguments map of parameter values keyed by name
   */
  public void setArguments (Map<String, Object> arguments) {

    this.arguments = arguments;
  }

  /**
   * Resolves a parameter by name from the backing map, converting it to the requested type and honoring any
   * {@link XmlJavaTypeAdapter} annotation.
   *
   * @param key                  parameter name
   * @param clazz                target type
   * @param parameterAnnotations annotations present on the parameter definition
   * @return converted value, or {@code null} if the key is not present
   * @throws ParameterProcessingException if adapter construction or value conversion fails
   */
  @Override
  public <T> T getParameter (String key, Class<T> clazz, ParameterAnnotations parameterAnnotations) {

    Object obj;

    if ((obj = arguments.get(key)) != null) {

      XmlJavaTypeAdapter xmlJavaTypeAdapter;

      if ((xmlJavaTypeAdapter = parameterAnnotations.getAnnotation(XmlJavaTypeAdapter.class)) != null) {
        try {

          XmlAdapter xmlAdapter;
          Constructor<? extends XmlAdapter> adapterConstructor;

          if ((adapterConstructor = xmlJavaTypeAdapter.value().getConstructor(NO_ARG_SIGNATURE)) == null) {
            throw new ParameterProcessingException("XmlAdapter of type(%s) must have a no arg constructor", xmlJavaTypeAdapter.value().getName());
          }

          xmlAdapter = adapterConstructor.newInstance();

          return JsonCodec.convert(xmlAdapter.unmarshal(JsonCodec.convert(obj, GenericUtility.getTypeArgumentsOfSubclass(XmlAdapter.class, xmlAdapter.getClass()).get(0))), clazz);
        } catch (Exception exception) {
          throw new ParameterProcessingException(exception);
        }
      } else {

        return JsonCodec.convert(obj, clazz);
      }
    }

    return null;
  }
}
