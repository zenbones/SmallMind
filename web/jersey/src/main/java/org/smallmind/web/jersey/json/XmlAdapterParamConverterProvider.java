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
package org.smallmind.web.jersey.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * JAX-RS {@link ParamConverterProvider} that supplies converters backed by JAXB {@link XmlAdapter} instances
 * declared via {@link XmlJavaTypeAdapter}.
 */
public class XmlAdapterParamConverterProvider implements ParamConverterProvider {

  private final ConcurrentHashMap<Class<? extends XmlAdapter>, ParamConverter<?>> CONVERTER_MAP = new ConcurrentHashMap<>();

  /**
   * Returns a {@link ParamConverter} wrapping the {@link XmlAdapter} identified by an {@link XmlJavaTypeAdapter}
   * annotation on the parameter, creating and caching the adapter instance on first use.
   *
   * @param rawType     the raw Java type of the parameter
   * @param genericType the generic type of the parameter
   * @param annotations annotations present on the parameter
   * @param <T>         the parameter type
   * @return a converter backed by the declared adapter, or {@code null} if no {@link XmlJavaTypeAdapter} is present
   * @throws XmlAdapterParamConversionException if the adapter cannot be instantiated
   */
  @Override
  public <T> ParamConverter<T> getConverter (final Class<T> rawType, final Type genericType, final Annotation[] annotations) {

    for (Annotation annotation : annotations) {
      if (annotation.annotationType().equals(XmlJavaTypeAdapter.class)) {

        Class<? extends XmlAdapter> xmlAdapterClass;

        if ((xmlAdapterClass = ((XmlJavaTypeAdapter)annotation).value()) != null) {

          ParamConverter<?> paramConverter;

          if ((paramConverter = CONVERTER_MAP.get(xmlAdapterClass)) == null) {
            synchronized (CONVERTER_MAP) {
              if ((paramConverter = CONVERTER_MAP.get(xmlAdapterClass)) == null) {
                try {
                  CONVERTER_MAP.put(xmlAdapterClass, paramConverter = new XmlAdapterParamConverter<T>(xmlAdapterClass.getConstructor().newInstance()));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                  throw new XmlAdapterParamConversionException(exception);
                }
              }
            }
          }

          return (ParamConverter<T>)paramConverter;
        }
      }
    }

    return null;
  }

  /**
   * {@link ParamConverter} implementation that delegates string-to-object and object-to-string conversion to a
   * wrapped {@link XmlAdapter}.
   *
   * @param <T> the target type produced and consumed by the adapter
   */
  private static class XmlAdapterParamConverter<T> implements ParamConverter<T> {

    private final XmlAdapter<String, T> xmlAdapter;

    /**
     * Constructs a converter that wraps the given adapter.
     *
     * @param xmlAdapter the adapter to delegate conversion to
     */
    public XmlAdapterParamConverter (XmlAdapter<String, T> xmlAdapter) {

      this.xmlAdapter = xmlAdapter;
    }

    /**
     * Converts a string parameter value to the target type using {@link XmlAdapter#unmarshal(Object)}.
     *
     * @param value the string value to convert
     * @return the unmarshalled object
     * @throws XmlAdapterParamConversionException if unmarshalling throws an exception
     */
    @Override
    public T fromString (String value) {

      try {
        return xmlAdapter.unmarshal(value);
      } catch (Exception exception) {
        throw new XmlAdapterParamConversionException(exception);
      }
    }

    /**
     * Converts an object to its string representation using {@link XmlAdapter#marshal(Object)}.
     *
     * @param value the value to convert
     * @return the marshalled string
     * @throws XmlAdapterParamConversionException if marshalling throws an exception
     */
    @Override
    public String toString (T value) {

      try {
        return xmlAdapter.marshal(value);
      } catch (Exception exception) {
        throw new XmlAdapterParamConversionException(exception);
      }
    }
  }
}
