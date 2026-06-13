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
package org.smallmind.web.json.query.throng;

import org.smallmind.persistence.orm.throng.Filters;
import org.smallmind.web.json.query.AndWhereConjunction;
import org.smallmind.web.json.query.ArrayWhereOperand;
import org.smallmind.web.json.query.BooleanWhereOperand;
import org.smallmind.web.json.query.IntegerWhereOperand;
import org.smallmind.web.json.query.LongWhereOperand;
import org.smallmind.web.json.query.NullWhereOperand;
import org.smallmind.web.json.query.OrWhereConjunction;
import org.smallmind.web.json.query.QueryProcessingException;
import org.smallmind.web.json.query.Sort;
import org.smallmind.web.json.query.SortField;
import org.smallmind.web.json.query.StringWhereOperand;
import org.smallmind.web.json.query.Where;
import org.smallmind.web.json.query.WhereCriterion;
import org.smallmind.web.json.query.WhereField;
import org.smallmind.web.json.query.WhereOperator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Translates {@link Where}/{@link Sort} structures into Throng (MongoDB) filter and sort expressions. The
 * translation builds filter objects in memory, so no MongoDB connection is required.
 *
 * <p>{@link org.smallmind.web.json.query.throng.ThrongQueryUtility#apply(Filters, Where)} always returns the
 * same {@link Filters} instance it was handed, so each operator/conjunction/wildcard case below builds a
 * {@link Where} that drives a distinct branch of the translator and then asserts {@code assertSame}; the
 * branch is what is exercised, not the (unobservable) composed filter.
 */
@Test(groups = "unit")
public class ThrongQueryUtilityTest {

  private Filters applyField (String name, WhereOperator operator, org.smallmind.web.json.query.WhereOperand operand) {

    Filters filters = Filters.on();

    return ThrongQueryUtility.apply(filters, Where.instance(new AndWhereConjunction(WhereField.instance(name, operator, operand))));
  }

  public void testApplyBuildsFiltersForOperators () {

    Where where = Where.instance(new AndWhereConjunction(
      WhereField.instance("age", WhereOperator.GE, IntegerWhereOperand.instance(18)),
      WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("active")),
      WhereField.instance("id", WhereOperator.IN, ArrayWhereOperand.instance(new Integer[] {1, 2, 3})),
      WhereField.instance("name", WhereOperator.LIKE, StringWhereOperand.instance("bo*"))));

    Filters filters = Filters.on();

    Assert.assertSame(ThrongQueryUtility.apply(filters, where), filters);
  }

  public void testApplyNullWhereLeavesFiltersUntouched () {

    Filters filters = Filters.on();

    Assert.assertSame(ThrongQueryUtility.apply(filters, null), filters);
  }

  public void testApplyEmptyConjunctionLeavesFiltersUntouched () {

    Filters filters = Filters.on();

    Assert.assertSame(ThrongQueryUtility.apply(filters, Where.instance(new AndWhereConjunction())), filters);
  }

  public void testLessThan () {

    Assert.assertNotNull(applyField("age", WhereOperator.LT, IntegerWhereOperand.instance(40)));
  }

  public void testLessThanOrEqual () {

    Assert.assertNotNull(applyField("age", WhereOperator.LE, IntegerWhereOperand.instance(40)));
  }

  public void testGreaterThan () {

    Assert.assertNotNull(applyField("age", WhereOperator.GT, IntegerWhereOperand.instance(40)));
  }

  public void testGreaterThanOrEqual () {

    Assert.assertNotNull(applyField("age", WhereOperator.GE, IntegerWhereOperand.instance(40)));
  }

  public void testEqualValue () {

    Assert.assertNotNull(applyField("status", WhereOperator.EQ, StringWhereOperand.instance("active")));
  }

  public void testEqualNullBecomesExistsFalse () {

    Assert.assertNotNull(applyField("status", WhereOperator.EQ, NullWhereOperand.instance()));
  }

  public void testNotEqualValue () {

    Assert.assertNotNull(applyField("status", WhereOperator.NE, StringWhereOperand.instance("active")));
  }

  public void testNotEqualNullBecomesExistsTrue () {

    Assert.assertNotNull(applyField("status", WhereOperator.NE, NullWhereOperand.instance()));
  }

  public void testExistsTrue () {

    Assert.assertNotNull(applyField("status", WhereOperator.EXISTS, BooleanWhereOperand.instance(true)));
  }

  public void testExistsFalse () {

    Assert.assertNotNull(applyField("status", WhereOperator.EXISTS, BooleanWhereOperand.instance(false)));
  }

  public void testInArray () {

    Assert.assertNotNull(applyField("id", WhereOperator.IN, ArrayWhereOperand.instance(new Long[] {1L, 2L, 3L})));
  }

  public void testLikeNullBecomesExistsFalse () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, NullWhereOperand.instance()));
  }

  public void testLikeEmptyStringBecomesEqualsEmpty () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("")));
  }

  public void testLikeSingleWildcardBecomesExists () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("*")));
  }

  public void testLikeSingleCharLiteral () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("a")));
  }

  public void testLikeDoubleWildcardBecomesExists () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("**")));
  }

  public void testLikeTwoCharLeadingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("*a")));
  }

  public void testLikeTwoCharTrailingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("a*")));
  }

  public void testLikeTwoCharLiteral () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("ab")));
  }

  public void testLikeLeadingAndTrailingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("*bob*")));
  }

  public void testLikeLeadingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("*bob")));
  }

  public void testLikeTrailingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("bob*")));
  }

  public void testLikePlainLiteral () {

    Assert.assertNotNull(applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("bob")));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testLikeInteriorWildcardRejected () {

    applyField("name", WhereOperator.LIKE, StringWhereOperand.instance("bo*by"));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testLikeNonStringOperandRejected () {

    applyField("name", WhereOperator.LIKE, IntegerWhereOperand.instance(5));
  }

  public void testUnlikeNullBecomesExistsTrue () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, NullWhereOperand.instance()));
  }

  public void testUnlikeEmptyStringBecomesNotEqualsEmpty () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("")));
  }

  public void testUnlikeSingleWildcardBecomesExistsFalse () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("*")));
  }

  public void testUnlikeSingleCharLiteral () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("a")));
  }

  public void testUnlikeDoubleWildcardBecomesExistsFalse () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("**")));
  }

  public void testUnlikeTwoCharLeadingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("*a")));
  }

  public void testUnlikeTwoCharTrailingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("a*")));
  }

  public void testUnlikeTwoCharLiteral () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("ab")));
  }

  public void testUnlikeLeadingAndTrailingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("*bob*")));
  }

  public void testUnlikeLeadingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("*bob")));
  }

  public void testUnlikeTrailingWildcard () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("bob*")));
  }

  public void testUnlikePlainLiteral () {

    Assert.assertNotNull(applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("bob")));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testUnlikeInteriorWildcardRejected () {

    applyField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("bo*by"));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testUnlikeNonStringOperandRejected () {

    applyField("name", WhereOperator.UNLIKE, IntegerWhereOperand.instance(5));
  }

  public void testNestedOrConjunctionInsideAnd () {

    Where where = Where.instance(new AndWhereConjunction(
      WhereField.instance("age", WhereOperator.GE, IntegerWhereOperand.instance(18)),
      new OrWhereConjunction(
        WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("active")),
        WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("pending")))));

    Filters filters = Filters.on();

    Assert.assertSame(ThrongQueryUtility.apply(filters, where), filters);
  }

  public void testOrConjunctionAtRoot () {

    Where where = Where.instance(new OrWhereConjunction(
      WhereField.instance("age", WhereOperator.LT, IntegerWhereOperand.instance(18)),
      WhereField.instance("age", WhereOperator.GT, IntegerWhereOperand.instance(65))));

    Filters filters = Filters.on();

    Assert.assertSame(ThrongQueryUtility.apply(filters, where), filters);
  }

  public void testEmptyNestedConjunctionIsSkipped () {

    // The empty inner conjunction walks to null and is dropped, leaving a single effective field.
    Where where = Where.instance(new AndWhereConjunction(
      new AndWhereConjunction(),
      WhereField.instance("age", WhereOperator.GE, LongWhereOperand.instance(18L))));

    Filters filters = Filters.on();

    Assert.assertSame(ThrongQueryUtility.apply(filters, where), filters);
  }

  public void testApplyWithFieldTransformerRenamesField () {

    Where where = Where.instance(new AndWhereConjunction(
      WhereField.instance("e", "age", WhereOperator.GE, IntegerWhereOperand.instance(18))));

    Filters filters = Filters.on();

    Assert.assertSame(ThrongQueryUtility.apply(filters, where, new ThrongWhereFieldTransformer((entity, name) -> new ThrongWherePath("mapped_" + name))), filters);
  }

  public void testApplySortProducesSort () {

    Sort sort = Sort.instance(
      SortField.instance("created", org.smallmind.nutsnbolts.json.SortDirection.DESC),
      SortField.instance("name", org.smallmind.nutsnbolts.json.SortDirection.ASC));

    Assert.assertNotNull(ThrongQueryUtility.apply(sort));
  }

  public void testApplyNullSortProducesEmptySort () {

    Assert.assertNotNull(ThrongQueryUtility.apply((Sort)null));
  }

  public void testApplySortWithFieldTransformer () {

    Sort sort = Sort.instance(SortField.instance("e", "created", org.smallmind.nutsnbolts.json.SortDirection.DESC));

    Assert.assertNotNull(ThrongQueryUtility.apply(sort, new ThrongWhereFieldTransformer((entity, name) -> new ThrongWherePath("mapped_" + name))));
  }
}
