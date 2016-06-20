/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.query;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.joda.time.DateTime;
import org.smallmind.nutsnbolts.json.DateTimeXmlAdapter;

@XmlRootElement(name = "array")
public class ArrayWhereOperand extends WhereOperand<Object[]> {

  private static final DateTimeXmlAdapter DATE_TIME_XML_ADAPTER = new DateTimeXmlAdapter();

  private Object[] value;
  private String type;

  public ArrayWhereOperand () {

  }

  public ArrayWhereOperand (String type, Object[] value) {

    this.type = type;
    this.value = value;
  }

  @Override
  public Object[] extract (WhereOperandTransformer transformer) {

    if (value == null) {

      return null;
    }

    switch (type) {
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

        DateTime[] dateTimes = new DateTime[value.length];

        for (int index = 0; index < value.length; index++) {
          dateTimes[index] = (value[index] == null) ? null : DATE_TIME_XML_ADAPTER.unmarshal(value[index].toString());
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
      default:

        Class<? extends Enum> enumClass;

        if (transformer == null) {
          throw new WhereValidationException("Translation of enum type(%s) requires an implementation of a WhereOperandTransformer", type);
        } else if ((enumClass = transformer.getEnumType(type)) == null) {
          throw new WhereValidationException("Missing a %s capable of transforming enum type(%s)", WhereOperandTransformer.class.getSimpleName(), type);
        } else {

          Enum[] enums = new Enum[value.length];

          for (int index = 0; index < value.length; index++) {
            enums[index] = (value[index] == null) ? null : Enum.valueOf(enumClass, value[index].toString());
          }

          return enums;
        }
    }
  }

  @XmlElement(name = "type", required = true)
  public String getType () {

    return type;
  }

  public void setType (String type) {

    this.type = type;
  }

  @XmlElement(name = "value", required = true)
  @XmlJavaTypeAdapter(ArrayValueXmlAdapter.class)
  public Object[] getValue () {

    return value;
  }

  public void setValue (Object[] value) {

    this.value = value;
  }
}
