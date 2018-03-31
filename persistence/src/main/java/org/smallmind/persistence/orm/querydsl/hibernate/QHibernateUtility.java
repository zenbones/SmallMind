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

import java.lang.reflect.Array;
import java.util.LinkedList;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
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

public class QHibernateUtility {

  private static final WhereFieldTransformer<EntityPath<?>> WHERE_FIELD_TRANSFORMER = new QWhereFieldTransformer();
  private static final WhereOperandTransformer WHERE_OPERAND_TRANSFORMER = new DefaultWhereOperandTransformer();

  public static QApplied<Predicate> apply (Where where) {

    return apply(where, WHERE_FIELD_TRANSFORMER, WHERE_OPERAND_TRANSFORMER);
  }

  public static QApplied<Predicate> apply (Where where, WhereFieldTransformer<EntityPath<?>> fieldTransformer) {

    return apply(where, fieldTransformer, WHERE_OPERAND_TRANSFORMER);
  }

  public static QApplied<Predicate> apply (Where where, WhereOperandTransformer operandTransformer) {

    return apply(where, WHERE_FIELD_TRANSFORMER, operandTransformer);
  }

  public static QApplied<Predicate> apply (Where where, WhereFieldTransformer<EntityPath<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    if (where == null) {

      return NoneQApplied.none();
    } else {

      SomeQApplied<Predicate> qApplied = new SomeQApplied<>();
      Predicate predicate;

      if ((predicate = walkConjunction(new SomeQApplied<>(), where.getRootConjunction(), fieldTransformer, operandTransformer)) == null) {

        return NoneQApplied.none();
      }

      return qApplied.set(predicate);
    }
  }

  private static Predicate walkConjunction (SomeQApplied<Predicate> qApplied, WhereConjunction whereConjunction, WhereFieldTransformer<EntityPath<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Predicate> predicateList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {
      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:

          Predicate walkedPredicate;

          if ((walkedPredicate = walkConjunction(qApplied, (WhereConjunction)whereCriterion, fieldTransformer, operandTransformer)) != null) {
            predicateList.add(walkedPredicate);
          }
          break;
        case FIELD:
          predicateList.add(walkField(qApplied, (WhereField)whereCriterion, fieldTransformer, operandTransformer));
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

  private static Predicate walkField (SomeQApplied<Predicate> qApplied, WhereField whereField, WhereFieldTransformer<EntityPath<?>> fieldTransformer, WhereOperandTransformer operandTransformer) {

    Object fieldValue = operandTransformer.transform(whereField.getOperand());
    WherePath<EntityPath<?>> wherePath = fieldTransformer.transform(whereField.getEntity(), whereField.getName());

    qApplied.add(wherePath.asNative());
    switch (whereField.getOperator()) {
      case LT:
        return Expressions.predicate(Ops.LT, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue));
      case LE:
        return Expressions.predicate(Ops.LOE, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue));
      case EQ:
        if (fieldValue == null) {
          return Expressions.predicate(Ops.IS_NULL, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()));
        } else {
          return Expressions.predicate(Ops.EQ, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue));
        }
      case NE:
        if (fieldValue == null) {
          return Expressions.predicate(Ops.IS_NOT_NULL, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()));
        } else {
          return Expressions.predicate(Ops.NE, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue));
        }
      case GE:
        return Expressions.predicate(Ops.GOE, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue));
      case GT:
        return Expressions.predicate(Ops.GT, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue));
      case LIKE:
        return Expressions.predicate(Ops.LIKE, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue));
      case UNLIKE:
        return Expressions.predicate(Ops.NOT, Expressions.predicate(Ops.LIKE, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.constant(fieldValue)));
      case IN:

        int arrayLength = Array.getLength(fieldValue);
        Expression[] arrayElements = new Expression[arrayLength];

        for (int index = 0; index < arrayLength; index++) {
          arrayElements[index] = Expressions.constant(Array.get(fieldValue, index));
        }

        return Expressions.predicate(Ops.IN, Expressions.path(String.class, wherePath.asNative(), wherePath.asString()), Expressions.collectionOperation(fieldValue.getClass().getComponentType(), Ops.LIST, arrayElements));
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static QApplied<OrderSpecifier[]> apply (Sort sort) {

    return apply(sort, WHERE_FIELD_TRANSFORMER);
  }

  public static QApplied<OrderSpecifier[]> apply (Sort sort, WhereFieldTransformer<EntityPath<?>> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      SomeQApplied<OrderSpecifier[]> qApplied = new SomeQApplied<>();
      OrderSpecifier[] orderSpecifiers;
      LinkedList<OrderSpecifier<?>> orderSpecifierList = new LinkedList<>();

      for (SortField sortField : sort.getFields()) {

        WherePath<EntityPath<?>> wherePath = fieldTransformer.transform(sortField.getEntity(), sortField.getName());

        qApplied.add(wherePath.asNative());
        switch (sortField.getDirection()) {
          case ASC:
            orderSpecifierList.add(new OrderSpecifier<>(Order.ASC, Expressions.path(String.class, wherePath.asNative(), wherePath.asString())));
            break;
          case DESC:
            orderSpecifierList.add(new OrderSpecifier<>(Order.DESC, Expressions.path(String.class, wherePath.asNative(), wherePath.asString())));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }

      orderSpecifiers = new OrderSpecifier[orderSpecifierList.size()];
      orderSpecifierList.toArray(orderSpecifiers);

      return qApplied.set(orderSpecifiers);
    }

    return NoneQApplied.none();
  }
}
