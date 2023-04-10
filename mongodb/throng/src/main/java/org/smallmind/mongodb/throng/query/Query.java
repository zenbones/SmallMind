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
package org.smallmind.mongodb.throng.query;

import com.mongodb.client.FindIterable;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.ThrongDocument;

public class Query {

  private Filter filter;
  private Sort sort;
  private Projection projection;
  private int skip = 0;
  private int limit = 0;
  private int batchSize = 0;

  public static Query with () {

    return new Query();
  }

  public static Query empty () {

    return new Query().filter(Filter.empty());
  }

  public Query filter (Filter filter) {

    this.filter = filter;

    return this;
  }

  public Query sort (Sort sort) {

    this.sort = sort;

    return this;
  }

  public Query projection (Projection projection) {

    this.projection = projection;

    return this;
  }

  public Query skip (int skip) {

    this.skip = skip;

    return this;
  }

  public Query limit (int limit) {

    this.limit = limit;

    return this;
  }

  public Query batchSize (int batchSize) {

    this.batchSize = batchSize;

    return this;
  }

  public <T> FindIterable<ThrongDocument> apply (FindIterable<ThrongDocument> findIterable, Class<T> documentClass, CodecRegistry codecRegistry) {

    if (filter != null) {
      findIterable.filter(filter.toBsonDocument(documentClass, codecRegistry));
    }
    if (sort != null) {
      findIterable.sort(sort.toBsonDocument(documentClass, codecRegistry));
    }
    if (projection != null) {
      findIterable.projection(projection.toBsonDocument(documentClass, codecRegistry));
    }
    if (skip > 0) {
      findIterable.skip(skip);
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
