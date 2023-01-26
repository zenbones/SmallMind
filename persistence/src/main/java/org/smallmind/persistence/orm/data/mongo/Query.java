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

  public Query and (Criteria criteria) {

    if (criteria != null) {
      criteriaList.add(criteria);
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
