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
package org.smallmind.nutsnbolts.io;

import java.util.regex.Pattern;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class RegExTranslatorTest {

  public void testLiteralCharactersPassThroughUnchanged () {

    Assert.assertEquals(RegExTranslator.translate("abc"), "abc");
  }

  public void testDotIsEscapedAsLiteral () {

    Assert.assertEquals(RegExTranslator.translate("a.b"), "a\\.b");
  }

  public void testSingleStarMatchesAnyExceptSlash () {

    Pattern pattern = Pattern.compile(RegExTranslator.translate("*.txt"));

    Assert.assertTrue(pattern.matcher("foo.txt").matches());
    Assert.assertTrue(pattern.matcher(".txt").matches());
    Assert.assertFalse(pattern.matcher("dir/foo.txt").matches());
  }

  public void testDoubleStarMatchesAnyIncludingSlash () {

    Pattern pattern = Pattern.compile(RegExTranslator.translate("**.txt"));

    Assert.assertTrue(pattern.matcher("foo.txt").matches());
    Assert.assertTrue(pattern.matcher("dir/sub/foo.txt").matches());
  }

  public void testQuestionMatchesSingleCharExceptSlash () {

    Pattern pattern = Pattern.compile(RegExTranslator.translate("foo?.txt"));

    Assert.assertTrue(pattern.matcher("foox.txt").matches());
    Assert.assertTrue(pattern.matcher("foo.txt").matches());
    Assert.assertFalse(pattern.matcher("foo/.txt").matches());
  }

  public void testTrailingStarMatchesRemainder () {

    Pattern pattern = Pattern.compile(RegExTranslator.translate("foo*"));

    Assert.assertTrue(pattern.matcher("foo").matches());
    Assert.assertTrue(pattern.matcher("foobar").matches());
    Assert.assertFalse(pattern.matcher("foo/bar").matches());
  }

  public void testDollarSignIsEscapedAsLiteral () {

    Assert.assertEquals(RegExTranslator.translate("$"), "\\$");
    Assert.assertEquals(RegExTranslator.translate("a$b"), "a\\$b");
  }

  public void testDollarSignMatchesLiteralCharacter () {

    Pattern pattern = Pattern.compile(RegExTranslator.translate("price-$"));

    Assert.assertTrue(pattern.matcher("price-$").matches());
    Assert.assertFalse(pattern.matcher("price-").matches());
  }

  public void testDollarAndDotAreEscapedIndependently () {

    Assert.assertEquals(RegExTranslator.translate("$.txt"), "\\$\\.txt");
  }
}
