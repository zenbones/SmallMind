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
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
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
      query.filter(walkConjunction(where.getRootConjunction(), fieldTransformer));
    }

    return query;
  }

  private static Filter walkConjunction (WhereConjunction whereConjunction, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    } else {

      LinkedList<Filter> filterList = new LinkedList<>();

      for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {

        Filter filter;

        switch (whereCriterion.getCriterionType()) {
          case CONJUNCTION:
            if ((filter = walkConjunction((WhereConjunction)whereCriterion, fieldTransformer)) != null) {
              filterList.add(filter);
            }
            break;
          case FIELD:
            if ((filter = walkField((WhereField)whereCriterion, fieldTransformer)) != null) {
              filterList.add(filter);
            }
            break;
          default:
            throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
        }
      }

      if (filterList.isEmpty()) {

        return null;
      } else {

        Filter[] filters = filterList.toArray(new Filter[0]);

        switch (whereConjunction.getConjunctionType()) {
          case AND:
            return Filters.and(filters);
          case OR:
            return Filters.or(filters);
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
        return Filters.lt(fieldName, fieldValue);
      case LE:
        return Filters.lte(fieldName, fieldValue);
      case EQ:

        Object equalValue;

        return ((equalValue = fieldValue) == null) ? Filters.exists(fieldName).not() : Filters.eq(fieldName, equalValue);
      case NE:

        Object notEqualValue;

        return ((notEqualValue = fieldValue) == null) ? Filters.exists(fieldName) : Filters.ne(fieldName, notEqualValue);
      case GE:
        return Filters.gte(fieldName, fieldValue);
      case GT:
        return Filters.gt(fieldName, fieldValue);
      case EXISTS:
        return Boolean.TRUE.equals(fieldValue) ? Filters.exists(fieldName) : Filters.exists(fieldName).not();
      case LIKE:

        Object likeValue;

        if ((likeValue = fieldValue) == null) {

          return Filters.exists(fieldName).not();
        } else if (!(likeValue instanceof String)) {

          throw new QueryProcessingException("The operation(%s) requires a String operand", WhereOperator.LIKE.name());
        } else {
          switch (((String)likeValue).length()) {
            case 0:
              return Filters.eq(fieldName, "");
            case 1:
              return likeValue.equals(SINGLE_WILDCARD) ? Filters.exists(fieldName) : Filters.eq(fieldName, likeValue);
            case 2:
              return likeValue.equals(DOUBLE_WILDCARD) ? Filters.exists(fieldName) : (((String)likeValue).charAt(0) == WILDCARD_CHAR) ? Filters.regex(fieldName).pattern(".*" + ((String)likeValue).substring(1)) : (((String)likeValue).charAt(1) == WILDCARD_CHAR) ? Filters.regex(fieldName).pattern(((String)likeValue).charAt(0) + ".*") : Filters.eq(fieldName, likeValue);
            default:
              if (((String)likeValue).substring(1, ((String)likeValue).length() - 1).indexOf(WILDCARD_CHAR) >= 0) {
                throw new QueryProcessingException("The operation(%s) allows wildcards(%s) only at the start or end of the operand", WhereOperator.LIKE.name(), SINGLE_WILDCARD);
              } else if (((String)likeValue).startsWith(SINGLE_WILDCARD) && ((String)likeValue).endsWith(SINGLE_WILDCARD)) {

                return Filters.regex(fieldName).pattern(".*" + ((String)likeValue).substring(1, ((String)likeValue).length() - 1) + ".*");
              } else if (((String)likeValue).startsWith(SINGLE_WILDCARD)) {

                return Filters.regex(fieldName).pattern(".*" + ((String)likeValue).substring(1));
              } else if (((String)likeValue).endsWith(SINGLE_WILDCARD)) {

                return Filters.regex(fieldName).pattern(((String)likeValue).substring(0, ((String)likeValue).length() - 1) + ".*");
              } else {

                return Filters.eq(fieldName, likeValue);
              }
          }
        }
      case UNLIKE:

        Object unlikeValue;

        if ((unlikeValue = fieldValue) == null) {

          return Filters.exists(fieldName);
        } else if (!(unlikeValue instanceof String)) {

          throw new QueryProcessingException("The operation(%s) requires a String operand", WhereOperator.UNLIKE.name());
        } else {
          switch (((String)unlikeValue).length()) {
            case 0:
              return Filters.ne(fieldName, "");
            case 1:
              return unlikeValue.equals(SINGLE_WILDCARD) ? Filters.exists(fieldName).not() : Filters.ne(fieldName, unlikeValue);
            case 2:
              return unlikeValue.equals(DOUBLE_WILDCARD) ? Filters.exists(fieldName).not() : (((String)unlikeValue).charAt(0) == WILDCARD_CHAR) ? Filters.regex(fieldName).pattern(".*" + ((String)unlikeValue).substring(1)).not() : (((String)unlikeValue).charAt(1) == WILDCARD_CHAR) ? Filters.regex(fieldName).pattern(((String)unlikeValue).charAt(0) + ".*").not() : Filters.ne(fieldName, unlikeValue);
            default:
              if (((String)unlikeValue).substring(1, ((String)unlikeValue).length() - 1).indexOf(WILDCARD_CHAR) >= 0) {
                throw new QueryProcessingException("The operation(%s) allows wildcards(%s) only at the start or end of the operand", WhereOperator.UNLIKE.name(), SINGLE_WILDCARD);
              } else if (((String)unlikeValue).startsWith(SINGLE_WILDCARD) && ((String)unlikeValue).endsWith(SINGLE_WILDCARD)) {

                return Filters.regex(fieldName).pattern(".*" + ((String)unlikeValue).substring(1, ((String)unlikeValue).length() - 1) + ".*").not();
              } else if (((String)unlikeValue).startsWith(SINGLE_WILDCARD)) {

                return Filters.regex(fieldName).pattern(".*" + ((String)unlikeValue).substring(1)).not();
              } else if (((String)unlikeValue).endsWith(SINGLE_WILDCARD)) {

                return Filters.regex(fieldName).pattern(((String)unlikeValue).substring(0, ((String)unlikeValue).length() - 1) + ".*").not();
              } else {

                return Filters.ne(fieldName, unlikeValue);
              }
          }
        }
      case IN:
        return Filters.in(fieldName, Arrays.asList((Object[])fieldValue));
      default:
        throw new UnknownSwitchCaseException(whereField.getOperator().name());
    }
  }

  public static FindOptions apply (FindOptions findOptions, Sort sort) {

    return apply(findOptions, sort, null);
  }

  public static FindOptions apply (FindOptions findOptions, Sort sort, WhereFieldTransformer<Void, Void> fieldTransformer) {

    if ((sort != null) && (!sort.isEmpty())) {

      LinkedList<dev.morphia.query.Sort> morphiaSortList = new LinkedList<>();

      for (SortField sortField : sort.getFields()) {

        String fieldName = (fieldTransformer == null) ? sortField.getName() : fieldTransformer.transform(sortField.getEntity(), sortField.getName()).getField();

        switch (sortField.getDirection()) {
          case ASC:
            morphiaSortList.add(dev.morphia.query.Sort.ascending(fieldName));
            break;
          case DESC:
            morphiaSortList.add(dev.morphia.query.Sort.descending(fieldName));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }

      return findOptions.sort(morphiaSortList.toArray(new dev.morphia.query.Sort[0]));
    }

    return findOptions;
  }
}
