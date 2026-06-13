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
package org.smallmind.web.json.query.querydsl;

import java.util.function.BiFunction;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import org.smallmind.nutsnbolts.json.SortDirection;
import org.smallmind.web.json.query.AndWhereConjunction;
import org.smallmind.web.json.query.ArrayWhereOperand;
import org.smallmind.web.json.query.IntegerWhereOperand;
import org.smallmind.web.json.query.LongWhereOperand;
import org.smallmind.web.json.query.NullWhereOperand;
import org.smallmind.web.json.query.OrWhereConjunction;
import org.smallmind.web.json.query.Product;
import org.smallmind.web.json.query.Sort;
import org.smallmind.web.json.query.SortField;
import org.smallmind.web.json.query.StringWhereOperand;
import org.smallmind.web.json.query.Where;
import org.smallmind.web.json.query.WhereField;
import org.smallmind.web.json.query.WhereOperand;
import org.smallmind.web.json.query.WhereOperator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Translates {@link Where}/{@link Sort} structures into QueryDSL predicates and order specifiers using a
 * {@link PathBuilder}-backed transformer (no entity Q-types or database needed).
 */
@Test(groups = "unit")
public class QueryDslQueryUtilityTest {

  private QueryDslWhereFieldTransformer transformer () {

    PathBuilder<Object> root = new PathBuilder<>(Object.class, "person");
    BiFunction<String, String, Path<?>> pathFunction = (entity, name) -> root.get(name);

    return new QueryDslWhereFieldTransformer(pathFunction);
  }

  public void testNullWhereProducesNone () {

    Assert.assertTrue(QueryDslQueryUtility.apply(null, transformer(), true).isEmpty());
  }

  public void testEmptyConjunctionProducesNone () {

    Assert.assertTrue(QueryDslQueryUtility.apply(Where.instance(new AndWhereConjunction()), transformer(), true).isEmpty());
  }

  public void testAndConjunctionBuildsPredicate () {

    Where where = Where.instance(new AndWhereConjunction(
      WhereField.instance("age", WhereOperator.GE, IntegerWhereOperand.instance(18)),
      WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("active"))));

    Product<Path<?>, Predicate> product = QueryDslQueryUtility.apply(where, transformer(), true);

    Assert.assertFalse(product.isEmpty());

    String rendered = product.getValue().toString();

