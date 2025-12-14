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

/**
 * High-level convenience wrapper for Throng-mapped entities that wires codecs, index creation, and CRUD helpers.
 */
public class ThrongClient {

  private final MongoDatabase mongoDatabase;
  private final CodecRegistry codecRegistry;
  private final HashMap<Class<?>, ThrongEntityCodec<?>> entityCodecMap = new HashMap<>();

  /**
   * Builds a client for the given database, registering codecs and optionally creating indexes for the provided entity classes.
   *
   * @param mongoClient   the underlying MongoDB client
   * @param database      the database name
   * @param options       configurable behaviors such as index creation and null storage
   * @param entityClasses the entity and embedded classes to register
   * @throws ThrongMappingException    if entity metadata is invalid
   * @throws NoSuchMethodException     if an entity lacks an expected constructor
   * @throws InstantiationException    if an entity cannot be instantiated
   * @throws IllegalAccessException    if reflection cannot access an entity
   * @throws InvocationTargetException if construction of an entity fails
   */
  public ThrongClient (MongoClient mongoClient, String database, ThrongOptions options, Class<?>... entityClasses)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    CodecRegistry driverCodecRegistry;

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

      for (Class<?> entityClass : entityClasses) {

        Entity entity;

        if ((entity = entityClass.getAnnotation(Entity.class)) != null) {

          ThrongEntityCodec<?> entityCodec;

          entityCodecMap.put(entityClass, entityCodec = new ThrongEntityCodec<>(new ThrongEntity<>(entityClass, entity, CodecRegistries.fromRegistries(CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new LinkedList<>(embeddedReferences.values()))), driverCodecRegistry), embeddedReferences, options.isStoreNulls())));
          if (options.isCreateIndexes()) {
            IndexUtility.createIndex(mongoDatabase.getCollection(entityCodec.getCollection()), entityCodec.provideIndexes(), options.isIncludeCollation());
          }
        }
      }
    }

    codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new ThrongDocumentCodec()), CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new LinkedList<>(embeddedReferences.values()))), driverCodecRegistry);
  }

  /**
   * Looks up the {@link ThrongEntityCodec} registered for the provided entity type.
   *
   * @param entityClass the entity type
   * @param <T>         entity type
   * @return the codec configured for the entity
   * @throws ThrongRuntimeException if the entity is not registered with the client
   */
  private <T> ThrongEntityCodec<T> getCodec (Class<T> entityClass) {

    ThrongEntityCodec<T> entityCodec;

    if ((entityCodec = (ThrongEntityCodec<T>)entityCodecMap.get(entityClass)) == null) {
      throw new ThrongRuntimeException("Unmapped entity type(%s)", entityClass);
    } else {

      return entityCodec;
    }
  }

  /**
   * Counts documents for the entity type matching the provided filter.
   *
   * @param entityClass the entity type
   * @param filter      the filter to apply
   * @param <T>         entity type
   * @return number of matching documents
   */
  public <T> long count (Class<T> entityClass, Filter filter) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).countDocuments(filter.toBsonDocument(BsonDocument.class, codecRegistry));
  }

  /**
   * Executes a find query for the entity type.
   *
   * @param entityClass the entity type
   * @param query       query options such as filters, projections, and sorting
   * @param <T>         entity type
   * @return iterable over decoded entity instances
   */
  public <T> ThrongIterable<T> find (Class<T> entityClass, Query query) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return new ThrongIterable<>(query.apply(mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).find(), BsonDocument.class, codecRegistry), entityCodec);
  }

  /**
   * Fetches the first document that matches the provided query.
   *
   * @param entityClass the entity type
   * @param query       query options to apply
   * @param <T>         entity type
   * @return the first matching entity or {@code null} if none are found
   */
  public <T> T findOne (Class<T> entityClass, Query query) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);
    FindIterable<ThrongDocument> findIterable = query.apply(mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).find(), BsonDocument.class, codecRegistry).limit(1);
    ThrongDocument throngDocument;

    if ((throngDocument = findIterable.first()) == null) {

      return null;
    } else {

      return TranslationUtility.fromBson(entityCodec, throngDocument.getBsonDocument());
    }
  }

  /**
   * Inserts a new entity using the configured codecs and collection.
   *
   * @param value   the entity instance to insert
   * @param options insert options such as bypassing validation
   * @param <T>     entity type
   * @return insert result produced by the driver
   */
  public <T> InsertOneResult insert (T value, InsertOneOptions options) {

    ThrongEntityCodec<T> entityCodec = getCodec((Class<T>)value.getClass());

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).insertOne(new ThrongDocument(TranslationUtility.toBson(value, entityCodec)));
  }

  /**
   * Applies updates to documents matching the filter.
   *
   * @param entityClass   the entity type
   * @param filter        selector for documents to update
   * @param updates       the update operations to apply
   * @param updateOptions driver update options (upsert, etc.)
   * @param <T>           entity type
   * @return wrapper around the driver's update result
   */
  public <T> UpdateResult update (Class<T> entityClass, Filter filter, Updates updates, UpdateOptions updateOptions) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return new UpdateResult(mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).updateMany(filter.toBsonDocument(BsonDocument.class, codecRegistry), updates.toBsonDocument(BsonDocument.class, codecRegistry), updateOptions));
  }

  /**
   * Deletes documents matching the provided filter.
   *
   * @param entityClass   the entity type
   * @param filter        selector for documents to delete
   * @param deleteOptions driver delete options
   * @param <T>           entity type
   * @return driver delete result
   */
  public <T> DeleteResult delete (Class<T> entityClass, Filter filter, DeleteOptions deleteOptions) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).deleteMany(filter.toBsonDocument(BsonDocument.class, codecRegistry), deleteOptions);
  }
}
