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

/**
 * Primary entry point for Throng-based MongoDB access that wires together codec registration, optional automatic
 * index creation, and typed CRUD operations for annotated entity classes.
 *
 * <p><b>Known limitations:</b>
 * <ul>
 *   <li>Entity classes ({@code @Entity}) cannot themselves be marked as polymorphic.</li>
 *   <li>Embedded classes ({@code @Embedded}) cannot declare lifecycle methods.</li>
 *   <li>Generic containers ({@code List}, {@code Map}, etc.) whose element type is an {@code @Embedded}
 *       or {@code @Codec} type are not fully supported: {@code @Embedded} containers will miss automated
 *       index processing, and {@code @Codec} containers will throw an exception due to a type mismatch.
 *       The recommended workaround is to create a typed container subclass with a parameterized codec,
 *       and, for {@code @Embedded} element types, declare the required indexes on the parent entity.</li>
 * </ul>
 */
public class ThrongClient {

  private final MongoDatabase mongoDatabase;
  private final CodecRegistry codecRegistry;
  private final HashMap<Class<?>, ThrongEntityCodec<?>> entityCodecMap = new HashMap<>();

  /**
   * Constructs a client for the given database, scanning the supplied classes to register embedded codecs and
   * entity codecs, and optionally creating collection indexes.
   *
   * @param mongoClient   the underlying MongoDB driver client
   * @param database      the name of the database to operate on
   * @param options       runtime options governing null storage, index creation, and collation inclusion
   * @param entityClasses the entity ({@code @Entity}) and embedded ({@code @Embedded}) classes to register
   * @throws ThrongMappingException    if any entity or embedded annotation is invalid or a codec cannot be resolved
   * @throws NoSuchMethodException     if reflective construction of a codec fails due to a missing constructor
   * @throws InstantiationException    if a codec class cannot be instantiated
   * @throws IllegalAccessException    if reflection cannot access a required constructor or field
   * @throws InvocationTargetException if a constructor invoked during codec setup throws an exception
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
   * Retrieves the registered {@link ThrongEntityCodec} for the given entity class.
   *
   * @param entityClass the entity class whose codec is required
   * @param <T>         the entity type
   * @return the codec registered for the class
   * @throws ThrongRuntimeException if no codec has been registered for the class
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
   * Counts all documents in the entity's collection that satisfy the given filter.
   *
   * @param entityClass the entity class identifying the collection
   * @param filter      the filter to apply to the count
   * @param <T>         the entity type
   * @return the number of matching documents
   */
  public <T> long count (Class<T> entityClass, Filter filter) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).countDocuments(filter.toBsonDocument(BsonDocument.class, codecRegistry));
  }

  /**
   * Executes a find query against the entity's collection and returns a lazy iterable of decoded entities.
   *
   * @param entityClass the entity class identifying the collection
   * @param query       the query carrying filter, sort, projection, skip, limit, and batch-size settings
   * @param <T>         the entity type
   * @return a {@link ThrongIterable} over the matching entity instances
   */
  public <T> ThrongIterable<T> find (Class<T> entityClass, Query query) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return new ThrongIterable<>(query.apply(mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).find(), BsonDocument.class, codecRegistry), entityCodec);
  }

  /**
   * Returns the first entity that matches the query, or {@code null} if there are no matches.
   *
   * @param entityClass the entity class identifying the collection
   * @param query       the query to apply
   * @param <T>         the entity type
   * @return the first matching entity, or {@code null}
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
   * Inserts a single entity into its mapped collection.
   *
   * @param value   the entity instance to insert
   * @param options driver options for the insert operation such as bypassing document validation
   * @param <T>     the entity type
   * @return the driver {@link InsertOneResult} describing the outcome
   */
  public <T> InsertOneResult insert (T value, InsertOneOptions options) {

    ThrongEntityCodec<T> entityCodec = getCodec((Class<T>)value.getClass());

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).insertOne(new ThrongDocument(TranslationUtility.toBson(value, entityCodec)));
  }

  /**
   * Applies an update to all documents in the entity's collection that match the given filter.
   *
   * @param entityClass   the entity class identifying the collection
   * @param filter        the filter that selects documents to update
   * @param updates       the update operations to apply
   * @param updateOptions driver options such as upsert behaviour
   * @param <T>           the entity type
   * @return an {@link UpdateResult} describing the number of matched, modified, and upserted documents
   */
  public <T> UpdateResult update (Class<T> entityClass, Filter filter, Updates updates, UpdateOptions updateOptions) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return new UpdateResult(mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).updateMany(filter.toBsonDocument(BsonDocument.class, codecRegistry), updates.toBsonDocument(BsonDocument.class, codecRegistry), updateOptions));
  }

  /**
   * Deletes all documents in the entity's collection that match the given filter.
   *
   * @param entityClass   the entity class identifying the collection
   * @param filter        the filter that selects documents to delete
   * @param deleteOptions driver options for the delete operation
   * @param <T>           the entity type
   * @return the driver {@link DeleteResult} describing the number of deleted documents
   */
  public <T> DeleteResult delete (Class<T> entityClass, Filter filter, DeleteOptions deleteOptions) {

    ThrongEntityCodec<T> entityCodec = getCodec(entityClass);

    return mongoDatabase.getCollection(entityCodec.getCollection()).withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class).deleteMany(filter.toBsonDocument(BsonDocument.class, codecRegistry), deleteOptions);
  }
}
