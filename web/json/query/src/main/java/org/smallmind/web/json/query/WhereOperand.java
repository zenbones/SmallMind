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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.smallmind.web.json.scaffold.util.XmlPolymorphicSubClasses;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
@XmlPolymorphicSubClasses({ArrayWhereOperand.class, BooleanWhereOperand.class, ByteWhereOperand.class, CharacterWhereOperand.class, DateWhereOperand.class, DoubleWhereOperand.class, EnumWhereOperand.class, FloatWhereOperand.class, IntegerWhereOperand.class, LongWhereOperand.class, NullWhereOperand.class, ShortWhereOperand.class, StringWhereOperand.class})
public abstract class WhereOperand<I> {

  public static WhereOperand<?> fromJsonNode (JsonNode node) {

    switch (node.getNodeType()) {
      case BOOLEAN:
        return new BooleanWhereOperand(node.booleanValue());
      case NUMBER:
        switch (node.numberType()) {
          case DOUBLE:
            return new DoubleWhereOperand(node.doubleValue());
          case FLOAT:
            return new FloatWhereOperand(node.floatValue());
          case INT:
            return new IntegerWhereOperand(node.intValue());
          case LONG:
            return new LongWhereOperand(node.longValue());
          default:
            throw new QueryProcessingException("Unable to convert json number type(%s) to operand", node.numberType().name());
        }
      case STRING:
        return new StringWhereOperand(node.textValue());
      case NULL:
        return NullWhereOperand.instance();
      case ARRAY:
        if (node.size() == 0) {
          throw new QueryProcessingException("Unable to convert an empty array");
        } else {
          switch (node.get(0).getNodeType()) {
            case BOOLEAN:
              return new ArrayWhereOperand(new ComponentHint(ComponentType.BOOLEAN), (ArrayNode)node);
            case NUMBER:
              switch (node.numberType()) {
                case DOUBLE:
                  return new ArrayWhereOperand(new ComponentHint(ComponentType.DOUBLE), (ArrayNode)node);
                case FLOAT:
                  return new ArrayWhereOperand(new ComponentHint(ComponentType.FLOAT), (ArrayNode)node);
                case INT:
                  return new ArrayWhereOperand(new ComponentHint(ComponentType.INTEGER), (ArrayNode)node);
                case LONG:
                  return new ArrayWhereOperand(new ComponentHint(ComponentType.LONG), (ArrayNode)node);
                default:
                  throw new QueryProcessingException("Unable to convert json array of number type(%s) to operand", node.numberType().name());
              }
            case STRING:
              return new ArrayWhereOperand(new ComponentHint(ComponentType.STRING), (ArrayNode)node);
            default:
              throw new QueryProcessingException("Unable to convert json array of type(%s) to operand", node.getNodeType().name());
          }
        }
      default:
        throw new QueryProcessingException("Unable to convert json node type(%s) to operand", node.getNodeType().name());
    }
  }

  public abstract JsonType getJsonType ();

  public abstract OperandType getOperandType ();

  public abstract I get ();
}