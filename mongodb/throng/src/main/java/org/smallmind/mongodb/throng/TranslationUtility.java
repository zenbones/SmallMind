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

    instance = entityCodec.decode(new BsonDocumentReader(bsonDocument), DecoderContext.builder().build());

    if (entityCodec.getLifecycle() != null) {
      entityCodec.getLifecycle().executePostLoad(instance);
    }

    return instance;
  }

  public static <T> BsonDocument toBson (T entity, ThrongEntityCodec<T> entityCodec) {

    BsonDocument bsonDocument;

    if (entityCodec.getLifecycle() != null) {
      entityCodec.getLifecycle().executePrePersist(entity);
    }

    entityCodec.encode(new BsonDocumentWriter(bsonDocument = new BsonDocument()), entity, EncoderContext.builder().build());

    if (entityCodec.getLifecycle() != null) {
      entityCodec.getLifecycle().executePostPersist(entity, bsonDocument);
    }

    return bsonDocument;
  }
}
