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
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.smallmind.mongodb.throng.mapping.ThrongEntityCodec;

public class TranslationUtility {

  public static <T> T fromBson (ThrongEntityCodec<T> entityCodec, BsonDocument bsonDocument) {

    T instance;

    if (entityCodec.getLifecycle() != null) {
      entityCodec.getLifecycle().executePreLoad(entityCodec.getEntityClass(), bsonDocument);
    }

    if (((instance = entityCodec.decode(new BsonDocumentReader(bsonDocument), DecoderContext.builder().build())) != null) && (entityCodec.getLifecycle() != null)) {
      entityCodec.getLifecycle().executePostLoad(instance);
    }

    return instance;
  }

  public static <T> BsonDocument toBson (T entity, ThrongEntityCodec<T> entityCodec) {

    BsonDocument bsonDocument;

    if ((entity != null) && (entityCodec.getLifecycle() != null)) {
      entityCodec.getLifecycle().executePrePersist(entity);
    }

    entityCodec.encode(new BsonDocumentWriter(bsonDocument = new BsonDocument()), entity, EncoderContext.builder().build());

    if ((entity != null) && (entityCodec.getLifecycle() != null)) {
      entityCodec.getLifecycle().executePostPersist(entity, bsonDocument);
    }

    return bsonDocument;
  }
}
