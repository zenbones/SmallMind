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
public class StringUtilityTest {

  public void testToDisplayCaseTreatsSourceAsSingleWord () {

    Assert.assertEquals(StringUtility.toDisplayCase("foo"), "Foo");
    Assert.assertEquals(StringUtility.toDisplayCase("FOOBAR"), "Foobar");
  }

  public void testToDisplayCaseWithMarkerReplacesMarkerWithSpace () {

    Assert.assertEquals(StringUtility.toDisplayCase("hello_world", '_'), "Hello World");
    Assert.assertEquals(StringUtility.toDisplayCase("ONE_TWO_THREE", '_'), "One Two Three");
  }

  public void testToCamelCaseDefaultsToUpperCamel () {

    Assert.assertEquals(StringUtility.toCamelCase("foo_bar", '_'), "FooBar");
  }

  public void testToCamelCaseLowerFirstCharacter () {

    Assert.assertEquals(StringUtility.toCamelCase("foo_bar", '_', false), "fooBar");
  }

  public void testToCamelCaseLowersInternalCharacters () {

    Assert.assertEquals(StringUtility.toCamelCase("FOO_BAR", '_'), "FooBar");
  }

  public void testToStaticFieldNameProducesUnderscoredUpperCase () {

    Assert.assertEquals(StringUtility.toStaticFieldName("foo bar", ' '), "FOO_BAR");
    Assert.assertEquals(StringUtility.toStaticFieldName("Foo.Bar.Baz", '.'), "FOO_BAR_BAZ");
  }

  public void testTrimWithElipsesPreservesTextWithinLimit () {

    Assert.assertEquals(StringUtility.trimWithElipses("hello", 10), "hello");
  }

  public void testTrimWithElipsesAppendsEllipsisWhenLonger () {

    Assert.assertEquals(StringUtility.trimWithElipses("this is too long", 10), "this is...");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testTrimWithElipsesRejectsShortText () {

    StringUtility.trimWithElipses("abc", 10);
  }

  public void testIsJavaIdentifierAcceptsValidIdentifiers () {

    Assert.assertTrue(StringUtility.isJavaIdentifier("foo"));
    Assert.assertTrue(StringUtility.isJavaIdentifier("_bar"));
    Assert.assertTrue(StringUtility.isJavaIdentifier("$baz"));
    Assert.assertTrue(StringUtility.isJavaIdentifier("name123"));
  }

  public void testIsJavaIdentifierRejectsLeadingDigit () {

    Assert.assertFalse(StringUtility.isJavaIdentifier("1abc"));
  }

  public void testIsJavaIdentifierRejectsEmbeddedPunctuation () {

    Assert.assertFalse(StringUtility.isJavaIdentifier("foo-bar"));
    Assert.assertFalse(StringUtility.isJavaIdentifier("foo bar"));
  }

  public void testHasNextDetectsMatchAtPosition () {

    Assert.assertTrue(StringUtility.hasNext("hello world", "world", 6));
    Assert.assertFalse(StringUtility.hasNext("hello world", "world", 0));
    Assert.assertTrue(StringUtility.hasNext("hello", "hello", 0));
  }

  public void testHasNextReturnsFalseWhenMatchRunsPastEnd () {

    Assert.assertFalse(StringUtility.hasNext("abc", "abcd", 0));
  }
}
