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
package org.smallmind.web.json.query.morphia;

import java.util.Arrays;
import java.util.LinkedList;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.web.json.query.QueryProcessingException;
import org.smallmind.web.json.query.Sort;
import org.smallmind.web.json.query.SortField;
import org.smallmind.web.json.query.Where;
import org.smallmind.web.json.query.WhereConjunction;
import org.smallmind.web.json.query.WhereCriterion;
import org.smallmind.web.json.query.WhereField;
import org.smallmind.web.json.query.WhereFieldTransformer;
import org.smallmind.web.json.query.WhereOperator;

public class MorphiaQueryUtility {

  private static final String SINGLE_WILDCARD = "*";
  private static final String DOUBLE_WILDCARD = SINGLE_WILDCARD + SINGLE_WILDCARD;
  private static final char WILDCARD_CHAR = '*';

  public static <T> Query<T> apply (Query<T> query, Where where) {

    return apply(query, where, null);
  }

  public static <T> Query<T> apply (Query<T> query, Where where, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if (where != null) {
      walkConjunction(query, where.getRootConjunction(), fieldTransformer);
    }

    return query;
  }

  private static <T> Criteria walkConjunction (Query<T> query, WhereConjunction whereConjunction, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Criteria> criteriaList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {

      Criteria criteria;

      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:
          if ((criteria = walkConjunction(query, (WhereConjunction)whereCriterion, fieldTransformer)) != null) {
            criteriaList.add(criteria);
          }
          break;
        case FIELD:
          if ((criteria = walkField(query, (WhereField)whereCriterion, fieldTransformer)) != null) {
            criteriaList.add(criteria);
          }
          break;
        default:
          throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
      }
    }

