/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

  public static Product<Root<?>, Predicate> apply (CriteriaBuilder criteriaBuilder, Where where, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer, boolean allowNonTerminalWildcards) {

    if (where == null) {

      return NoneProduct.none();
    } else {

      Set<Root<?>> rootSet = new HashSet<>();
      Predicate predicate;

      if ((predicate = walkConjunction(criteriaBuilder, rootSet, where.getRootConjunction(), fieldTransformer, allowNonTerminalWildcards)) == null) {

        return NoneProduct.none();
      }

      return new SomeProduct<>(rootSet, predicate);
    }
  }

  private static Predicate walkConjunction (CriteriaBuilder criteriaBuilder, Set<Root<?>> rootSet, WhereConjunction whereConjunction, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer, boolean allowNonTerminalWildcards) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Predicate> predicateList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {
      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION -> {

          Predicate walkedPredicate;

          if ((walkedPredicate = walkConjunction(criteriaBuilder, rootSet, (WhereConjunction)whereCriterion, fieldTransformer, allowNonTerminalWildcards)) != null) {
            predicateList.add(walkedPredicate);
          }
        }
        case FIELD -> predicateList.add(walkField(criteriaBuilder, rootSet, (WhereField)whereCriterion, fieldTransformer, allowNonTerminalWildcards));
      }
    }

    if (predicateList.isEmpty()) {

      return null;
    } else {

      Predicate[] predicates = new Predicate[predicateList.size()];

      predicateList.toArray(predicates);
      return switch (whereConjunction.getConjunctionType()) {
        case AND -> criteriaBuilder.and(predicates);
        case OR -> criteriaBuilder.or(predicates);
      };
    }
  }

  private static Predicate walkField (CriteriaBuilder criteriaBuilder, Set<Root<?>> rootSet, WhereField whereField, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer, boolean allowNonTerminalWildcards) {

    Object fieldValue = whereField.getOperand().get();
    WherePath<Root<?>, Path<?>> wherePath = fieldTransformer.transform(whereField.getEntity(), whereField.getName());

    rootSet.add(wherePath.getRoot());

    return switch (whereField.getOperator()) {
      case LT -> OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.lessThan((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.lt((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case LE -> OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.lessThanOrEqualTo((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.le((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case EQ -> {
        if (fieldValue == null) {
          yield criteriaBuilder.isNull(wherePath.getPath());
        } else {
          yield criteriaBuilder.equal(wherePath.getPath(), fieldValue);
        }
      }
      case NE -> {
        if (fieldValue == null) {
          yield criteriaBuilder.isNotNull(wherePath.getPath());
        } else {
          yield criteriaBuilder.notEqual(wherePath.getPath(), fieldValue);
        }
      }
      case GE -> OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.greaterThanOrEqualTo((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.ge((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case GT -> OperandType.DATE.equals(whereField.getOperand().getOperandType()) ? criteriaBuilder.greaterThan((Path<Date>)wherePath.getPath(), (Date)fieldValue) : criteriaBuilder.gt((Path<Number>)wherePath.getPath(), (Number)fieldValue);
      case EXISTS -> Boolean.TRUE.equals(fieldValue) ? criteriaBuilder.isNotNull(wherePath.getPath()) : criteriaBuilder.isNull(wherePath.getPath());
      case LIKE -> criteriaBuilder.like((Path<String>)wherePath.getPath(), WildcardUtility.swapWithSqlWildcard((String)fieldValue, allowNonTerminalWildcards));
      case UNLIKE -> criteriaBuilder.notLike((Path<String>)wherePath.getPath(), WildcardUtility.swapWithSqlWildcard((String)fieldValue, allowNonTerminalWildcards));
      case IN -> criteriaBuilder.in((Path<?>)wherePath.getPath()).in(fieldValue);
    };
  }

  public static Product<Root<?>, Order[]> apply (CriteriaBuilder criteriaBuilder, Sort sort, WhereFieldTransformer<Root<?>, Path<?>> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      Set<Root<?>> rootSet = new HashSet<>();
      Order[] orders;
      LinkedList<Order> orderList = new LinkedList<>();

      for (SortField sortField : sort.getFields()) {

        WherePath<Root<?>, Path<?>> wherePath = fieldTransformer.transform(sortField.getEntity(), sortField.getName());

        rootSet.add(wherePath.getRoot());
        orderList.add(switch (sortField.getDirection()) {
          case ASC -> criteriaBuilder.asc(wherePath.getPath());
          case DESC -> criteriaBuilder.desc(wherePath.getPath());
        });
      }

      orders = new Order[orderList.size()];
      orderList.toArray(orders);

      return new SomeProduct<>(rootSet, orders);
    }

    return NoneProduct.none();
  }
}
