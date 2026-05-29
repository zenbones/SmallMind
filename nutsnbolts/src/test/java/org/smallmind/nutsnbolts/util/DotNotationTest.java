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
package org.smallmind.nutsnbolts.util;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DotNotationTest {

  public void testLiteralPatternMatchesExactName ()
    throws DotNotationException {

    DotNotation notation = new DotNotation("foo.bar");

    Assert.assertTrue(notation.getPattern().matcher("foo.bar").matches());
    Assert.assertFalse(notation.getPattern().matcher("foo.baz").matches());
  }

  public void testStarWildcardMatchesAcrossSegments ()
    throws DotNotationException {

    DotNotation notation = new DotNotation("*.bar");

    Assert.assertTrue(notation.getPattern().matcher("foo.bar").matches());
    Assert.assertTrue(notation.getPattern().matcher("foo.baz.bar").matches());
    Assert.assertFalse(notation.getPattern().matcher("foo.baz").matches());
  }

  public void testQuestionWildcardMatchesSingleSegment ()
    throws DotNotationException {

    DotNotation notation = new DotNotation("?.bar");

    Assert.assertTrue(notation.getPattern().matcher("foo.bar").matches());
    Assert.assertFalse(notation.getPattern().matcher("foo.baz.bar").matches());
  }

  public void testValueAccumulatesAcrossSegments ()
    throws DotNotationException {

    Assert.assertTrue(new DotNotation("foo").getValue() > 0);
    Assert.assertTrue(new DotNotation("foo.bar").getValue() > new DotNotation("foo").getValue());
  }

  public void testCalculateValueReturnsInitialOnNoMatch ()
    throws DotNotationException {

    DotNotation notation = new DotNotation("foo");

    Assert.assertEquals(notation.calculateValue("bar", -1), -1);
    Assert.assertTrue(notation.calculateValue("foo", -1) > 0);
  }

  public void testUninitializedNotationReturnsInitialFromCalculateValue () {

    Assert.assertEquals(new DotNotation().calculateValue("anything", 42), 42);
  }

  @Test(expectedExceptions = DotNotationException.class)
  public void testLeadingDotIsRejected ()
    throws DotNotationException {

    new DotNotation(".foo");
  }

  @Test(expectedExceptions = DotNotationException.class)
  public void testTrailingDotIsRejected ()
    throws DotNotationException {

    new DotNotation("foo.");
  }

  @Test(expectedExceptions = DotNotationException.class)
  public void testEmptyComponentIsRejected ()
    throws DotNotationException {

    new DotNotation("foo..bar");
  }

  @Test(expectedExceptions = DotNotationException.class)
  public void testWildcardMidSegmentIsRejected ()
    throws DotNotationException {

    new DotNotation("foo*");
  }

  @Test(expectedExceptions = DotNotationException.class)
  public void testRedundantWildcardCombinationIsRejected ()
    throws DotNotationException {

    new DotNotation("**");
  }

  @Test(expectedExceptions = DotNotationException.class)
  public void testInvalidIdentifierCharacterIsRejected ()
    throws DotNotationException {

    new DotNotation("foo-bar");
  }
}
