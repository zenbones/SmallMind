/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.persistence.orm.querydsl;

import java.util.HashSet;
import java.util.Set;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import org.smallmind.persistence.query.Product;
import org.smallmind.persistence.query.Sort;
import org.smallmind.persistence.query.Where;
import org.smallmind.persistence.query.WhereFieldTransformer;
import org.smallmind.persistence.query.WhereOperandTransformer;

public class QBuilder<T> {

  private WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer;
  private WhereOperandTransformer operandTransformer = WhereOperandTransformer.instance();
  private Where[] wheres;
  private Sort sort;
  private QJoins joins;

  public QBuilder (WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer, Where... wheres) {

    this.fieldTransformer = fieldTransformer;
    this.wheres = wheres;
  }

  public static <T> QBuilder<T> instance (WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer, Where... wheres) {

    return new QBuilder<>(fieldTransformer, wheres);
  }

  public QBuilder<T> operandTransformer (WhereOperandTransformer operandTransformer) {

    this.operandTransformer = operandTransformer;

    return this;
  }

  public QBuilder<T> sort (Sort sort) {

    this.sort = sort;

    return this;
  }

  public QBuilder<T> joins (QJoin... series) {

    joins = new QJoins(series);

    return this;
  }

  public JPAQuery<T> update (JPAQuery<T> query) {

    Product<EntityPath<?>, OrderSpecifier[]> orderProduct;
    Set<EntityPath<?>> rootSet = new HashSet<>();

    if (wheres != null) {
      for (Where where : wheres) {

        Product<EntityPath<?>, Predicate> predicateProduct;

        if (!(predicateProduct = QUtility.apply(where, fieldTransformer, operandTransformer)).isEmpty()) {
          rootSet.addAll(predicateProduct.getRootSet());
          query.where(predicateProduct.getValue());
        }
      }
    }

    if (!(orderProduct = QUtility.apply(sort, fieldTransformer)).isEmpty()) {
      rootSet.addAll(orderProduct.getRootSet());
      query.orderBy(orderProduct.getValue());
    }

    for (EntityPath<?> root : rootSet) {
      if (joins != null) {
        joins.use(root);
      }
    }

    if (joins != null) {
      joins.update(query);
    }

    return query;
  }
}