    if (criteriaList.isEmpty()) {

      return null;
    } else {

      Criteria[] criteria;

      criteria = new Criteria[criteriaList.size()];
      criteriaList.toArray(criteria);

      switch (whereConjunction.getConjunctionType()) {
        case AND:
          return query.and(criteria);
        case OR:
          return query.or(criteria);
        default:
          throw new UnknownSwitchCaseException(whereConjunction.getConjunctionType().name());
      }
    }
  }

  private static <T> Criteria walkField (Query<T> query, WhereField whereField, WhereFieldTransformer<Void, Void> fieldTransformer) {

    FieldEnd<? extends Criteria> fieldEnd = query.criteria((fieldTransformer == null) ? whereField.getName() : fieldTransformer.transform(whereField.getEntity(), whereField.getName()).getField());
    Object fieldValue = whereField.getOperand().get();

    switch (whereField.getOperator()) {
      case LT:
        return fieldEnd.lessThan(fieldValue);
      case LE:
        return fieldEnd.lessThanOrEq(fieldValue);
      case EQ:

        Object equalValue;

        return ((equalValue = fieldValue) == null) ? fieldEnd.doesNotExist() : fieldEnd.equal(equalValue);
      case NE:

        Object notEqualValue;

        return ((notEqualValue = fieldValue) == null) ? fieldEnd.exists() : fieldEnd.notEqual(notEqualValue);
      case GE:
        return fieldEnd.greaterThanOrEq(fieldValue);
      case GT:
        return fieldEnd.greaterThan(fieldValue);
      case LIKE:

        Object likeValue;

        if ((likeValue = fieldValue) == null) {

          return fieldEnd.doesNotExist();
        } else if (!(likeValue instanceof String)) {

          throw new QueryProcessingException("The operation(%s) requires a String operand", WhereOperator.LIKE.name());
        } else {
          switch (((String)likeValue).length()) {
            case 0:
              return fieldEnd.equal("");
            case 1:
              return likeValue.equals(SINGLE_WILDCARD) ? fieldEnd.exists() : fieldEnd.equal(likeValue);
            case 2:
              return likeValue.equals(DOUBLE_WILDCARD) ? fieldEnd.exists() : (((String)likeValue).charAt(0) == WILDCARD_CHAR) ? fieldEnd.endsWith(((String)likeValue).substring(1)) : (((String)likeValue).charAt(1) == WILDCARD_CHAR) ? fieldEnd.startsWith(((String)likeValue).substring(0, 1)) : fieldEnd.equal(likeValue);
            default:
              if (((String)likeValue).substring(1, ((String)likeValue).length() - 1).indexOf(WILDCARD_CHAR) >= 0) {
                throw new QueryProcessingException("The operation(%s) allows wildcards(%s) only at the start or end of the operand", WhereOperator.LIKE.name(), SINGLE_WILDCARD);
              } else if (((String)likeValue).startsWith(SINGLE_WILDCARD) && ((String)likeValue).endsWith(SINGLE_WILDCARD)) {

                return fieldEnd.contains(((String)likeValue).substring(1, ((String)likeValue).length() - 1));
              } else if (((String)likeValue).startsWith(SINGLE_WILDCARD)) {

                return fieldEnd.endsWith(((String)likeValue).substring(1));
              } else if (((String)likeValue).endsWith(SINGLE_WILDCARD)) {

                return fieldEnd.startsWith(((String)likeValue).substring(0, ((String)likeValue).length() - 1));
              } else {

                return fieldEnd.equal(likeValue);
              }
          }
        }
      case UNLIKE:

        Object unlikeValue;

        if ((unlikeValue = fieldValue) == null) {

          return fieldEnd.exists();
        } else if (!(unlikeValue instanceof String)) {

          throw new QueryProcessingException("The operation(%s) requires a String operand", WhereOperator.UNLIKE.name());
        } else {
          switch (((String)unlikeValue).length()) {
            case 0:
              return fieldEnd.notEqual("");
            case 1:
              return unlikeValue.equals(SINGLE_WILDCARD) ? fieldEnd.doesNotExist() : fieldEnd.notEqual(unlikeValue);
            case 2:
              return unlikeValue.equals(DOUBLE_WILDCARD) ? fieldEnd.doesNotExist() : (((String)unlikeValue).charAt(0) == WILDCARD_CHAR) ? fieldEnd.not().endsWith(((String)unlikeValue).substring(1)) : (((String)unlikeValue).charAt(1) == WILDCARD_CHAR) ? fieldEnd.not().startsWith(((String)unlikeValue).substring(0, 1)) : fieldEnd.notEqual(unlikeValue);
            default:
              if (((String)unlikeValue).substring(1, ((String)unlikeValue).length() - 1).indexOf(WILDCARD_CHAR) >= 0) {
                throw new QueryProcessingException("The operation(%s) allows wildcards(%s) only at the start or end of the operand", WhereOperator.UNLIKE.name(), SINGLE_WILDCARD);
              } else if (((String)unlikeValue).startsWith(SINGLE_WILDCARD) && ((String)unlikeValue).endsWith(SINGLE_WILDCARD)) {

                return fieldEnd.not().contains(((String)unlikeValue).substring(1, ((String)unlikeValue).length() - 1));
              } else if (((String)unlikeValue).startsWith(SINGLE_WILDCARD)) {

                return fieldEnd.not().endsWith(((String)unlikeValue).substring(1));
              } else if (((String)unlikeValue).endsWith(SINGLE_WILDCARD)) {

                return fieldEnd.not().startsWith(((String)unlikeValue).substring(0, ((String)unlikeValue).length() - 1));
              } else {

                return fieldEnd.notEqual(unlikeValue);
              }
          }
        }
      case IN:
        return fieldEnd.in(Arrays.asList((Object[])fieldValue));
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static <T> Query<T> apply (Query<T> query, Sort sort) {

    return apply(query, sort, null);
  }

  public static <T> Query<T> apply (Query<T> query, Sort sort, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      StringBuilder sortBuilder = new StringBuilder();

      for (SortField sortField : sort.getFields()) {

        String fieldName = (fieldTransformer == null) ? sortField.getName() : fieldTransformer.transform(sortField.getEntity(), sortField.getName()).getField();

        if (sortBuilder.length() > 0) {
          sortBuilder.append(", ");
        }
        switch (sortField.getDirection()) {
          case ASC:
            sortBuilder.append(fieldName);
            break;
          case DESC:
            sortBuilder.append('-').append(fieldName);
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }

      return query.order(sortBuilder.toString());
    }

    return query;
  }
}
