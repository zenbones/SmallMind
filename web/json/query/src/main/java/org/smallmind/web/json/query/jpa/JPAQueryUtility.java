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
package org.smallmind.web.json.query.jpa;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.web.json.query.NoneProduct;
import org.smallmind.web.json.query.OperandType;
import org.smallmind.web.json.query.Product;
import org.smallmind.web.json.query.SomeProduct;
import org.smallmind.web.json.query.Sort;
import org.smallmind.web.json.query.SortField;
import org.smallmind.web.json.query.Where;
import org.smallmind.web.json.query.WhereConjunction;
import org.smallmind.web.json.query.WhereCriterion;
import org.smallmind.web.json.query.WhereField;
import org.smallmind.web.json.query.WhereFieldTransformer;
import org.smallmind.web.json.query.WherePath;
import org.smallmind.web.json.query.WildcardUtility;

public class JPAQueryUtility {

  public static Product<Root<?>, Predicate> apply (CriteriaBuilder criteriaBuilder, Where where, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer) {

    if (where == null) {

      return NoneProduct.none();
    } else {

      Set<Root<?>> rootSet = new HashSet<>();
      Predicate predicate;

      if ((predicate = walkConjunction(criteriaBuilder, rootSet, where.getRootConjunction(), fieldTransformer)) == null) {

        return NoneProduct.none();
      }

      return new SomeProduct<>(rootSet, predicate);
    }
  }

  private static Predicate walkConjunction (CriteriaBuilder criteriaBuilder, Set<Root<?>> rootSet, WhereConjunction whereConjunction, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Predicate> predicateList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {
      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:

          Predicate walkedPredicate;

          if ((walkedPredicate = walkConjunction(criteriaBuilder, rootSet, (WhereConjunction)whereCriterion, fieldTransformer)) != null) {
            predicateList.add(walkedPredicate);
          }
          break;
        case FIELD:
          predicateList.add(walkField(criteriaBuilder, rootSet, (WhereField)whereCriterion, fieldTransformer));
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

  private static Predicate walkField (CriteriaBuilder criteriaBuilder, Set<Root<?>> rootSet, WhereField whereField, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer) {

    Object fieldValue = whereField.getOperand().get();
    WherePath<Root<?>, Path<?>> wherePath = fieldTransformer.transform(whereField.getEntity(), whereField.getName());

    rootSet.add(wherePath.getRoot());
    switch (whereField.getOperator()) {
      case LT:
        return OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.lessThan((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.lt((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case LE:
        return OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.lessThanOrEqualTo((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.le((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case EQ:
        if (fieldValue == null) {
          return criteriaBuilder.isNull(wherePath.getPath());
        } else {
          return criteriaBuilder.equal(wherePath.getPath(), fieldValue);
        }
      case NE:
        if (fieldValue == null) {
          return criteriaBuilder.isNotNull(wherePath.getPath());
        } else {
          return criteriaBuilder.notEqual(wherePath.getPath(), fieldValue);
        }
      case GE:
        return OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.greaterThanOrEqualTo((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.ge((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case GT:
        return OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.greaterThan((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.gt((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case EXISTS:
        return Boolean.TRUE.equals(fieldValue) ? criteriaBuilder.isNotNull(wherePath.getPath()) : criteriaBuilder.isNull(wherePath.getPath());
      case LIKE:
        return criteriaBuilder.like((Path<String>)wherePath.getPath(), WildcardUtility.swapWithSqlWildcard((String)fieldValue));
      case UNLIKE:
        return criteriaBuilder.notLike((Path<String>)wherePath.getPath(), WildcardUtility.swapWithSqlWildcard((String)fieldValue));
      case IN:
        return criteriaBuilder.in((Path<?>)wherePath.getPath()).in(fieldValue);
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static Product<Root<?>, Order[]> apply (CriteriaBuilder criteriaBuilder, Sort sort, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      Set<Root<?>> rootSet = new HashSet<>();
      Order[] orders;
      LinkedList<Order> orderList = new LinkedList<>();

      for (SortField sortField : sort.getFields()) {

        WherePath<Root<?>, Path<?>> wherePath = fieldTransformer.transform(sortField.getEntity(), sortField.getName());

        rootSet.add(wherePath.getRoot());
        switch (sortField.getDirection()) {
          case ASC:
            orderList.add(criteriaBuilder.asc(wherePath.getPath()));
            break;
          case DESC:
            orderList.add(criteriaBuilder.desc(wherePath.getPath()));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }

      orders = new Order[orderList.size()];
      orderList.toArray(orders);

      return new SomeProduct<>(rootSet, orders);
    }

    return NoneProduct.none();
  }
}
