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
import java.util.LinkedHashMap;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Abstract {@link JsonEntity} that extends {@link LinkedHashMap} so the map entries themselves serve as the parameter
 * store, enabling direct deserialization into the entity.
 */
public abstract class AbstractNakedMappedJsonEntity extends LinkedHashMap<String, Object> implements JsonEntity {

  private static final Class[] NO_ARG_SIGNATURE = new Class[0];

  /**
   * Retrieves the map entry for the given key and converts it to the requested type, applying any
   * {@link XmlJavaTypeAdapter} declared on the consuming parameter.
   *
   * @param key                  parameter name matching a map entry
   * @param clazz                target type to convert the value to
   * @param parameterAnnotations annotations present on the consuming parameter
   * @param <T>                  desired return type
   * @return the converted value, or {@code null} if no entry exists for the key
   * @throws ParameterProcessingException if adapter construction or type conversion fails
   */
  @Override
  public <T> T getParameter (String key, Class<T> clazz, ParameterAnnotations parameterAnnotations) {

    Object obj;

    if ((obj = get(key)) != null) {

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
