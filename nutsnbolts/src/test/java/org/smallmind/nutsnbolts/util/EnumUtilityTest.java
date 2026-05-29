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
public class EnumUtilityTest {

  public void testSimpleLowercaseTextIsUppercased () {

    Assert.assertEquals(EnumUtility.toEnumName("hello"), "HELLO");
  }

  public void testWhitespaceBecomesUnderscoreSeparator () {

    Assert.assertEquals(EnumUtility.toEnumName("hello world"), "HELLO_WORLD");
  }

  public void testExistingUnderscoresArePreserved () {

    Assert.assertEquals(EnumUtility.toEnumName("HELLO_WORLD"), "HELLO_WORLD");
  }

  public void testConsecutiveUnderscoresAreCollapsed () {

    Assert.assertEquals(EnumUtility.toEnumName("HELLO__WORLD"), "HELLO_WORLD");
  }

  public void testNonLetterNonDigitCharactersAreTreatedAsSeparators () {

    Assert.assertEquals(EnumUtility.toEnumName("hello-world"), "HELLO_WORLD");
  }

  public void testDigitGetsUnderscorePrefixByDefault () {

    Assert.assertEquals(EnumUtility.toEnumName("v2"), "V_2");
  }

  public void testDigitWithoutPrefixOptionRunsTogether () {

    Assert.assertEquals(EnumUtility.toEnumName("v2", false), "V2");
  }

  public void testTrailingPunctuationDoesNotProduceDanglingUnderscore () {

    Assert.assertEquals(EnumUtility.toEnumName("hello-world!"), "HELLO_WORLD");
    Assert.assertEquals(EnumUtility.toEnumName("hello-"), "HELLO");
    Assert.assertEquals(EnumUtility.toEnumName("hello!!!"), "HELLO");
  }

  public void testLeadingPunctuationDoesNotProduceLeadingUnderscore () {

    Assert.assertEquals(EnumUtility.toEnumName("-hello"), "HELLO");
    Assert.assertEquals(EnumUtility.toEnumName("_hello"), "HELLO");
    Assert.assertEquals(EnumUtility.toEnumName("!!!hello"), "HELLO");
  }

  public void testLeadingWhitespaceDoesNotProduceLeadingUnderscore () {

    Assert.assertEquals(EnumUtility.toEnumName("  hello"), "HELLO");
  }

  public void testCamelCaseBoundaryProducesUnderscore () {

    Assert.assertEquals(EnumUtility.toEnumName("helloWorld"), "HELLO_WORLD");
  }

  public void testAcronymFollowedByCamelCaseSplitsAtLastUpper () {

    Assert.assertEquals(EnumUtility.toEnumName("HTTPServer"), "HTTP_SERVER");
  }
}
