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
package org.smallmind.web.json.query.data.mongo;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.orm.data.mongo.query.Criterion;
import org.smallmind.persistence.orm.data.mongo.query.Filter;
import org.smallmind.persistence.orm.data.mongo.query.Query;
import org.smallmind.web.json.query.QueryProcessingException;
import org.smallmind.web.json.query.Sort;
import org.smallmind.web.json.query.SortField;
import org.smallmind.web.json.query.Where;
import org.smallmind.web.json.query.WhereConjunction;
import org.smallmind.web.json.query.WhereCriterion;
import org.smallmind.web.json.query.WhereField;
import org.smallmind.web.json.query.WhereFieldTransformer;
import org.smallmind.web.json.query.WhereOperator;

public class MongoDataQueryUtility {

  private static final String SINGLE_WILDCARD = "*";
  private static final String DOUBLE_WILDCARD = SINGLE_WILDCARD + SINGLE_WILDCARD;
  private static final char WILDCARD_CHAR = '*';

  public static Query apply (Query query, Where where) {

    return apply(query, where, null);
  }

  public static Query apply (Query query, Where where, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if (where != null) {
      query.add(walkConjunction(where.getRootConjunction(), fieldTransformer));
    }

    return query;
  }

  private static Criterion walkConjunction (WhereConjunction whereConjunction, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    } else {

      LinkedList<Criterion> criterionList = new LinkedList<>();

      for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {

        Criterion criterion;

        switch (whereCriterion.getCriterionType()) {
          case CONJUNCTION:
            if ((criterion = walkConjunction((WhereConjunction)whereCriterion, fieldTransformer)) != null) {
              criterionList.add(criterion);
            }
            break;
          case FIELD:
            if ((criterion = walkField((WhereField)whereCriterion, fieldTransformer)) != null) {
              criterionList.add(criterion);
            }
            break;
          default:
            throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
        }
      }

      if (criterionList.isEmpty()) {

        return null;
      } else if (criterionList.size() == 1) {

        return criterionList.getFirst();
      } else {
        switch (whereConjunction.getConjunctionType()) {
          case AND:
            return Query.and(criterionList);
          case OR:
            return Query.or(criterionList);
          default:
            throw new UnknownSwitchCaseException(whereConjunction.getConjunctionType().name());
        }
      }
    }
  }

  private static Filter walkField (WhereField whereField, WhereFieldTransformer<Void, Void> fieldTransformer) {

    String fieldName = (fieldTransformer == null) ? whereField.getName() : fieldTransformer.transform(whereField.getEntity(), whereField.getName()).getField();
    Object fieldValue = whereField.getOperand().get();

    switch (whereField.getOperator()) {
      case LT:
        return Filter.where(fieldName).lt(fieldValue);
      case LE:
        return Filter.where(fieldName).lte(fieldValue);
      case EQ:
        return (fieldValue == null) ? Filter.where(fieldName).exists(false) : Filter.where(fieldName).eq(fieldValue);
      case NE:
        return (fieldValue == null) ? Filter.where(fieldName).exists(true) : Filter.where(fieldName).ne(fieldValue);
      case GE:
        return Filter.where(fieldName).gte(fieldValue);
      case GT:
        return Filter.where(fieldName).gt(fieldValue);
      case EXISTS:
        return Boolean.TRUE.equals(fieldValue) ? Filter.where(fieldName).exists(true) : Filter.where(fieldName).exists(false);
      case LIKE:

        Object likeValue;

        if ((likeValue = fieldValue) == null) {

          return Filter.where(fieldName).exists(false);
        } else if (!(likeValue instanceof String)) {

          throw new QueryProcessingException("The operation(%s) requires a String operand", WhereOperator.LIKE.name());
        } else {
          switch (((String)likeValue).length()) {
            case 0:
              return Filter.where(fieldName).eq("");
            case 1:
              return likeValue.equals(SINGLE_WILDCARD) ? Filter.where(fieldName).exists(true) : Filter.where(fieldName).eq(likeValue);
            case 2:
              return likeValue.equals(DOUBLE_WILDCARD) ? Filter.where(fieldName).exists(true) : (((String)likeValue).charAt(0) == WILDCARD_CHAR) ? Filter.where(fieldName).regex(".*" + ((String)likeValue).substring(1)) : (((String)likeValue).charAt(1) == WILDCARD_CHAR) ? Filter.where(fieldName).regex(((String)likeValue).charAt(0) + ".*") : Filter.where(fieldName).eq(likeValue);
            default:
              if (((String)likeValue).substring(1, ((String)likeValue).length() - 1).indexOf(WILDCARD_CHAR) >= 0) {
                throw new QueryProcessingException("The operation(%s) allows wildcards(%s) only at the start or end of the operand", WhereOperator.LIKE.name(), SINGLE_WILDCARD);
              } else if (((String)likeValue).startsWith(SINGLE_WILDCARD) && ((String)likeValue).endsWith(SINGLE_WILDCARD)) {

                return Filter.where(fieldName).regex(".*" + ((String)likeValue).substring(1, ((String)likeValue).length() - 1) + ".*");
              } else if (((String)likeValue).startsWith(SINGLE_WILDCARD)) {

                return Filter.where(fieldName).regex(".*" + ((String)likeValue).substring(1));
              } else if (((String)likeValue).endsWith(SINGLE_WILDCARD)) {

                return Filter.where(fieldName).regex(((String)likeValue).substring(0, ((String)likeValue).length() - 1) + ".*");
              } else {

                return Filter.where(fieldName).eq(likeValue);
              }
          }
        }
      case UNLIKE:

        Object unlikeValue;

        if ((unlikeValue = fieldValue) == null) {

          return Filter.where(fieldName).exists(true);
        } else if (!(unlikeValue instanceof String)) {

          throw new QueryProcessingException("The operation(%s) requires a String operand", WhereOperator.UNLIKE.name());
        } else {
          switch (((String)unlikeValue).length()) {
            case 0:
              return Filter.where(fieldName).ne("");
            case 1:
              return unlikeValue.equals(SINGLE_WILDCARD) ? Filter.where(fieldName).exists(false) : Filter.where(fieldName).ne(unlikeValue);
            case 2:
              return unlikeValue.equals(DOUBLE_WILDCARD) ? Filter.where(fieldName).exists(false) : (((String)unlikeValue).charAt(0) == WILDCARD_CHAR) ? Filter.where(fieldName).regex(".*" + ((String)unlikeValue).substring(1)).not() : (((String)unlikeValue).charAt(1) == WILDCARD_CHAR) ? Filter.where(fieldName).regex(((String)unlikeValue).charAt(0) + ".*").not() : Filter.where(fieldName).ne(unlikeValue);
            default:
              if (((String)unlikeValue).substring(1, ((String)unlikeValue).length() - 1).indexOf(WILDCARD_CHAR) >= 0) {
                throw new QueryProcessingException("The operation(%s) allows wildcards(%s) only at the start or end of the operand", WhereOperator.UNLIKE.name(), SINGLE_WILDCARD);
              } else if (((String)unlikeValue).startsWith(SINGLE_WILDCARD) && ((String)unlikeValue).endsWith(SINGLE_WILDCARD)) {

                return Filter.where(fieldName).regex(".*" + ((String)unlikeValue).substring(1, ((String)unlikeValue).length() - 1) + ".*").not();
              } else if (((String)unlikeValue).startsWith(SINGLE_WILDCARD)) {

                return Filter.where(fieldName).regex(".*" + ((String)unlikeValue).substring(1)).not();
              } else if (((String)unlikeValue).endsWith(SINGLE_WILDCARD)) {

                return Filter.where(fieldName).regex(((String)unlikeValue).substring(0, ((String)unlikeValue).length() - 1) + ".*").not();
              } else {

                return Filter.where(fieldName).ne(unlikeValue);
              }
          }
        }
      case IN:
        return Filter.where(fieldName).in((Object[])fieldValue);
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static Query apply (Query query, Sort sort) {

    return apply(query, sort, null);
  }

  public static Query apply (Query query, Sort sort, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      LinkedList<org.springframework.data.domain.Sort.Order> orderList = new LinkedList<>();

      for (SortField sortField : sort.getFields()) {

        String fieldName = (fieldTransformer == null) ? sortField.getName() : fieldTransformer.transform(sortField.getEntity(), sortField.getName()).getField();

        switch (sortField.getDirection()) {
          case ASC:
            orderList.add(org.springframework.data.domain.Sort.Order.asc(fieldName));
            break;
          case DESC:
            orderList.add(org.springframework.data.domain.Sort.Order.desc(fieldName));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }

      return query.orderBy(org.springframework.data.domain.Sort.by(orderList));
    }

    return query;
  }
}