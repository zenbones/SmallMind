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

import com.mongodb.client.FindIterable;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.ThrongDocument;

/**
 * Fluent builder for configuring MongoDB find queries including filters, sorting, projections, and paging.
 */
public class Query {

  private Filter filter;
  private Sort sort;
  private Projections projections;
  private long skip = 0;
  private int limit = 0;
  private int batchSize = 0;

  /**
   * @return new query builder
   */
  public static Query with () {

    return new Query();
  }

  /**
   * @return query with an empty filter
   */
  public static Query empty () {

    return new Query().filter(Filter.empty());
  }

  /**
   * Applies a filter to the query.
   *
   * @param filter filter to apply
   * @return this builder for chaining
   */
  public Query filter (Filter filter) {

    this.filter = filter;

    return this;
  }

  /**
   * Applies a sort definition.
   *
   * @param sort sort specification
   * @return this builder for chaining
   */
  public Query sort (Sort sort) {

    this.sort = sort;

    return this;
  }

  /**
   * Applies projection definitions.
   *
   * @param projections projection builder
   * @return this builder for chaining
   */
  public Query projection (Projections projections) {

    this.projections = projections;

    return this;
  }

  /**
   * Configures how many documents to skip.
   *
   * @param skip number of documents to skip
   * @return this builder for chaining
   */
  public Query skip (long skip) {

    this.skip = skip;

    return this;
  }

  /**
   * Sets the maximum number of documents to return.
   *
   * @param limit maximum documents
   * @return this builder for chaining
   */
  public Query limit (int limit) {

    this.limit = limit;

    return this;
  }

  /**
   * Sets the batch size for the cursor.
   *
   * @param batchSize batch size
   * @return this builder for chaining
   */
  public Query batchSize (int batchSize) {

    this.batchSize = batchSize;

    return this;
  }

  /**
   * Applies the configured query options to the driver iterable.
   *
   * @param findIterable  driver find iterable
   * @param documentClass document class used for BSON conversion
   * @param codecRegistry codec registry used by the driver
   * @param <T>           document type
   * @return updated iterable with query options applied
   */
  public <T> FindIterable<ThrongDocument> apply (FindIterable<ThrongDocument> findIterable, Class<T> documentClass, CodecRegistry codecRegistry) {

    if (filter != null) {
      findIterable.filter(filter.toBsonDocument(documentClass, codecRegistry));
    }
    if (sort != null) {
      findIterable.sort(sort.toBsonDocument(documentClass, codecRegistry));
    }
    if (projections != null) {
      findIterable.projection(projections.toBsonDocument(documentClass, codecRegistry));
    }
    if (skip > 0) {
      //TODO: Mongo should be allowing long here, and maybe someday they will...
      findIterable.skip((int)skip);
    }
    if (limit > 0) {
      findIterable.limit(limit);
    }
    if (batchSize > 0) {
      findIterable.batchSize(batchSize);
    }

    return findIterable;
  }
}
