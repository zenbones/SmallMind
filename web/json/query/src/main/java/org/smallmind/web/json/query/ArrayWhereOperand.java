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
package org.smallmind.web.json.query;

import java.util.Date;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.smallmind.nutsnbolts.json.ZonedDateTimeXmlAdapter;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.time.TimeUtility;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Represents an array-valued operand in a where clause, preserving the element type via hints.
 */
@XmlRootElement(name = "array", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class ArrayWhereOperand extends WhereOperand<Object[]> {

  private static final ZonedDateTimeXmlAdapter ZONED_DATE_TIME_XML_ADAPTER = new ZonedDateTimeXmlAdapter();

  private ArrayNode value;
  private Hint hint;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public ArrayWhereOperand () {

  }

  /**
   * Constructs an operand with an explicit hint describing the element type and a raw JSON array node.
   *
   * @param hint  element type hint
   * @param value JSON array holding operand values
   */
  public ArrayWhereOperand (Hint hint, ArrayNode value) {

    this.hint = hint;
    this.value = value;
  }

  /**
   * Constructs an operand by inferring a hint and serializing the provided array elements.
   *
   * @param array typed array to be wrapped as an operand
   * @throws QueryProcessingException if the array component type cannot be mapped to an operand
   */
  public ArrayWhereOperand (Object[] array) {

    Class<?> componentClass = array.getClass().getComponentType();

    if (Boolean.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.BOOLEAN);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Byte.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.BYTE);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Character.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.CHARACTER);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Date.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.DATE);
      value = JsonNodeFactory.instance.arrayNode();
      for (Object obj : array) {
        value.add(ZONED_DATE_TIME_XML_ADAPTER.marshal(TimeUtility.fromDate((Date)obj)));
      }
    } else if (Double.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.DOUBLE);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Float.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.FLOAT);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Integer.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.INTEGER);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Long.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.LONG);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Short.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.SHORT);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (String.class.equals(componentClass)) {
      hint = new ComponentHint(ComponentType.STRING);
      value = (ArrayNode)JsonCodec.writeAsJsonNode(array);
    } else if (Enum.class.isAssignableFrom(componentClass)) {
      hint = new EnumHint((Class<? extends Enum>)componentClass);
      value = JsonNodeFactory.instance.arrayNode();
      for (Object obj : array) {
        value.add(((Enum)obj).name());
      }
    } else {
      throw new QueryProcessingException("Unknown array type(%s)", componentClass.getName());
    }
  }

  /**
   * Convenience factory for an array operand.
   *
   * @param value array content
   * @return the constructed operand
   */
  public static ArrayWhereOperand instance (Object[] value) {

    return new ArrayWhereOperand(value);
  }

  /**
   * Resolves the logical element type of the array based on the hint.
   *
   * @return element type for downstream translation
   * @throws UnknownSwitchCaseException if the hint cannot be mapped to a supported element type
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return switch (hint.getHintType()) {
      case COMPONENT -> switch (((ComponentHint)hint).getType()) {
        case BOOLEAN -> ElementType.BOOLEAN;
        case DOUBLE, FLOAT, LONG, INTEGER, SHORT, BYTE -> ElementType.NUMBER;
        case CHARACTER, STRING -> ElementType.STRING;
        case DATE -> ElementType.DATE;
      };
      case ENUM -> ElementType.STRING;
    };
  }

  /**
   * Identifies this operand as representing an array.
   *
   * @return {@link OperandType#ARRAY}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.ARRAY;
  }

  /**
   * Converts the stored JSON array into a strongly typed Java array based on the hint.
   *
   * @return typed array value or {@code null} if no value is set
   * @throws QueryProcessingException   if the hint references an enum class that cannot be found
   * @throws UnknownSwitchCaseException if the hint type or component type is unsupported
   */
  @XmlTransient
  public Object[] get () {

    if (value == null) {

      return null;
    } else {
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

                characterArray[index] = (value.get(index) == null) ? null : (string = JsonCodec.convert(value.get(index), String.class)).isEmpty() ? null : string.charAt(0);
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

          try {

            Class<? extends Enum> enumClass = (Class<? extends Enum>)Class.forName(((EnumHint)hint).getType());
            Object[] enumArray = new Object[value.size()];

            for (int index = 0; index < value.size(); index++) {
              enumArray[index] = (value.get(index) == null) ? null : Enum.valueOf(enumClass, JsonCodec.convert(value.get(index), String.class));
            }

            return enumArray;
          } catch (ClassNotFoundException classNotFoundException) {
            throw new QueryProcessingException(classNotFoundException);
          }
        default:
          throw new UnknownSwitchCaseException(hint.getHintType().name());
      }
    }
  }

  /**
   * Returns the hint describing the array element type.
   *
   * @return element hint
   */
  @XmlElement(name = "hint")
  public Hint getHint () {

    return hint;
  }

  /**
   * Sets the hint describing the array element type.
   *
   * @param hint element hint
   */
  public void setHint (Hint hint) {

    this.hint = hint;
  }

  /**
   * Returns the raw JSON array value backing this operand.
   *
   * @return JSON array node or {@code null}
   */
  @XmlElement(name = "value", required = true)
  public ArrayNode getValue () {

    return value;
  }

  /**
   * Sets the raw JSON array value backing this operand.
   *
   * @param value JSON array node
   */
  public void setValue (ArrayNode value) {

    this.value = value;
  }
}
