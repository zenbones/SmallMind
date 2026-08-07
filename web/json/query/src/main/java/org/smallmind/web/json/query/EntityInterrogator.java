/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.web.json.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.smallmind.nutsnbolts.json.SortDirection;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.smallmind.web.json.scaffold.reflection.BeanReflector;
import org.smallmind.web.json.scaffold.util.Page;

/**
 * Applies a query, expressed as the same {@link Where} and {@link Sort} structures used to drive persistent stores,
 * against an in-memory {@link List} of entities. Field values are read reflectively via {@link BeanReflector} and
 * wrapped as {@link WhereOperand}s, so filtering and ordering follow the same comparison semantics as the query
 * translations targeting JPA, QueryDsl and the like.
 */
public class EntityInterrogator {

  /**
   * Filters, sorts and pages the given entities. An entity is retained only if it satisfies both the {@code where}
   * clause and the supplied {@link Predicate}; the survivors are then ordered by {@code sort} and the requested window
   * is extracted. A {@code null} or empty {@code where} or {@code sort} is skipped, and a {@code null} entity list is
   * treated as empty.
   *
   * @param entityList the entities to interrogate, may be {@code null}
   * @param where      the filter to apply, may be {@code null} to match everything
   * @param sort       the ordering to apply, may be {@code null} or empty to leave the list in encounter order
   * @param first      zero-based offset of the first result to return, may be {@code null} for {@code 0}
   * @param max        maximum number of results to return, may be {@code null} for all remaining results
   * @param predicate  an additional filter which must also accept an entity for it to be retained
   * @param <E>        the entity type
   * @return a page over the requested window, carrying the total count of matching entities
   * @throws QueryProcessingException if {@code first} or {@code max} is negative, or if a field named by the query
   *                                  can't be read from an entity
   */
  public static <E> Page<E> page (List<E> entityList, Where where, Sort sort, Long first, Integer max, Predicate<E> predicate) {

    ArrayList<E> filteredList;
    E[] pagedArray;
    long firstResult;
    int fromIndex;
    int toIndex;

    if ((first != null) && (first < 0)) {
      throw new QueryProcessingException("The first result offset(%s) may not be negative", first);
    }
    if ((max != null) && (max < 0)) {
      throw new QueryProcessingException("The maximum result count(%s) may not be negative", max);
    }

    filteredList = new ArrayList<>((entityList == null) ? 0 : entityList.size());

    if (entityList != null) {
      for (E entity : entityList) {
        if (matchesWhere(entity, where) && predicate.test(entity)) {
          filteredList.add(entity);
        }
      }
    }

    if ((sort != null) && (!sort.isEmpty())) {
      filteredList.sort(new SortFieldComparator<>(sort.getFields()));
    }

    firstResult = (first == null) ? 0 : first;
    fromIndex = (firstResult >= filteredList.size()) ? filteredList.size() : (int)firstResult;
    toIndex = (max == null) ? filteredList.size() : (int)Math.min(filteredList.size(), (long)fromIndex + max);
    pagedArray = (E[])filteredList.subList(fromIndex, toIndex).toArray();

    return new Page<>(pagedArray, firstResult, (max == null) ? pagedArray.length : max, filteredList.size());
  }

  /**
   * Determines whether an entity satisfies a where clause, treating a missing clause or a missing root conjunction as
   * an unconditional match.
   *
   * @param entity the entity to test
   * @param where  the filter to apply, may be {@code null}
   * @param <E>    the entity type
   * @return {@code true} if the entity matches
   */
  private static <E> boolean matchesWhere (E entity, Where where) {

    WhereConjunction rootConjunction;

    if ((where == null) || ((rootConjunction = where.getRootConjunction()) == null)) {

      return true;
    }

    return matchesConjunction(entity, rootConjunction);
  }

  /**
   * Determines whether an entity satisfies a conjunction, combining its criteria as a logical 'and' or 'or' according
   * to the conjunction type. An empty conjunction imposes no constraint and matches unconditionally.
   *
   * @param entity      the entity to test
   * @param conjunction the conjunction to apply
   * @param <E>         the entity type
   * @return {@code true} if the entity matches
   */
  private static <E> boolean matchesConjunction (E entity, WhereConjunction conjunction) {

    return conjunction.isEmpty() || switch (conjunction.getConjunctionType()) {
      case AND -> matchesAllCriteria(entity, conjunction.getCriteria());
      case OR -> matchesAnyCriterion(entity, conjunction.getCriteria());
    };
  }

