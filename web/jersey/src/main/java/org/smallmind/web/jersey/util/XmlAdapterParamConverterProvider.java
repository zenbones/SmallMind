/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.jersey.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Provider
public class XmlAdapterParamConverterProvider implements ParamConverterProvider {

  private final ConcurrentHashMap<Class<? extends XmlAdapter>, ParamConverter<?>> CONVERTER_MAP = new ConcurrentHashMap<>();

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
                  CONVERTER_MAP.put(xmlAdapterClass, new XmlAdapterParamConverter<T>(xmlAdapterClass.newInstance()));
                } catch (InstantiationException | IllegalAccessException exception) {
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

  private class XmlAdapterParamConverter<T> implements ParamConverter<T> {

    private XmlAdapter<String, T> xmlAdapter;

    public XmlAdapterParamConverter (XmlAdapter<String, T> xmlAdapter) {

      this.xmlAdapter = xmlAdapter;
    }

    @Override
    public T fromString (String value) {

      try {
        return xmlAdapter.unmarshal(value);
      } catch (Exception exception) {
        throw new XmlAdapterParamConversionException(exception);
      }
    }

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
