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

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.smallmind.nutsnbolts.json.ZonedDateTimeXmlAdapter;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.web.json.scaffold.util.JsonCodec;

@XmlRootElement(name = "array")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class ArrayWhereOperand extends WhereOperand<Object[]> {

  private static final ZonedDateTimeXmlAdapter ZONED_DATE_TIME_XML_ADAPTER = new ZonedDateTimeXmlAdapter();

  private ArrayNode value;
  private Hint hint;

  public ArrayWhereOperand () {

  }

  public ArrayWhereOperand (Object[] array) {

    Class<?> componentClass = array.getClass().getComponentType();

    if (Boolean.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.BOOLEAN);
    } else if (Byte.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.BYTE);
    } else if (Character.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.CHARACTER);
    } else if (Date.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.DATE);
    } else if (Double.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.DOUBLE);
    } else if (Float.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.FLOAT);
    } else if (Integer.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.INTEGER);
    } else if (Long.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.LONG);
    } else if (Short.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.SHORT);
    } else if (String.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.STRING);
    } else if (Enum.class.equals(componentClass)) {
      hint = new EnumHint((Class<? extends Enum>)componentClass);
    } else {
      throw new QueryProcessingException("Unknown array type(%s)", componentClass.getName());
    }

    value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
  }

  public static ArrayWhereOperand instance (Object[] value) {

    return new ArrayWhereOperand(value);
  }

  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.ARRAY;
  }

  @XmlTransient
  public Object[] get () {

    if (value == null) {

      return null;
    }

    switch (hint.getHintType()) {
      case COMPONENT:
        switch (((ComponentHint)hint).getType()) {
          case BOOLEAN:

            Boolean[] booleanArray = new Boolean[value.size()];

            for (int index = 0; index < value.size(); index++) {
              booleanArray[index] = JsonCodec.convert(value.get(index), Boolean.class);
            }

            return booleanArray;
          case BYTE:

            Byte[] byteArray = new Byte[value.size()];

            for (int index = 0; index < value.size(); index++) {
              byteArray[index] = (value.get(index) == null) ? null : JsonCodec.convert(value.get(index), Byte.class);
            }

            return byteArray;
          case CHARACTER:

            Character[] characterArray = new Character[value.size()];

            for (int index = 0; index < value.size(); index++) {

              String string;

              characterArray[index] = (value.get(index) == null) ? null : ((string = JsonCodec.convert(value.get(index), String.class)).length() == 0) ? null : string.charAt(0);
            }

            return characterArray;
          case DATE:

            Date[] dates = new Date[value.size()];

            for (int index = 0; index < value.size(); index++) {
              dates[index] = (value.get(index) == null) ? null : Date.from(ZONED_DATE_TIME_XML_ADAPTER.unmarshal(JsonCodec.convert(value.get(index), String.class)).toInstant());
            }

            return dates;
          case DOUBLE:

            Double[] doubleArray = new Double[value.size()];

            for (int index = 0; index < value.size(); index++) {
              doubleArray[index] = JsonCodec.convert(value.get(index), Double.class);
            }

            return doubleArray;
          case FLOAT:

            Float[] floatArray = new Float[value.size()];

            for (int index = 0; index < value.size(); index++) {
              floatArray[index] = (value.get(index) == null) ? null : JsonCodec.convert(value.get(index), Float.class);
            }

            return floatArray;
          case INTEGER:

            Integer[] integerArray = new Integer[value.size()];

            for (int index = 0; index < value.size(); index++) {
              integerArray[index] = JsonCodec.convert(value.get(index), Integer.class);
            }

            return integerArray;
          case LONG:

            Long[] longArray = new Long[value.size()];

            for (int index = 0; index < value.size(); index++) {
              longArray[index] = (value.get(index) == null) ? null : JsonCodec.convert(value.get(index), Long.class);
            }

            return longArray;
          case SHORT:

            Short[] shortArray = new Short[value.size()];

            for (int index = 0; index < value.size(); index++) {
              shortArray[index] = (value.get(index) == null) ? null : JsonCodec.convert(value.get(index), Short.class);
            }

            return shortArray;
          case STRING:

            String[] stringArray = new String[value.size()];

            for (int index = 0; index < value.size(); index++) {
              stringArray[index] = (value.get(index) == null) ? null : JsonCodec.convert(value.get(index), String.class);
            }

            return stringArray;
          default:
            throw new UnknownSwitchCaseException(((ComponentHint)hint).getType().name());
        }
      case ENUM:

      default:
        throw new UnknownSwitchCaseException(hint.getHintType().name());
    }
  }

  @XmlElement(name = "hint")
  public Hint getHint () {

    return hint;
  }

  public void setHint (Hint hint) {

    this.hint = hint;
  }

  @XmlElement(name = "value", required = true)
  public ArrayNode getValue () {

    return value;
  }

  public void setValue (ArrayNode value) {

    this.value = value;
  }
}
