/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.web.json.query;

import java.time.ZonedDateTime;
import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.nutsnbolts.json.ZonedDateTimeXmlAdapter;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

@XmlRootElement(name = "array")
public class ArrayWhereOperand implements WhereOperand<Object[], Object[]> {

  private static final ZonedDateTimeXmlAdapter ZONED_DATE_TIME_XML_ADAPTER = new ZonedDateTimeXmlAdapter();

  private Object[] value;
  private String typeHint;

  public ArrayWhereOperand () {

  }

  public ArrayWhereOperand (String typeHint, Object[] value) {

    this.typeHint = typeHint;
    this.value = value;
  }

  public static ArrayWhereOperand instance (String typeHint, Object[] value) {

    return new ArrayWhereOperand(typeHint, value);
  }

  @Override
  @XmlTransient
  public Class<? extends Object[]> getTargetClass () {

    switch (typeHint) {
      case "boolean":
        return Boolean[].class;
      case "byte":
        return Byte[].class;
      case "character":
        return Character[].class;
      case "date":
        return Date[].class;
      case "double":
        return Double[].class;
      case "float":
        return Float[].class;
      case "integer":
        return Integer[].class;
      case "long":
        return Long[].class;
      case "short":
        return Short[].class;
      case "string":
        return String[].class;
      case "enum":
        return Enum[].class;
      default:
        throw new UnknownSwitchCaseException(typeHint);
    }
  }

  @Override
  @XmlElement(name = "type", required = true)
  public String getTypeHint () {

    return typeHint;
  }

  public void setTypeHint (String typeHint) {

    this.typeHint = typeHint;
  }

  @XmlElement(name = "value", required = true)
  @XmlJavaTypeAdapter(ArrayValueXmlAdapter.class)
  public Object[] getValue () {

    if (value == null) {

      return null;
    }

    switch (typeHint) {
      case "boolean":

        Boolean[] booleanArray = new Boolean[value.length];

        for (int index = 0; index < value.length; index++) {
          booleanArray[index] = (Boolean)value[index];
        }

        return booleanArray;
      case "byte":

        Byte[] byteArray = new Byte[value.length];

        for (int index = 0; index < value.length; index++) {
          byteArray[index] = (value[index] == null) ? null : ((Integer)value[index]).byteValue();
        }

        return byteArray;
      case "character":

        Character[] characterArray = new Character[value.length];

        for (int index = 0; index < value.length; index++) {

          String string;

          characterArray[index] = (value[index] == null) ? null : ((string = value[index].toString()).length() == 0) ? null : string.charAt(0);
        }

        return characterArray;
      case "date":

        ZonedDateTime[] dateTimes = new ZonedDateTime[value.length];

        for (int index = 0; index < value.length; index++) {
          dateTimes[index] = (value[index] == null) ? null : ZONED_DATE_TIME_XML_ADAPTER.unmarshal(value[index].toString());
        }

        return dateTimes;
      case "double":

        Double[] doubleArray = new Double[value.length];

        for (int index = 0; index < value.length; index++) {
          doubleArray[index] = (Double)value[index];
        }

        return doubleArray;
      case "float":

        Float[] floatArray = new Float[value.length];

        for (int index = 0; index < value.length; index++) {
          floatArray[index] = (value[index] == null) ? null : ((Double)value[index]).floatValue();
        }

        return floatArray;
      case "integer":

        Integer[] integerArray = new Integer[value.length];

        for (int index = 0; index < value.length; index++) {
          integerArray[index] = (Integer)value[index];
        }

        return integerArray;
      case "long":

        Long[] longArray = new Long[value.length];

        for (int index = 0; index < value.length; index++) {
          longArray[index] = (value[index] == null) ? null : (value[index] instanceof Integer) ? ((Integer)value[index]).longValue() : (Long)value[index];
        }

        return longArray;
      case "short":

        Short[] shortArray = new Short[value.length];

        for (int index = 0; index < value.length; index++) {
          shortArray[index] = (value[index] == null) ? null : ((Integer)value[index]).shortValue();
        }

        return shortArray;
      case "string":

        String[] stringArray = new String[value.length];

        for (int index = 0; index < value.length; index++) {
          stringArray[index] = (value[index] == null) ? null : value[index].toString();
        }

        return stringArray;
      case "enum":

        return value;
      default:
        throw new UnknownSwitchCaseException(typeHint);
    }
  }

  public void setValue (Object[] value) {

    this.value = value;
  }
}
