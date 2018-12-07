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

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.query.NoneProduct;
import org.smallmind.persistence.query.Product;
import org.smallmind.persistence.query.SomeProduct;
import org.smallmind.persistence.query.Sort;
import org.smallmind.persistence.query.SortField;
import org.smallmind.persistence.query.Where;
import org.smallmind.persistence.query.WhereConjunction;
import org.smallmind.persistence.query.WhereCriterion;
import org.smallmind.persistence.query.WhereField;
import org.smallmind.persistence.query.WhereFieldTransformer;
import org.smallmind.persistence.query.WhereOperandTransformer;
import org.smallmind.persistence.query.WherePath;

public class QUtility {

  public static Product<EntityPath<?>, Predicate> apply (Where where, WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer) {

    return apply(where, fieldTransformer, WhereOperandTransformer.instance());
  }

  public static Product<EntityPath<?>, Predicate> apply (Where where, WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    if (where == null) {

      return NoneProduct.none();
    } else {

      Set<EntityPath<?>> rootSet = new HashSet<>();
      Predicate predicate;

      if ((predicate = walkConjunction(rootSet, where.getRootConjunction(), fieldTransformer, operandTransformer)) == null) {

        return NoneProduct.none();
      }

      return new SomeProduct<>(rootSet, predicate);
    }
  }

  private static Predicate walkConjunction (Set<EntityPath<?>> rootSet, WhereConjunction whereConjunction, WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Predicate> predicateList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {

      Predicate walkedPredicate;

      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:
          if ((walkedPredicate = walkConjunction(rootSet, (WhereConjunction)whereCriterion, fieldTransformer, operandTransformer)) != null) {
            predicateList.add(walkedPredicate);
          }
          break;
        case FIELD:
          if ((walkedPredicate = walkField(rootSet, (WhereField)whereCriterion, fieldTransformer, operandTransformer)) != null) {
            predicateList.add(walkedPredicate);
          }
          break;
        default:
          throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
      }
    }

    if (predicateList.isEmpty()) {

      return null;
    } else {

      BooleanBuilder booleanBuilder = new BooleanBuilder();

      switch (whereConjunction.getConjunctionType()) {
        case AND:
          for (Predicate predicate : predicateList) {
            booleanBuilder.and(predicate);
          }
          return booleanBuilder;
        case OR:
          for (Predicate predicate : predicateList) {
            booleanBuilder.or(predicate);
          }
          return booleanBuilder;
        default:
          throw new UnknownSwitchCaseException(whereConjunction.getConjunctionType().name());
      }
    }
  }

  private static Predicate walkField (Set<EntityPath<?>> rootSet, WhereField whereField, WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    Object fieldValue = operandTransformer.transform(whereField.getOperand());
    WherePath<EntityPath<?>, Path<?>> wherePath = fieldTransformer.transform(whereField.getEntity(), whereField.getName());

    rootSet.add(wherePath.getRoot());
    switch (whereField.getOperator()) {
      case LT:
        return Expressions.predicate(Ops.LT, wherePath.getPath(), Expressions.constant(fieldValue));
      case LE:
        return Expressions.predicate(Ops.LOE, wherePath.getPath(), Expressions.constant(fieldValue));
      case EQ:
        if (fieldValue == null) {
          return Expressions.predicate(Ops.IS_NULL, wherePath.getPath());
        } else {
          return Expressions.predicate(Ops.EQ, wherePath.getPath(), Expressions.constant(fieldValue));
        }
      case NE:
        if (fieldValue == null) {
          return Expressions.predicate(Ops.IS_NOT_NULL, wherePath.getPath());
        } else {
          return Expressions.predicate(Ops.NE, wherePath.getPath(), Expressions.constant(fieldValue));
        }
      case GE:
        return Expressions.predicate(Ops.GOE, wherePath.getPath(), Expressions.constant(fieldValue));
      case GT:
        return Expressions.predicate(Ops.GT, wherePath.getPath(), Expressions.constant(fieldValue));
      case LIKE:
        return Expressions.predicate(Ops.LIKE, wherePath.getPath(), Expressions.constant(fieldValue));
      case UNLIKE:
        return Expressions.predicate(Ops.NOT, Expressions.predicate(Ops.LIKE, wherePath.getPath(), Expressions.constant(fieldValue)));
      case IN:

        int arrayLength;

        if ((arrayLength = Array.getLength(fieldValue)) == 0) {

          return null;
        } else {

          Expression<?> collectionExpression = Expressions.collectionOperation(fieldValue.getClass().getComponentType(), Ops.SINGLETON, Expressions.constant(Array.get(fieldValue, 0)));

          if (arrayLength > 1) {
            for (int index = 1; index < arrayLength; index++) {
              collectionExpression = Expressions.collectionOperation(fieldValue.getClass().getComponentType(), Ops.LIST, collectionExpression, Expressions.constant(Array.get(fieldValue, index)));
            }
          }

          return Expressions.predicate(Ops.IN, wherePath.getPath(), collectionExpression);
        }
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static Product<EntityPath<?>, OrderSpecifier[]> apply (Sort sort, WhereFieldTransformer<EntityPath<?>, Path<?>> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      Set<EntityPath<?>> rootSet = new HashSet<>();
      OrderSpecifier[] orderSpecifiers;
      LinkedList<OrderSpecifier<?>> orderSpecifierList = new LinkedList<>();

      for (SortField sortField : sort.getFields()) {

        WherePath<EntityPath<?>, Path<?>> wherePath = fieldTransformer.transform(sortField.getEntity(), sortField.getName());

        rootSet.add(wherePath.getRoot());
        switch (sortField.getDirection()) {
          case ASC:
            orderSpecifierList.add(new OrderSpecifier(Order.ASC, wherePath.getPath()));
            break;
          case DESC:
            orderSpecifierList.add(new OrderSpecifier(Order.DESC, wherePath.getPath()));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }

      orderSpecifiers = new OrderSpecifier[orderSpecifierList.size()];
      orderSpecifierList.toArray(orderSpecifiers);

      return new SomeProduct<>(rootSet, orderSpecifiers);
    }

    return NoneProduct.none();
  }
}
