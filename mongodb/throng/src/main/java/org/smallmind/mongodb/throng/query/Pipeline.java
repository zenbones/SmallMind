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
package org.smallmind.mongodb.throng.query;

import java.util.LinkedList;
import java.util.List;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * Fluent builder for MongoDB aggregation pipelines. Stages are accumulated in order and converted to a
 * driver-compatible {@code List<Bson>} on demand. Where a stage takes a {@link Filter}, {@link Sort}, or
 * {@link Projections} argument, the inner DSL object is wrapped so that BSON serialisation is deferred
 * until the driver provides its codec registry.
 *
 * <p>Pipeline construction is single-use: do not share a {@link Pipeline} across threads or reuse one
 * across multiple {@code aggregate} calls.
 */
public class Pipeline {

  private final LinkedList<Bson> stages = new LinkedList<>();

  /**
   * Creates a new empty pipeline builder.
   *
   * @return a fresh {@link Pipeline} instance
   */
  public static Pipeline begin () {

    return new Pipeline();
  }

  /**
   * Appends a {@code $match} stage that filters documents by the supplied filter.
   *
   * @param filter filter to apply
   * @return this builder for chaining
   */
  public Pipeline match (Filter filter) {

    stages.add(Aggregates.match(asDeferredBson(filter)));

    return this;
  }

  /**
   * Appends a {@code $sort} stage that orders documents by the supplied sort specification.
   *
   * @param sort sort specification
   * @return this builder for chaining
   */
  public Pipeline sort (Sort sort) {

    stages.add(Aggregates.sort(asDeferredBson(sort)));

    return this;
  }

  /**
   * Appends a {@code $project} stage that includes or excludes fields per the supplied projections.
   *
   * @param projections projection specification
   * @return this builder for chaining
   */
  public Pipeline project (Projections projections) {

    stages.add(Aggregates.project(asDeferredBson(projections)));

    return this;
  }

  /**
   * Appends a {@code $limit} stage that caps the number of documents passed downstream.
   *
   * @param limit maximum number of documents to retain
   * @return this builder for chaining
   */
  public Pipeline limit (int limit) {

    stages.add(Aggregates.limit(limit));

    return this;
  }

  /**
   * Appends a {@code $skip} stage that discards the leading documents.
   *
   * @param skip number of documents to skip
   * @return this builder for chaining
   */
  public Pipeline skip (long skip) {

    stages.add(Aggregates.skip((int)skip));

    return this;
  }

  /**
   * Appends a {@code $count} stage that emits a single document containing the count under the supplied
   * field name.
   *
   * @param outputFieldName name of the output field carrying the count
   * @return this builder for chaining
   */
  public Pipeline count (String outputFieldName) {

    stages.add(Aggregates.count(outputFieldName));

    return this;
  }

  /**
   * Appends an {@code $unwind} stage that flattens the array value at the supplied field path.
   *
   * @param fieldPath dotted field path prefixed with {@code $}, e.g. {@code "$tags"}
   * @return this builder for chaining
   */
  public Pipeline unwind (String fieldPath) {

    stages.add(Aggregates.unwind(fieldPath));

    return this;
  }

  /**
   * Appends a {@code $lookup} stage that performs a left outer join into the named collection.
   *
   * @param from         collection on the right side of the join
   * @param localField   field on the current document used for matching
   * @param foreignField field on the {@code from} collection used for matching
   * @param as           name of the array field that holds the joined documents
   * @return this builder for chaining
   */
  public Pipeline lookup (String from, String localField, String foreignField, String as) {

    stages.add(Aggregates.lookup(from, localField, foreignField, as));

    return this;
  }

  /**
   * Appends a {@code $group} stage that groups documents by the given id expression and computes the
   * supplied accumulator fields. Use {@link com.mongodb.client.model.Accumulators} factories to build
   * each {@link BsonField} (for example {@code Accumulators.sum("total", "$amount")}).
   *
   * @param idExpression  the group key expression; {@code null} groups everything into a single bucket
   * @param accumulators  accumulator fields produced by {@link com.mongodb.client.model.Accumulators}
   * @return this builder for chaining
   */
  public Pipeline group (Object idExpression, BsonField... accumulators) {

    stages.add(Aggregates.group(idExpression, accumulators));

    return this;
  }

  /**
   * Appends a raw pipeline stage. Use this as an escape hatch for stages that are not directly supported
   * by the fluent surface (for example {@code $facet} or {@code $bucket}); the supplied {@link Bson} is
   * added verbatim.
   *
   * @param stage raw stage document
   * @return this builder for chaining
   */
  public Pipeline stage (Bson stage) {

    stages.add(stage);

    return this;
  }

  /**
   * Returns the ordered list of pipeline stages as the driver expects.
   *
   * @return list of pipeline stages
   */
  public List<Bson> toBsonList () {

    return stages;
  }

  private static Bson asDeferredBson (Filter filter) {

    return new Bson() {

      @Override
      public <TDocument> BsonDocument toBsonDocument (Class<TDocument> documentClass, CodecRegistry codecRegistry) {

        return (BsonDocument)filter.toBsonDocument(documentClass, codecRegistry);
      }
    };
  }

  private static Bson asDeferredBson (Sort sort) {

    return new Bson() {

      @Override
      public <TDocument> BsonDocument toBsonDocument (Class<TDocument> documentClass, CodecRegistry codecRegistry) {

        return (BsonDocument)sort.toBsonDocument(documentClass, codecRegistry);
      }
    };
  }

  private static Bson asDeferredBson (Projections projections) {

    return new Bson() {

      @Override
      public <TDocument> BsonDocument toBsonDocument (Class<TDocument> documentClass, CodecRegistry codecRegistry) {

        return (BsonDocument)projections.toBsonDocument(documentClass, codecRegistry);
      }
    };
  }
}
