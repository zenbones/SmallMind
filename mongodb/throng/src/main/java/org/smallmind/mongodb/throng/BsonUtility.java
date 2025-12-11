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
package org.smallmind.mongodb.throng;

import java.util.Map;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonTimestamp;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class BsonUtility {

  public static BsonValue read (BsonReader reader) {

    BsonType bsonType;

    switch (bsonType = reader.getCurrentBsonType()) {
      case OBJECT_ID:
        return new BsonObjectId(reader.readObjectId());
      case TIMESTAMP:
        return reader.readTimestamp();
      case DATE_TIME:
        return new BsonDateTime(reader.readDateTime());
      case DOCUMENT:

        BsonDocument bsonDocument = new BsonDocument();

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
          bsonDocument.put(reader.readName(), read(reader));
        }
        reader.readEndDocument();

        return bsonDocument;
      case ARRAY:

        BsonArray bsonArray = new BsonArray();

        reader.readStartArray();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
          bsonArray.add(read(reader));
        }
        reader.readEndArray();

        return bsonArray;
      case NULL:
        reader.readNull();

        return BsonNull.VALUE;
      case STRING:
        return new BsonString(reader.readString());
      case BOOLEAN:
        return new BsonBoolean(reader.readBoolean());
      case DOUBLE:
        return new BsonDouble(reader.readDouble());
      case INT32:
        return new BsonInt32(reader.readInt32());
      case INT64:
        return new BsonInt64(reader.readInt64());
      default:
        throw new DocumentParsingException("Unknown bson node type(%s)", bsonType.name());
    }
  }

  public static void write (BsonWriter writer, BsonValue bsonValue) {

    switch (bsonValue.getBsonType()) {
      case OBJECT_ID:
        writer.writeObjectId(((BsonObjectId)bsonValue).getValue());
        break;
      case TIMESTAMP:
        writer.writeTimestamp((BsonTimestamp)bsonValue);
        break;
      case DATE_TIME:
        writer.writeDateTime(((BsonDateTime)bsonValue).getValue());
        break;
      case DOCUMENT:
        writer.writeStartDocument();

        for (Map.Entry<String, BsonValue> bsonEntry : ((BsonDocument)bsonValue).entrySet()) {
          writer.writeName(bsonEntry.getKey());
          write(writer, bsonEntry.getValue());
        }

        writer.writeEndDocument();
        break;
      case ARRAY:
        writer.writeStartArray();

        for (BsonValue item : new IterableIterator<>(((BsonArray)bsonValue).iterator())) {
          write(writer, item);
        }

        writer.writeEndArray();
        break;
      case NULL:
        writer.writeNull();
        break;
      case INT32:
        writer.writeInt32(((BsonInt32)bsonValue).getValue());
        break;
      case INT64:
        writer.writeInt64(((BsonInt64)bsonValue).getValue());
        break;
      case DOUBLE:
        writer.writeDouble(((BsonDouble)bsonValue).getValue());
        break;
      case BOOLEAN:
        writer.writeBoolean(((BsonBoolean)bsonValue).getValue());
        break;
      case STRING:
        writer.writeString(((BsonString)bsonValue).getValue());
        break;
      default:
        throw new DocumentParsingException("Unknown json node type(%s)", bsonValue.getBsonType().name());
    }
  }
}