  /**
   * Determines whether an entity satisfies every one of the given criteria, short-circuiting on the first failure.
   *
   * @param entity   the entity to test
   * @param criteria the criteria to apply
   * @param <E>      the entity type
   * @return {@code true} if the entity matches all criteria
   */
  private static <E> boolean matchesAllCriteria (E entity, WhereCriterion[] criteria) {

    for (WhereCriterion criterion : criteria) {
      if (!matchesCriterion(entity, criterion)) {

        return false;
      }
    }

    return true;
  }

  /**
   * Determines whether an entity satisfies at least one of the given criteria, short-circuiting on the first match.
   *
   * @param entity   the entity to test
   * @param criteria the criteria to apply
   * @param <E>      the entity type
   * @return {@code true} if the entity matches any criterion
   */
  private static <E> boolean matchesAnyCriterion (E entity, WhereCriterion[] criteria) {

    for (WhereCriterion criterion : criteria) {
      if (matchesCriterion(entity, criterion)) {

        return true;
      }
    }

    return false;
  }

  /**
   * Determines whether an entity satisfies a single criterion. A nested conjunction recurses, while a field criterion
   * reads the named property from the entity, wraps it as a {@link WhereOperand}, and hands both operands to the
   * criterion's operator.
   *
   * @param entity    the entity to test
   * @param criterion the criterion to apply
   * @param <E>       the entity type
   * @return {@code true} if the entity matches
   * @throws QueryProcessingException if the field named by the criterion can't be read from the entity
   */
  private static <E> boolean matchesCriterion (E entity, WhereCriterion criterion) {

    return switch (criterion.getCriterionType()) {
      case CONJUNCTION -> matchesConjunction(entity, (WhereConjunction)criterion);
      case FIELD -> {
        try {
          yield ((WhereField)criterion).getOperator().isTrue(((WhereField)criterion).getOperand(), WhereOperand.fromObject(BeanReflector.get(entity, constructAttributePath((WhereField)criterion))));
        } catch (BeanAccessException beanAccessException) {
          throw new QueryProcessingException(beanAccessException);
        }
      }
    };
  }

  /**
   * Assembles the dotted bean path used to read a where field's value from an entity. A field naming an entity is
   * qualified by it, so that the value is reached through that association rather than off the root entity.
   *
   * @param whereField the field to resolve
   * @return the field name, prefixed by the entity name and a dot when the field declares one
   */
  private static String constructAttributePath (WhereField whereField) {

    return ((whereField.getEntity() == null) || whereField.getEntity().isBlank()) ? whereField.getName() : whereField.getEntity() + "." + whereField.getName();
  }

  /**
   * Assembles the dotted bean path used to read a sort field's value from an entity. A field naming an entity is
   * qualified by it, so that the value is reached through that association rather than off the root entity.
   *
   * @param sortField the field to resolve
   * @return the field name, prefixed by the entity name and a dot when the field declares one
   */
  private static String constructAttributePath (SortField sortField) {

    return ((sortField.getEntity() == null) || sortField.getEntity().isBlank()) ? sortField.getName() : sortField.getEntity() + "." + sortField.getName();
  }

  /**
   * Orders entities by a list of sort fields, applied in declaration order as successive tie-breakers.
   *
   * @param sortFields the fields to order by, each carrying its own direction
   * @param <E>        the entity type
   */
  private record SortFieldComparator<E>(SortField... sortFields) implements Comparator<E> {

    /**
     * Compares two entities by walking the sort fields in order and returning the first non-zero comparison. Each
     * field's value is read from both entities and wrapped as a {@link WhereOperand}, with the comparison reversed for
     * a descending field. A field with no direction is treated as {@link SortDirection#ASC}.
     *
     * @param firstEntity  the first entity to compare
     * @param secondEntity the second entity to compare
     * @return a negative integer, zero, or a positive integer as the first entity sorts before, at, or after the second
     * @throws QueryProcessingException if a field named by a sort field can't be read from either entity
     */
    @Override
    public int compare (E firstEntity, E secondEntity) {

      for (SortField sortField : sortFields) {
        try {

          WhereOperand<?> firstOperand = WhereOperand.fromObject(BeanReflector.get(firstEntity, constructAttributePath(sortField)));
          WhereOperand<?> secondOperand = WhereOperand.fromObject(BeanReflector.get(secondEntity, constructAttributePath(sortField)));
          int comparison = switch ((sortField.getDirection() == null) ? SortDirection.ASC : sortField.getDirection()) {
            case ASC -> firstOperand.compareTo(secondOperand);
            case DESC -> secondOperand.compareTo(firstOperand);
          };

          if (comparison != 0) {

            return comparison;
          }
        } catch (BeanAccessException beanAccessException) {
          throw new QueryProcessingException(beanAccessException);
        }
      }

      return 0;
    }
  }
}
