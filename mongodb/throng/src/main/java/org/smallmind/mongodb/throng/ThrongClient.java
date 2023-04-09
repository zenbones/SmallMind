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
package org.smallmind.mongodb.throng;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.codec.ArrayCodecProvider;
import org.smallmind.mongodb.throng.index.IndexUtility;
import org.smallmind.mongodb.throng.mapping.EmbeddedReferences;
import org.smallmind.mongodb.throng.mapping.ThrongEmbeddedUtility;
import org.smallmind.mongodb.throng.mapping.ThrongEntity;
import org.smallmind.mongodb.throng.mapping.ThrongEntityCodec;
import org.smallmind.mongodb.throng.mapping.annotation.Embedded;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Query;
import org.smallmind.mongodb.throng.query.Updates;

/*
  Weaknesses...
    1) Entity classes can not be marked as polymorphic.
    2) Embedded classes can not have lifecycle methods.
    3) There's no fully correct automated handling for containers with generics of either @Embedded or @Codec types, i.e. List, Map, Bag, etc.
       Containers of @Embedded types will miss automated index processing, and containers of @Codec types will throw an exception due to mismatch
       of the field and codec. Creating container subclasses, with codecs parameterized for those subclasses, and, in the case of @Embedded types,
       adding the appropriate indexes to the parent class, is the correct route.
*/
public class ThrongClient {

  private final MongoDatabase mongoDatabase;
  private final CodecRegistry codecRegistry;
  private final HashMap<Class<?>, ThrongEntityCodec<?>> entityCodecMap = new HashMap<>();

  public ThrongClient (MongoClient mongoClient, String database, ThrongOptions options, Class<?>... entityClasses)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    CodecRegistry driverCodecRegistry;
    CodecRegistry embeddecCodecRegistry;
    EmbeddedReferences embeddedReferences = new EmbeddedReferences();

    mongoDatabase = mongoClient.getDatabase(database);
    driverCodecRegistry = CodecRegistries.fromRegistries(mongoDatabase.getCodecRegistry(), CodecRegistries.fromProviders(new ArrayCodecProvider(options.isStoreNulls())));

    if (entityClasses != null) {
      for (Class<?> entityClass : entityClasses) {

        Embedded embedded;

        if ((embedded = entityClass.getAnnotation(Embedded.class)) != null) {
          ThrongEmbeddedUtility.generateEmbeddedCodec(entityClass, embedded, driverCodecRegistry, embeddedReferences, options.isStoreNulls());
        }
      }

      embeddecCodecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new LinkedList<>(embeddedReferences.values())));

      for (Class<?> entityClass : entityClasses) {

        Entity entity;

        if ((entity = entityClass.getAnnotation(Entity.class)) != null) {

          ThrongEntityCodec<?> entityCodec;

          entityCodecMap.put(entityClass, entityCodec = new ThrongEntityCodec<>(new ThrongEntity<>(entityClass, entity, CodecRegistries.fromRegistries(embeddecCodecRegistry, driverCodecRegistry), embeddedReferences, options.isStoreNulls())));
          if (options.isCreateIndexes()) {
            IndexUtility.createIndex(mongoDatabase.getCollection(entityCodec.getCollection()), entityCodec.provideIndexes());
          }
        }
      }
    }

    codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new ThrongDocumentCodec()), driverCodecRegistry);
  }

  private <T> ThrongEntityCodec<T> getCodec (Class<T> entityClass) {

    ThrongEntityCodec<T> entityCodec;

    if ((entityCodec = (ThrongEntityCodec<T>)entityCodecMap.get(entityClass)) == null) {
      throw new ThrongRuntimeException("Unmapped entity type(%s)", entityClass);
    } else {

      return entityCodec;
    }
  }

  public <T> Iterable<T> find (Class<T> entityClass, Query query) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return new ThrongIterable<>(query.apply(mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).find(), BsonDocument.class, codecRegistry), entityCodec);
  }

  public <T> T findOne (Class<T> entityClass, Filter filter) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);
    FindIterable<ThrongDocument> findIterable = mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).find(filter.toBsonDocument(BsonDocument.class, codecRegistry)).limit(1);
    ThrongDocument throngDocument;

    if ((throngDocument = findIterable.first()) == null) {

      return null;
    } else {

      return TranslationUtility.fromBson(entityCodec, throngDocument.getBsonDocument());
    }
  }

  public <T> InsertOneResult insert (T value, InsertOneOptions options) {

    ThrongEntityCodec<T> entityCodec = getCodec((Class<T>)value.getClass());

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).insertOne(new ThrongDocument(TranslationUtility.toBson(value, entityCodec)));
  }

  public <T> UpdateResult update (Class<T> entityClass, Filter filter, Updates updates, UpdateOptions updateOptions) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).updateMany(filter.toBsonDocument(BsonDocument.class, codecRegistry), updates.toBsonDocument(BsonDocument.class, codecRegistry), updateOptions);
  }

  public <T> DeleteResult delete (Class<T> entityClass, Filter filter, DeleteOptions deleteOptions) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).deleteMany(filter.toBsonDocument(BsonDocument.class, codecRegistry), deleteOptions);
  }
}