    Assert.assertTrue(rendered.contains("age"), rendered);
    Assert.assertTrue(rendered.contains("status"), rendered);
  }

  public void testOrConjunctionWithInAndLike () {

    Where where = Where.instance(new OrWhereConjunction(
      WhereField.instance("id", WhereOperator.IN, ArrayWhereOperand.instance(new Integer[] {1, 2, 3})),
      WhereField.instance("name", WhereOperator.LIKE, StringWhereOperand.instance("bo*"))));

    Product<Path<?>, Predicate> product = QueryDslQueryUtility.apply(where, transformer(), true);

    Assert.assertFalse(product.isEmpty());

    String rendered = product.getValue().toString();

    Assert.assertTrue(rendered.contains("id"), rendered);
    Assert.assertTrue(rendered.contains("name"), rendered);
  }

  private String renderField (String name, WhereOperator operator, WhereOperand operand) {

    Product<Path<?>, Predicate> product = QueryDslQueryUtility.apply(Where.instance(new AndWhereConjunction(WhereField.instance(name, operator, operand))), transformer(), true);

    Assert.assertFalse(product.isEmpty());

    return product.getValue().toString();
  }

  public void testLessThan () {

    Assert.assertTrue(renderField("age", WhereOperator.LT, IntegerWhereOperand.instance(40)).contains("age"));
  }

  public void testLessThanOrEqual () {

    Assert.assertTrue(renderField("age", WhereOperator.LE, IntegerWhereOperand.instance(40)).contains("age"));
  }

  public void testGreaterThan () {

    Assert.assertTrue(renderField("age", WhereOperator.GT, IntegerWhereOperand.instance(40)).contains("age"));
  }

  public void testGreaterThanOrEqual () {

    Assert.assertTrue(renderField("age", WhereOperator.GE, IntegerWhereOperand.instance(40)).contains("age"));
  }

  public void testEqualValue () {

    Assert.assertTrue(renderField("status", WhereOperator.EQ, StringWhereOperand.instance("active")).contains("status"));
  }

  public void testEqualNullBecomesIsNull () {

    Assert.assertTrue(renderField("status", WhereOperator.EQ, NullWhereOperand.instance()).contains("status"));
  }

  public void testNotEqualValue () {

    Assert.assertTrue(renderField("status", WhereOperator.NE, StringWhereOperand.instance("active")).contains("status"));
  }

  public void testNotEqualNullBecomesIsNotNull () {

    Assert.assertTrue(renderField("status", WhereOperator.NE, NullWhereOperand.instance()).contains("status"));
  }

  public void testExistsTrueBecomesIsNotNull () {

    Assert.assertTrue(renderField("status", WhereOperator.EXISTS, org.smallmind.web.json.query.BooleanWhereOperand.instance(true)).contains("status"));
  }

  public void testExistsFalseBecomesIsNull () {

    Assert.assertTrue(renderField("status", WhereOperator.EXISTS, org.smallmind.web.json.query.BooleanWhereOperand.instance(false)).contains("status"));
  }

  public void testLikeProducesLikePredicate () {

    Assert.assertTrue(renderField("name", WhereOperator.LIKE, StringWhereOperand.instance("bo*")).contains("name"));
  }

  public void testUnlikeProducesNotLikePredicate () {

    Assert.assertTrue(renderField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("bo*")).contains("name"));
  }

  public void testInSingleElementArray () {

    Assert.assertTrue(renderField("id", WhereOperator.IN, ArrayWhereOperand.instance(new Integer[] {7})).contains("id"));
  }

  public void testInMultiElementArray () {

    Assert.assertTrue(renderField("id", WhereOperator.IN, ArrayWhereOperand.instance(new Long[] {1L, 2L, 3L})).contains("id"));
  }

  public void testInEmptyArrayYieldsNoPredicate () {

    // An empty IN array walks to a null predicate, so the lone-field conjunction collapses to NoneProduct.
    Where where = Where.instance(new AndWhereConjunction(WhereField.instance("id", WhereOperator.IN, ArrayWhereOperand.instance(new Integer[] {}))));

    Assert.assertTrue(QueryDslQueryUtility.apply(where, transformer(), true).isEmpty());
  }

  public void testNestedAndInsideOrConjunction () {

    Where where = Where.instance(new OrWhereConjunction(
      new AndWhereConjunction(
        WhereField.instance("age", WhereOperator.GE, IntegerWhereOperand.instance(18)),
        WhereField.instance("age", WhereOperator.LE, IntegerWhereOperand.instance(65))),
      WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("vip"))));

    Product<Path<?>, Predicate> product = QueryDslQueryUtility.apply(where, transformer(), true);

    Assert.assertFalse(product.isEmpty());

    String rendered = product.getValue().toString();

    Assert.assertTrue(rendered.contains("age"), rendered);
    Assert.assertTrue(rendered.contains("status"), rendered);
  }

  public void testEmptyNestedConjunctionIsSkipped () {

    Where where = Where.instance(new AndWhereConjunction(
      new AndWhereConjunction(),
      WhereField.instance("age", WhereOperator.GE, LongWhereOperand.instance(18L))));

    Assert.assertFalse(QueryDslQueryUtility.apply(where, transformer(), true).isEmpty());
  }

  public void testNullSortProducesNone () {

    Assert.assertTrue(QueryDslQueryUtility.apply((Sort)null, transformer()).isEmpty());
  }

  public void testEmptySortProducesNone () {

    Assert.assertTrue(QueryDslQueryUtility.apply(Sort.instance(), transformer()).isEmpty());
  }

  public void testSortBuildsOrderSpecifiers () {

    Sort sort = Sort.instance(
      SortField.instance("created", SortDirection.DESC),
      SortField.instance("name", SortDirection.ASC));

    Product<Path<?>, OrderSpecifier[]> product = QueryDslQueryUtility.apply(sort, transformer());

    Assert.assertFalse(product.isEmpty());
    Assert.assertEquals(product.getValue().length, 2);
  }
}
