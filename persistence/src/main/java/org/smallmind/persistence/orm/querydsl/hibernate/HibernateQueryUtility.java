/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence.orm.querydsl.hibernate;

import java.util.LinkedList;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.hibernate.HibernateQuery;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.orm.ORMOperationException;
import org.smallmind.persistence.query.Sort;
import org.smallmind.persistence.query.SortField;
import org.smallmind.persistence.query.Where;
import org.smallmind.persistence.query.WhereConjunction;
import org.smallmind.persistence.query.WhereCriterion;
import org.smallmind.persistence.query.WhereField;
import org.smallmind.persistence.query.WhereOperandTransformer;

public class HibernateQueryUtility {

  public static HibernateQuery<?> apply (HibernateQuery<?> query, Where where) {

    return apply(query, where, (type) -> {

      throw new ORMOperationException("Translation of enum(%s) requires an implementation of a WhereOperandTransformer", type);
    });
  }

  public static HibernateQuery<?> apply (HibernateQuery<?> query, Where where, WhereOperandTransformer transformer) {

    if (where != null) {

      Predicate walkedPredicate;

      if ((walkedPredicate = walkConjunction(where.getRootConjunction(), transformer)) != null) {

        return query.where(walkedPredicate);
      }
    }

    return query;
  }

  private static Predicate walkConjunction (WhereConjunction whereConjunction, WhereOperandTransformer transformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Expression<Boolean>> expressionList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {
      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:

          Expression<Boolean> walkedExpression;

          if ((walkedExpression = walkConjunction((WhereConjunction)whereCriterion, transformer)) != null) {
            expressionList.add(walkedExpression);
          }
          break;
        case FIELD:
          expressionList.add(walkField((WhereField)whereCriterion, transformer));
          break;
        default:
          throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
      }
    }

    if (expressionList.isEmpty()) {

      return null;
    } else {

      Expression[] expressions;

      expressions = new Expression[expressionList.size()];
      expressionList.toArray(expressions);

      switch (whereConjunction.getConjunctionType()) {
        case AND:
          return Expressions.predicate(Ops.AND, expressions);
        case OR:
          return Expressions.predicate(Ops.OR, expressions);
        default:
          throw new UnknownSwitchCaseException(whereConjunction.getConjunctionType().name());
      }
    }
  }

  private static Expression<Boolean> walkField (WhereField whereField, WhereOperandTransformer transformer) {

    switch (whereField.getOperator()) {
      case LT:
        return Expressions.predicate(Ops.LT, Expressions.path(String.class, whereField.getName()), Expressions.constant(whereField.getOperand().extract(transformer)));
      case LE:
        return Expressions.predicate(Ops.LOE, Expressions.path(String.class, whereField.getName()), Expressions.constant(whereField.getOperand().extract(transformer)));
      case EQ:

        Object equalValue;

        if ((equalValue = whereField.getOperand().extract(transformer)) == null) {
          return Expressions.predicate(Ops.IS_NULL, Expressions.path(String.class, whereField.getName()));
        } else {
          return Expressions.predicate(Ops.EQ, Expressions.path(String.class, whereField.getName()), Expressions.constant(equalValue));
        }
      case NE:

        Object notEqualValue;

        if ((notEqualValue = whereField.getOperand().extract(transformer)) == null) {
          return Expressions.predicate(Ops.IS_NOT_NULL, Expressions.path(String.class, whereField.getName()));
        } else {
          return Expressions.predicate(Ops.NE, Expressions.path(String.class, whereField.getName()), Expressions.constant(notEqualValue));
        }
      case GE:
        return Expressions.predicate(Ops.GOE, Expressions.path(String.class, whereField.getName()), Expressions.constant(whereField.getOperand().extract(transformer)));
      case GT:
        return Expressions.predicate(Ops.GT, Expressions.path(String.class, whereField.getName()), Expressions.constant(whereField.getOperand().extract(transformer)));
      case LIKE:
        return Expressions.predicate(Ops.LIKE, Expressions.path(String.class, whereField.getName()), Expressions.constant(whereField.getOperand().extract(transformer)));
      case UNLIKE:
        return Expressions.predicate(Ops.NOT, Expressions.predicate(Ops.LIKE, Expressions.path(String.class, whereField.getName()), Expressions.constant(whereField.getOperand().extract(transformer))));
      case IN:
        return Expressions.predicate(Ops.IN, Expressions.path(String.class, whereField.getName()), Expressions.constant(whereField.getOperand().extract(transformer)));
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static Criteria apply (Criteria criteria, Sort sort) {

    if ((sort != null) && (!sort.isEmpty())) {
      for (SortField sortField : sort.getFields()) {
        switch (sortField.getDirection()) {
          case ASC:
            criteria.addOrder(Order.asc(sortField.getName()));
            break;
          case DESC:
            criteria.addOrder(Order.desc(sortField.getName()));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }
    }

    return criteria;
  }
}
