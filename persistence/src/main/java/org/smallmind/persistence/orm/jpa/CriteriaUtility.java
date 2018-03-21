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
package org.smallmind.persistence.orm.jpa;

import java.util.LinkedList;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.query.DefaultWhereOperandTransformer;
import org.smallmind.persistence.query.Sort;
import org.smallmind.persistence.query.SortField;
import org.smallmind.persistence.query.Where;
import org.smallmind.persistence.query.WhereConjunction;
import org.smallmind.persistence.query.WhereCriterion;
import org.smallmind.persistence.query.WhereField;
import org.smallmind.persistence.query.WhereFieldTransformer;
import org.smallmind.persistence.query.WhereOperandTransformer;
import org.smallmind.persistence.query.WherePath;

public class CriteriaUtility {

  private static final WhereFieldTransformer<Path<?>> WHERE_FIELD_TRANSFORMER = new CriteriaWhereFieldTransformer();
  private static final WhereOperandTransformer WHERE_OPERAND_TRANSFORMER = new DefaultWhereOperandTransformer();

  public static CriteriaApplied<Predicate> apply (CriteriaBuilder criteriaBuilder, Where where) {

    return apply(criteriaBuilder, where, WHERE_FIELD_TRANSFORMER, WHERE_OPERAND_TRANSFORMER);
  }

  public static CriteriaApplied<Predicate> apply (CriteriaBuilder criteriaBuilder, Where where, WhereFieldTransformer<Path<?>> fieldTransformer) {

    return apply(criteriaBuilder, where, fieldTransformer, WHERE_OPERAND_TRANSFORMER);
  }

  public static CriteriaApplied<Predicate> apply (CriteriaBuilder criteriaBuilder, Where where, WhereOperandTransformer operandTransformer) {

    return apply(criteriaBuilder, where, WHERE_FIELD_TRANSFORMER, operandTransformer);
  }

  public static CriteriaApplied<Predicate> apply (CriteriaBuilder criteriaBuilder, Where where, WhereFieldTransformer<Path<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    if (where == null) {

      return NoneCriteriaApplied.none();
    } else {

      SomeCriteriaApplied<Predicate> applied = new SomeCriteriaApplied<>();
      Predicate predicate;

      if ((predicate = walkConjunction(criteriaBuilder, new SomeCriteriaApplied<>(), where.getRootConjunction(), fieldTransformer, operandTransformer)) == null) {

        return NoneCriteriaApplied.none();
      }

      return applied.set(predicate);
    }
  }

  private static Predicate walkConjunction (CriteriaBuilder criteriaBuilder, SomeCriteriaApplied<Predicate> applied, WhereConjunction whereConjunction, WhereFieldTransformer<Path<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Predicate> predicateList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {
      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:

          Predicate walkedPredicate;

          if ((walkedPredicate = walkConjunction(criteriaBuilder, applied, (WhereConjunction)whereCriterion, fieldTransformer, operandTransformer)) != null) {
            predicateList.add(walkedPredicate);
          }
          break;
        case FIELD:
          predicateList.add(walkField(criteriaBuilder, applied, (WhereField)whereCriterion, fieldTransformer, operandTransformer));
          break;
        default:
          throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
      }
    }

    if (predicateList.isEmpty()) {

      return null;
    } else {

      Predicate[] predicates = new Predicate[predicateList.size()];

      predicateList.toArray(predicates);
      switch (whereConjunction.getConjunctionType()) {
        case AND:
          return criteriaBuilder.and(predicates);
        case OR:
          return criteriaBuilder.or(predicates);
        default:
          throw new UnknownSwitchCaseException(whereConjunction.getConjunctionType().name());
      }
    }
  }

  private static Predicate walkField (CriteriaBuilder criteriaBuilder, SomeCriteriaApplied<Predicate> applied, WhereField whereField, WhereFieldTransformer<Path<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    Object fieldValue = operandTransformer.transform(whereField.getOperand());
    WherePath<Path<?>> wherePath = fieldTransformer.transform(whereField.getEntity(), whereField.getName());

    applied.add(((CriteriaWherePath)wherePath).findRoot());
    switch (whereField.getOperator()) {
      case LT:
        return criteriaBuilder.lt((Path<Number>)wherePath.asNative(), (Number)fieldValue);
      case LE:
        return criteriaBuilder.le((Path<Number>)wherePath.asNative(), (Number)fieldValue);
      case EQ:
        if (fieldValue == null) {
          return criteriaBuilder.isNull(wherePath.asNative());
        } else {
          return criteriaBuilder.equal(wherePath.asNative(), fieldValue);
        }
      case NE:
        if (fieldValue == null) {
          return criteriaBuilder.isNotNull(wherePath.asNative());
        } else {
          return criteriaBuilder.notEqual(wherePath.asNative(), fieldValue);
        }
      case GE:
        return criteriaBuilder.ge((Path<Number>)wherePath.asNative(), (Number)fieldValue);
      case GT:
        return criteriaBuilder.gt((Path<Number>)wherePath.asNative(), (Number)fieldValue);
      case LIKE:
        return criteriaBuilder.like((Path<String>)wherePath.asNative(), (String)fieldValue);
      case UNLIKE:
        return criteriaBuilder.notLike((Path<String>)wherePath.asNative(), (String)fieldValue);
      case IN:
        return criteriaBuilder.in((Path<String>)wherePath.asNative()).in(fieldValue);
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static CriteriaApplied<Order[]> apply (CriteriaBuilder criteriaBuilder, Sort sort) {

    return apply(criteriaBuilder, sort, WHERE_FIELD_TRANSFORMER);
  }

  public static CriteriaApplied<Order[]> apply (CriteriaBuilder criteriaBuilder, Sort sort, WhereFieldTransformer<Path<?>> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      SomeCriteriaApplied<Order[]> applied = new SomeCriteriaApplied<>();
      Order[] orders;
      LinkedList<Order> orderList = new LinkedList<>();

      for (SortField sortField : sort.getFields()) {

        WherePath<Path<?>> wherePath = fieldTransformer.transform(sortField.getEntity(), sortField.getName());

        applied.add(((CriteriaWherePath)wherePath).findRoot());
        switch (sortField.getDirection()) {
          case ASC:
            orderList.add(criteriaBuilder.asc(wherePath.asNative()));
            break;
          case DESC:
            orderList.add(criteriaBuilder.desc(wherePath.asNative()));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }

      orders = new Order[orderList.size()];
      orderList.toArray(orders);

      return applied.set(orders);
    }

    return NoneCriteriaApplied.none();
  }
}
