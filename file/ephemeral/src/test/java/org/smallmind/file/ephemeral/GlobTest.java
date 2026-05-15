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
package org.smallmind.file.ephemeral;

import java.util.regex.PatternSyntaxException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class GlobTest {

  private void assertMatches (String glob, String input) {

    Assert.assertTrue(Glob.toRegexPattern('/', glob).matcher(input).matches(), "glob '" + glob + "' should match '" + input + "'");
  }

  private void assertNotMatches (String glob, String input) {

    Assert.assertFalse(Glob.toRegexPattern('/', glob).matcher(input).matches(), "glob '" + glob + "' should not match '" + input + "'");
  }

  public void testLiteralMatch () {

    assertMatches("foo.txt", "foo.txt");
    assertNotMatches("foo.txt", "bar.txt");
  }

  public void testStarDoesNotCrossSeparator () {

    assertMatches("*.txt", "foo.txt");
    assertNotMatches("*.txt", "a/b.txt");
  }

  public void testDoubleStarCrossesSeparator () {

    assertMatches("**.txt", "a/b.txt");
    assertMatches("**.txt", "foo.txt");
  }

  public void testQuestionMark () {

    assertMatches("?bc", "abc");
    assertMatches("?bc", "xbc");
    assertNotMatches("?bc", "abcd");
    assertNotMatches("?bc", "/bc");
  }

  public void testCharacterClass () {

    assertMatches("[abc]", "a");
    assertMatches("[abc]", "b");
    assertNotMatches("[abc]", "d");
  }

  public void testCharacterRange () {

    assertMatches("[a-c]", "b");
    assertNotMatches("[a-c]", "d");
  }

  public void testNegatedCharacterClass () {

    assertMatches("[!abc]", "d");
    assertNotMatches("[!abc]", "a");
  }

  public void testAlternationGroup () {

    assertMatches("{cat,dog,bird}", "cat");
    assertMatches("{cat,dog,bird}", "dog");
    assertMatches("{cat,dog,bird}", "bird");
    assertNotMatches("{cat,dog,bird}", "fish");
  }

  public void testEscape () {

    assertMatches("\\*", "*");
    assertNotMatches("\\*", "a");
  }

  public void testRegexMetaIsLiteral () {

    assertMatches("foo(bar)", "foo(bar)");
    assertNotMatches("foo(bar)", "foobar");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testUnterminatedEscape () {

    Glob.toRegexPattern('/', "abc\\");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testUnterminatedCharacterClass () {

    Glob.toRegexPattern('/', "abc[");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testUnterminatedGroup () {

    Glob.toRegexPattern('/', "{a,b");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testNestedGroupRejected () {

    Glob.toRegexPattern('/', "{a,{b,c}}");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testSeparatorInCharacterClassRejected () {

    Glob.toRegexPattern('/', "[a/b]");
  }
}
