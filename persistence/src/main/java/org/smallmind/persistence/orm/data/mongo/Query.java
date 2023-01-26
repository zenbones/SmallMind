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
package org.smallmind.persistence.orm.data.mongo;

import java.util.Arrays;
import java.util.LinkedList;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

public class Query {

  private final LinkedList<Criteria> criteriaList = new LinkedList<>();
  private final LinkedList<Sort> sortList = new LinkedList<>();
  private final LinkedList<String> includeList = new LinkedList<>();
  private long skip = 0;
  private int limit = 0;

  public static And and () {

    return new And();
  }

  public static Or or () {

    return new Or();
  }

  public org.springframework.data.mongodb.core.query.Query as () {

    org.springframework.data.mongodb.core.query.Query query = criteriaList.isEmpty() ? new org.springframework.data.mongodb.core.query.Query() : (criteriaList.size() == 1) ? org.springframework.data.mongodb.core.query.Query.query(criteriaList.getFirst()) : org.springframework.data.mongodb.core.query.Query.query(new Criteria().andOperator(criteriaList));

    if (!sortList.isEmpty()) {
      for (Sort sort : sortList) {
        query.with(sort);
      }
    }

    if (!includeList.isEmpty()) {
      query.fields().include(includeList.toArray(new String[0]));
    }

    if (skip > 0) {
      query.skip(skip);
    }

    if (limit > 0) {
      query.limit(limit);
    }

    return query;
  }

  public Query skip (long skip) {

    this.skip = skip;

    return this;
  }

  public Query limit (int limit) {

    this.limit = limit;

    return this;
  }

  public Query and (Conjunction conjunction) {

    return and(conjunction.as());
  }

  public Query and (Criteria... criterion) {

    if ((criterion != null) && (criterion.length > 0)) {
      if (criterion.length == 1) {
        criteriaList.add(criterion[0]);
      } else {
        criteriaList.addAll(Arrays.asList(criterion));
      }
    }

    return this;
  }

  public Query orderBy (Sort sort) {

    if (sort != null) {
      sortList.add(sort);
    }

    return this;
  }

  public Query project (String... fields) {

    if ((fields != null) && (fields.length > 0)) {
      includeList.addAll(Arrays.asList(fields));
    }

    return this;
  }
}
