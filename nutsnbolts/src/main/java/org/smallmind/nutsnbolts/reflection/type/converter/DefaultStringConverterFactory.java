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
package org.smallmind.nutsnbolts.reflection.type.converter;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultStringConverterFactory implements StringConverterFactory {

  private static final StringConverter[] NO_SUPPLEMENTAL_CONVERTERS = new StringConverter[0];
  private static final StringConverterFactory INSTANCE = new DefaultStringConverterFactory();

  private final ConcurrentHashMap<Class, StringConverter<?>> converterMap = new ConcurrentHashMap<Class, StringConverter<?>>();

  public static StringConverterFactory getInstance () {

    return INSTANCE;
  }

  public DefaultStringConverterFactory () {

    this(NO_SUPPLEMENTAL_CONVERTERS);
  }

  public DefaultStringConverterFactory (StringConverter<?>... supplementalStringConverters) {

    converterMap.put(Long.class, new LongStringConverter());
    converterMap.put(Character.class, new CharacterStringConverter());
    converterMap.put(Integer.class, new IntegerStringConverter());
    converterMap.put(Byte.class, new ByteStringConverter());
    converterMap.put(Short.class, new ShortStringConverter());
    converterMap.put(Float.class, new FloatStringConverter());
    converterMap.put(Double.class, new DoubleStringConverter());
    converterMap.put(Boolean.class, new BooleanStringConverter());
    converterMap.put(String.class, new StringStringConverter());

    if (supplementalStringConverters != null) {
      for (StringConverter<?> supplementalStringConverter : supplementalStringConverters) {
        converterMap.put(supplementalStringConverter.getType(), supplementalStringConverter);
      }
    }

    if (!converterMap.containsKey(Date.class)) {
      converterMap.put(Date.class, new DateStringConverter());
    }
  }

  public StringConverter getStringConverter (Class parameterClass)
    throws StringConversionException {

    StringConverter<?> stringConverter;
    Class convergedClass;

    if ((stringConverter = converterMap.get(convergedClass = getConvergedClass(parameterClass))) == null) {
      if (convergedClass.isEnum()) {
        converterMap.put(convergedClass, stringConverter = new EnumStringConverter((Class<? extends Enum<?>>)convergedClass));
      }
      else {
        throw new StringConversionException("No known converter for type(%s)", convergedClass.getName());
      }
    }

    return stringConverter;
  }

  private static Class getConvergedClass (Class parameterClass)
    throws StringConversionException {

    if (parameterClass.isPrimitive()) {
      if (parameterClass.equals(long.class)) {
        return Long.class;
      }
      if (parameterClass.equals(char.class)) {
        return Character.class;
      }
      if (parameterClass.equals(int.class)) {
        return Integer.class;
      }
      if (parameterClass.equals(byte.class)) {
        return Byte.class;
      }
      if (parameterClass.equals(short.class)) {
        return Short.class;
      }
      if (parameterClass.equals(float.class)) {
        return Float.class;
      }
      if (parameterClass.equals(double.class)) {
        return Double.class;
      }
      if (parameterClass.equals(boolean.class)) {
        return Boolean.class;
      }
    }

    return parameterClass;
  }

}
