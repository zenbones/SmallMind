package org.smallmind.persistence.orm.data.mongo;

import java.util.Arrays;
import java.util.LinkedList;
import org.springframework.data.mongodb.core.query.Criteria;

public class And implements Conjunction {

  private final LinkedList<Criteria> criteriaList = new LinkedList<>();

  protected And () {

  }

  @Override
  public Criteria as () {

    return criteriaList.isEmpty() ? null : (criteriaList.size() == 1) ? criteriaList.getFirst() : new Criteria().andOperator(criteriaList);
  }

  public And add (Criteria... criterion) {

    criteriaList.addAll(Arrays.asList(criterion));

    return this;
  }
}
