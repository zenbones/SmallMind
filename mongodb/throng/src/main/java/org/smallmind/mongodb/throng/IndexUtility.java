package org.smallmind.mongodb.throng;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.smallmind.mongodb.throng.annotation.Index;
import org.smallmind.mongodb.throng.annotation.IndexOptions;
import org.smallmind.mongodb.throng.annotation.Indexed;
import org.smallmind.mongodb.throng.annotation.Indexes;

public class IndexUtility {

  public static void createIndex (MongoCollection<?> mongoCollection, Indexed indexed, String field) {

    mongoCollection.createIndex(indexed.value().construct(field), generateIndexOptions(indexed.options()));
  }

  public static void createIndex (MongoCollection<?> mongoCollection, Indexes indexes) {

    LinkedList<Bson> indexList = new LinkedList<>();

    for (Index index : indexes.value()) {
      indexList.add(index.type().construct(index.value()));
    }

    mongoCollection.createIndex(com.mongodb.client.model.Indexes.compoundIndex(indexList), generateIndexOptions(indexes.options()));
  }

  private static com.mongodb.client.model.IndexOptions generateIndexOptions (IndexOptions indexOptionsAnnotation) {

    com.mongodb.client.model.IndexOptions indexOptions = new com.mongodb.client.model.IndexOptions();

    indexOptions.background(indexOptionsAnnotation.background());

    if (indexOptionsAnnotation.bucketSize() > 0) {
      indexOptions.bucketSize(indexOptionsAnnotation.bucketSize());
    }

    if (indexOptionsAnnotation.bits() > 0) {
      indexOptions.bits(indexOptionsAnnotation.bits());
    }

    indexOptions.collation(CollationUtility.generate(indexOptionsAnnotation.collation()));

    if (!indexOptionsAnnotation.defaultLanguage().isEmpty()) {
      indexOptions.defaultLanguage(indexOptionsAnnotation.defaultLanguage());
    }

    if (indexOptionsAnnotation.expireAfterSeconds() > 0) {
      indexOptions.expireAfter(indexOptionsAnnotation.expireAfterSeconds(), TimeUnit.SECONDS);
    }

    indexOptions.hidden(indexOptionsAnnotation.hidden());

    if (!indexOptionsAnnotation.languageOverride().isEmpty()) {
      indexOptions.languageOverride(indexOptionsAnnotation.languageOverride());
    }

    if (indexOptionsAnnotation.max() <= 180) {
      indexOptions.max(indexOptionsAnnotation.max());
    }

    if (indexOptionsAnnotation.min() >= -180) {
      indexOptions.min(indexOptionsAnnotation.min());
    }

    if (!indexOptionsAnnotation.name().isEmpty()) {
      indexOptions.name(indexOptionsAnnotation.name());
    }

    if (!indexOptionsAnnotation.partialFilterExpression().isEmpty()) {
      indexOptions.partialFilterExpression(BasicDBObject.parse(indexOptionsAnnotation.partialFilterExpression()));
    }

    indexOptions.sparse(indexOptionsAnnotation.sparse());

    if (indexOptionsAnnotation.sphereVersion() > 0) {
      indexOptions.sphereVersion(indexOptionsAnnotation.sphereVersion());
    }

    if (!indexOptionsAnnotation.storageEngine().isEmpty()) {
      indexOptions.storageEngine(BasicDBObject.parse(indexOptionsAnnotation.storageEngine()));
    }

    if (indexOptionsAnnotation.textVersion() > 0) {
      indexOptions.textVersion(indexOptionsAnnotation.textVersion());
    }

    indexOptions.unique(indexOptionsAnnotation.unique());

    if (indexOptionsAnnotation.version() > 0) {
      indexOptions.version(indexOptionsAnnotation.version());
    }

    if (!indexOptionsAnnotation.weights().isEmpty()) {
      indexOptions.weights(BasicDBObject.parse(indexOptionsAnnotation.weights()));
    }

    if (!indexOptionsAnnotation.wildcardProjection().isEmpty()) {
      indexOptions.wildcardProjection(BasicDBObject.parse(indexOptionsAnnotation.wildcardProjection()));
    }

    return indexOptions;
  }
}
