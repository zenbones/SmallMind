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
package org.smallmind.persistence.orm.querydsl.jpa;

import com.querydsl.jpa.impl.JPAQuery;

/**
 * Encapsulates a QueryDSL JPA query definition with an optional entity graph hint.
 *
 * @param <T> result type
 */
public abstract class JPAQueryDetails<T> {

  private String graph;

  /**
   * Creates query details without specifying an entity graph.
   */
  public JPAQueryDetails () {

  }

  /**
   * Creates query details with an entity graph to apply.
   *
   * @param graph JPA entity graph name
   */
  public JPAQueryDetails (String graph) {

    this.graph = graph;
  }

  /**
   * @return the configured entity graph name, or {@code null} if none
   */
  public String getGraph () {

    return graph;
  }

  /**
   * Applies predicates, projections, and hints to the supplied QueryDSL query.
   *
   * @param query base query
   * @return the completed query ready to execute
   */
  public abstract JPAQuery<T> completeQuery (JPAQuery<T> query);
}
