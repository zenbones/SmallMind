/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import org.bson.BsonDocument;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ThrongDocumentCodec implements Codec<ThrongDocument> {

  @Override
  public Class<ThrongDocument> getEncoderClass () {

    return ThrongDocument.class;
  }

  @Override
  public ThrongDocument decode (BsonReader reader, DecoderContext decoderContext) {

    BsonValue bsonValue = BsonUtility.read(reader);

    if (!BsonType.DOCUMENT.equals(bsonValue.getBsonType())) {
      throw new DocumentParsingException("The bson node is not a document");
    } else {

      return new ThrongDocument((BsonDocument)bsonValue);
    }
  }

  @Override
  public void encode (BsonWriter writer, ThrongDocument document, EncoderContext encoderContext)
    throws DocumentParsingException {

    BsonUtility.write(writer, document.getBsonDocument());
  }
}
