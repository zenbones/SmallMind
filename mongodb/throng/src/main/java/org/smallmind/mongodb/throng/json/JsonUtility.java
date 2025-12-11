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
package org.smallmind.mongodb.throng.json;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;

public class JsonUtility {

  public static BsonValue fromJson (JsonNode jsonNode) {

    switch (jsonNode.getNodeType()) {
      case OBJECT:

        BsonDocument bsonDocument = new BsonDocument();

        for (Map.Entry<String, JsonNode> propertyEntry : jsonNode.properties()) {
          bsonDocument.append(propertyEntry.getKey(), fromJson(propertyEntry.getValue()));
        }

        return bsonDocument;
      case ARRAY:

        BsonArray bsonArray = new BsonArray();

        for (JsonNode elementNode : jsonNode) {
          bsonArray.add(fromJson(elementNode));
        }

        return bsonArray;
      case NULL:

        return new BsonNull();
      case NUMBER:
        if (jsonNode.isDouble() || jsonNode.isFloat()) {

          return new BsonDouble(jsonNode.asDouble());
        } else if (jsonNode.isLong()) {

          return new BsonInt64(jsonNode.asLong());
        } else {

          return new BsonInt32(jsonNode.asInt());
        }
      case BOOLEAN:

        return new BsonBoolean(jsonNode.booleanValue());
      case STRING:

        return new BsonString(jsonNode.textValue());
      default:
        throw new BSONParsingException("Unknown json node type(%s)", jsonNode.getNodeType().name());
    }
  }

  public static JsonNode toJson (BsonValue bsonValue) {

    switch (bsonValue.getBsonType()) {
      case DOCUMENT:

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        for (Map.Entry<String, BsonValue> documentEntry : ((BsonDocument)bsonValue).entrySet()) {
          objectNode.set(documentEntry.getKey(), toJson(documentEntry.getValue()));
        }

        return objectNode;
      case ARRAY:

        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();

        for (BsonValue element : (BsonArray)bsonValue) {
          arrayNode.add(toJson(element));
        }

        return arrayNode;
      case NULL:

        return JsonNodeFactory.instance.nullNode();
      case UNDEFINED:

        return JsonNodeFactory.instance.nullNode();
      case INT32:

        return JsonNodeFactory.instance.numberNode(((BsonInt32)bsonValue).getValue());
      case INT64:

        return JsonNodeFactory.instance.numberNode(((BsonInt64)bsonValue).getValue());
      case DOUBLE:

        return JsonNodeFactory.instance.numberNode(((BsonDouble)bsonValue).getValue());
      case DECIMAL128:

        return JsonNodeFactory.instance.numberNode(((BsonDecimal128)bsonValue).getValue().bigDecimalValue());
      case BOOLEAN:

        return JsonNodeFactory.instance.booleanNode(((BsonBoolean)bsonValue).getValue());
      case STRING:

        return JsonNodeFactory.instance.textNode(((BsonString)bsonValue).getValue());
      default:
        throw new BSONParsingException("Unknown bson type(%s)", bsonValue.getBsonType().name());
    }
  }

  public static JsonNode read (BsonReader reader) {

    BsonType bsonType;

    switch (bsonType = reader.getCurrentBsonType()) {
      case DOCUMENT:

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
          objectNode.set(reader.readName(), read(reader));
        }
        reader.readEndDocument();

        return objectNode;
      case ARRAY:

        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();

        reader.readStartArray();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
          arrayNode.add(read(reader));
        }
        reader.readEndArray();

        return arrayNode;
      case NULL:
        reader.readNull();
        return JsonNodeFactory.instance.nullNode();
      case STRING:
        return JsonNodeFactory.instance.textNode(reader.readString());
      case BOOLEAN:
        return JsonNodeFactory.instance.booleanNode(reader.readBoolean());
      case DOUBLE:
        return JsonNodeFactory.instance.numberNode(reader.readDouble());
      case INT32:
        return JsonNodeFactory.instance.numberNode(reader.readInt32());
      case INT64:
        return JsonNodeFactory.instance.numberNode(reader.readInt64());
      default:
        throw new BSONParsingException("Unknown bson node type(%s)", bsonType.name());
    }
  }

  public static void write (BsonWriter writer, JsonNode jsonNode) {

    switch (jsonNode.getNodeType()) {
      case OBJECT:
        writer.writeStartDocument();

        for (Map.Entry<String, JsonNode> propertyEntry : jsonNode.properties()) {
          writer.writeName(propertyEntry.getKey());
          write(writer, propertyEntry.getValue());
        }

        writer.writeEndDocument();
        break;
      case ARRAY:
        writer.writeStartArray();

        for (JsonNode elementNode : jsonNode) {
          write(writer, elementNode);
        }

        writer.writeEndArray();
        break;
      case NULL:
        writer.writeNull();
        break;
      case NUMBER:
        if (jsonNode.isDouble() || jsonNode.isFloat()) {
          writer.writeDouble(jsonNode.asDouble());
        } else if (jsonNode.isLong()) {
          writer.writeInt64(jsonNode.asLong());
        } else {
          writer.writeInt32(jsonNode.asInt());
        }
        break;
      case BOOLEAN:
        writer.writeBoolean(jsonNode.booleanValue());
        break;
      case STRING:
        writer.writeString(jsonNode.textValue());
        break;
      default:
        throw new BSONParsingException("Unknown json node type(%s)", jsonNode.getNodeType().name());
    }
  }
}
