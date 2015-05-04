/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
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
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.web.jersey.util.JsonCodec;

public abstract class AbstractIndexedJsonEntity implements JsonEntity {

  private static final Class[] NO_ARG_SIGNATURE = new Class[0];

  private Object[] arguments;

  public AbstractIndexedJsonEntity () {

  }

  public AbstractIndexedJsonEntity (Object[] arguments) {

    this.arguments = arguments;
  }

  public Object[] getArguments () {

    return arguments;
  }

  public void setArguments (Object[] arguments) {

    this.arguments = arguments;
  }

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

          return JsonCodec.convert(xmlAdapter.unmarshal(arguments[index]), clazz);
        } catch (Exception exception) {
          throw new ParameterProcessingException(exception);
        }
      } else {

        return JsonCodec.convert(arguments[index], clazz);
      }
    }
  }
}
