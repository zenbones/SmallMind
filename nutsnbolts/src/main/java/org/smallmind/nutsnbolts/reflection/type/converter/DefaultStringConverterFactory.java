/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.smallmind.nutsnbolts.reflection.type.PrimitiveType;

public class DefaultStringConverterFactory implements StringConverterFactory {

  private static final StringConverterFactory INSTANCE = new DefaultStringConverterFactory();
  private static final Class[] KNOWN_CONVERSIONS = new Class[] {Long.class, Character.class, Integer.class, Byte.class, Short.class, Float.class, Double.class, Boolean.class, String.class, Date.class};

  private final ConcurrentHashMap<Class, StringConverter<?>> converterMap = new ConcurrentHashMap<Class, StringConverter<?>>();

  public static StringConverterFactory getInstance () {

    return INSTANCE;
  }

  public DefaultStringConverterFactory () {

    this(new DateStringConverter());
  }

  public DefaultStringConverterFactory (StringConverter dateStringConverter) {

    if (!dateStringConverter.getPrimitiveType().equals(PrimitiveType.DATE)) {
      throw new IllegalArgumentException("Optional StringConverter for 'Date' must declare the proper PrimitiveType(" + dateStringConverter.getPrimitiveType().name() + ")");
    }

    converterMap.put(Long.class, new LongStringConverter());
    converterMap.put(Character.class, new CharacterStringConverter());
    converterMap.put(Integer.class, new IntegerStringConverter());
    converterMap.put(Byte.class, new ByteStringConverter());
    converterMap.put(Short.class, new ShortStringConverter());
    converterMap.put(Float.class, new FloatStringConverter());
    converterMap.put(Double.class, new DoubleStringConverter());
    converterMap.put(Boolean.class, new BooleanStringConverter());
    converterMap.put(String.class, new StringStringConverter());

    converterMap.put(Date.class, dateStringConverter);
  }

  public StringConverter getStringConverter (Class parameterClass)
    throws StringConversionException {

    Class convergedClass;

    if ((convergedClass = getConvergedClass(parameterClass)).isEnum()) {

      StringConverter stringConverter;

      if ((stringConverter = converterMap.get(convergedClass)) == null) {
        converterMap.put(convergedClass, stringConverter = new EnumStringConverter((Class<? extends Enum<?>>)convergedClass));
      }

      return stringConverter;
    }

    return converterMap.get(convergedClass);
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

    if (!(parameterClass.isEnum() || isKnownConversion(parameterClass))) {
      throw new StringConversionException("Can't convert to a 'setter' value of type(%s)", parameterClass.getName());
    }

    return parameterClass;
  }

  private static boolean isKnownConversion (Class parameterClass) {

    for (Class knownClass : KNOWN_CONVERSIONS) {
      if (knownClass.equals(parameterClass)) {

        return true;
      }
    }

    return false;
  }
}
