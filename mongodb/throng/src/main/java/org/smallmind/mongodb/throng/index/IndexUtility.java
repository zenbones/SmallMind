/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.mongodb.throng.index;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import org.bson.conversions.Bson;
import org.smallmind.mongodb.throng.index.annotation.IndexOptions;

public class IndexUtility {

  public static void createIndex (MongoCollection<?> mongoCollection, ThrongIndexes throngIndexes, boolean includeCollation) {

    for (IndexedField indexedField : throngIndexes.getIndexedFields()) {
      mongoCollection.createIndex(indexedField.getIndexed().value().construct(indexedField.getField()), generateIndexOptions(indexedField.getIndexed().options(), includeCollation));
    }
    for (CompoundIndex compoundIndex : throngIndexes.getCompoundIndexes()) {

      LinkedList<Bson> bsonList = new LinkedList<>();

      for (IndexedElement indexedElement : compoundIndex.getIndexedElements()) {
        bsonList.add(indexedElement.getIndexType().construct(indexedElement.getField()));
      }

      mongoCollection.createIndex(Indexes.compoundIndex(bsonList), generateIndexOptions(compoundIndex.getIndexOptions(), includeCollation));
    }
  }

  private static com.mongodb.client.model.IndexOptions generateIndexOptions (IndexOptions indexOptionsAnnotation, boolean includeCollation) {

    com.mongodb.client.model.IndexOptions indexOptions = new com.mongodb.client.model.IndexOptions();

    indexOptions.background(indexOptionsAnnotation.background());

    if (indexOptionsAnnotation.bucketSize() > 0) {
      indexOptions.bucketSize(indexOptionsAnnotation.bucketSize());
    }

    if (indexOptionsAnnotation.bits() > 0) {
      indexOptions.bits(indexOptionsAnnotation.bits());
    }

    if (includeCollation) {
      indexOptions.collation(CollationUtility.generate(indexOptionsAnnotation.collation()));
    }

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
