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

import java.time.LocalDateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the in-memory evaluation logic of every {@link WhereOperator}. Throughout, the first
 * operand is the pattern/threshold and the second is the input value, matching the operator contract.
 */
@Test(groups = "unit")
public class WhereOperatorTest {

  public void testLessThanNumeric () {

    Assert.assertTrue(WhereOperator.LT.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(3)));
    Assert.assertFalse(WhereOperator.LT.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(5)));
    Assert.assertFalse(WhereOperator.LT.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(7)));
  }

  public void testLessThanOrEqualNumeric () {

    Assert.assertTrue(WhereOperator.LE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(5)));
    Assert.assertTrue(WhereOperator.LE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(3)));
    Assert.assertFalse(WhereOperator.LE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(7)));
  }

  public void testGreaterThanNumeric () {

    Assert.assertTrue(WhereOperator.GT.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(7)));
    Assert.assertFalse(WhereOperator.GT.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(5)));
    Assert.assertFalse(WhereOperator.GT.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(3)));
  }

  public void testGreaterThanOrEqualNumeric () {

    Assert.assertTrue(WhereOperator.GE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(5)));
    Assert.assertTrue(WhereOperator.GE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(7)));
    Assert.assertFalse(WhereOperator.GE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(3)));
  }

  public void testOrderingMixesIntegerAndDouble () {

    Assert.assertTrue(WhereOperator.LT.isTrue(DoubleWhereOperand.instance(5.5D), IntegerWhereOperand.instance(5)));
    Assert.assertTrue(WhereOperator.GT.isTrue(IntegerWhereOperand.instance(5), DoubleWhereOperand.instance(5.5D)));
  }

  public void testOrderingOnDates () {

    LocalDateTime earlier = LocalDateTime.of(2020, 1, 1, 0, 0);
    LocalDateTime later = LocalDateTime.of(2021, 1, 1, 0, 0);

    Assert.assertTrue(WhereOperator.LT.isTrue(DateWhereOperand.instance(later), DateWhereOperand.instance(earlier)));
    Assert.assertTrue(WhereOperator.GT.isTrue(DateWhereOperand.instance(earlier), DateWhereOperand.instance(later)));
    Assert.assertTrue(WhereOperator.GE.isTrue(DateWhereOperand.instance(earlier), DateWhereOperand.instance(earlier)));
    Assert.assertTrue(WhereOperator.LE.isTrue(DateWhereOperand.instance(later), DateWhereOperand.instance(later)));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testOrderingRejectsStrings () {

    WhereOperator.LT.isTrue(StringWhereOperand.instance("a"), StringWhereOperand.instance("b"));
  }

  public void testEqualsAcrossTypes () {

    Assert.assertTrue(WhereOperator.EQ.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(5)));
    Assert.assertFalse(WhereOperator.EQ.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(6)));
    Assert.assertTrue(WhereOperator.EQ.isTrue(StringWhereOperand.instance("x"), StringWhereOperand.instance("x")));
    Assert.assertFalse(WhereOperator.EQ.isTrue(StringWhereOperand.instance("x"), StringWhereOperand.instance("y")));
    Assert.assertTrue(WhereOperator.EQ.isTrue(BooleanWhereOperand.instance(true), BooleanWhereOperand.instance(true)));
  }

  public void testEqualsHandlesNulls () {

    Assert.assertTrue(WhereOperator.EQ.isTrue(NullWhereOperand.instance(), NullWhereOperand.instance()));
    Assert.assertFalse(WhereOperator.EQ.isTrue(NullWhereOperand.instance(), StringWhereOperand.instance("x")));
    Assert.assertFalse(WhereOperator.EQ.isTrue(StringWhereOperand.instance("x"), NullWhereOperand.instance()));
  }

  public void testNotEqualsHandlesNulls () {

    Assert.assertFalse(WhereOperator.NE.isTrue(NullWhereOperand.instance(), NullWhereOperand.instance()));
    Assert.assertTrue(WhereOperator.NE.isTrue(NullWhereOperand.instance(), StringWhereOperand.instance("x")));
    Assert.assertTrue(WhereOperator.NE.isTrue(StringWhereOperand.instance("x"), NullWhereOperand.instance()));
    Assert.assertTrue(WhereOperator.NE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(6)));
    Assert.assertFalse(WhereOperator.NE.isTrue(IntegerWhereOperand.instance(5), IntegerWhereOperand.instance(5)));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testEqualsRejectsArrayOperand () {

    WhereOperator.EQ.isTrue(ArrayWhereOperand.instance(new Integer[] {1, 2}), IntegerWhereOperand.instance(1));
  }

  public void testExists () {

    Assert.assertTrue(WhereOperator.EXISTS.isTrue(BooleanWhereOperand.instance(true), StringWhereOperand.instance("present")));
    Assert.assertFalse(WhereOperator.EXISTS.isTrue(BooleanWhereOperand.instance(true), NullWhereOperand.instance()));
    Assert.assertTrue(WhereOperator.EXISTS.isTrue(BooleanWhereOperand.instance(false), NullWhereOperand.instance()));
    Assert.assertFalse(WhereOperator.EXISTS.isTrue(BooleanWhereOperand.instance(false), StringWhereOperand.instance("present")));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testExistsRequiresBooleanOperand () {

    WhereOperator.EXISTS.isTrue(StringWhereOperand.instance("nope"), StringWhereOperand.instance("present"));
  }

  public void testLikeShortPatterns () {

    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance(""), StringWhereOperand.instance("")));
    Assert.assertFalse(WhereOperator.LIKE.isTrue(StringWhereOperand.instance(""), StringWhereOperand.instance("x")));
    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("*"), StringWhereOperand.instance("anything")));
    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("a"), StringWhereOperand.instance("a")));
    Assert.assertFalse(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("a"), StringWhereOperand.instance("b")));
    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("*x"), StringWhereOperand.instance("aax")));
    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("x*"), StringWhereOperand.instance("xyz")));
  }

  public void testLikeWildcardPatterns () {

    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("*bc*"), StringWhereOperand.instance("abcd")));
    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("*cd"), StringWhereOperand.instance("abcd")));
    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("ab*"), StringWhereOperand.instance("abcd")));
    Assert.assertTrue(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("abc"), StringWhereOperand.instance("abc")));
    Assert.assertFalse(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("abc"), StringWhereOperand.instance("abx")));
  }

  public void testLikeAgainstNullInputIsFalse () {

    Assert.assertFalse(WhereOperator.LIKE.isTrue(StringWhereOperand.instance("abc"), NullWhereOperand.instance()));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testLikeRejectsNonTerminalWildcard () {

    WhereOperator.LIKE.isTrue(StringWhereOperand.instance("a*d"), StringWhereOperand.instance("axd"));
  }

  public void testUnlikeIsNegationOfLike () {

    Assert.assertFalse(WhereOperator.UNLIKE.isTrue(StringWhereOperand.instance("ab*"), StringWhereOperand.instance("abcd")));
    Assert.assertTrue(WhereOperator.UNLIKE.isTrue(StringWhereOperand.instance("ab*"), StringWhereOperand.instance("zzz")));
  }

  public void testInNumericMembership () {

    Assert.assertTrue(WhereOperator.IN.isTrue(ArrayWhereOperand.instance(new Integer[] {1, 2, 3}), IntegerWhereOperand.instance(2)));
    Assert.assertFalse(WhereOperator.IN.isTrue(ArrayWhereOperand.instance(new Integer[] {1, 2, 3}), IntegerWhereOperand.instance(9)));
  }

  public void testInStringMembership () {

    Assert.assertTrue(WhereOperator.IN.isTrue(ArrayWhereOperand.instance(new String[] {"a", "b"}), StringWhereOperand.instance("b")));
    Assert.assertFalse(WhereOperator.IN.isTrue(ArrayWhereOperand.instance(new String[] {"a", "b"}), StringWhereOperand.instance("c")));
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testInRequiresArrayOperand () {

    WhereOperator.IN.isTrue(IntegerWhereOperand.instance(2), IntegerWhereOperand.instance(2));
  }
}
