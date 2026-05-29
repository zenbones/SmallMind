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

import java.util.HashSet;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AnsiColorTest {

  private static final char ESC = (char)0x1B;

  public void testEveryConstantBeginsWithEscapeSequencePrefix () {

    for (AnsiColor color : AnsiColor.values()) {
      String code = color.getCode();
      Assert.assertEquals(code.charAt(0), ESC, color.name() + " did not start with ESC");
      Assert.assertEquals(code.charAt(1), '[', color.name() + " did not have CSI introducer");
      Assert.assertTrue(code.endsWith("m"), color.name() + " did not end with m");
    }
  }

  public void testAllCodesAreUnique () {

    Set<String> codes = new HashSet<>();

    for (AnsiColor color : AnsiColor.values()) {
      Assert.assertTrue(codes.add(color.getCode()), "Duplicate code for " + color.name());
    }
  }

  public void testKnownConstantsMatchExpectedSequences () {

    Assert.assertEquals(AnsiColor.RED.getCode(), ESC + "[31m");
    Assert.assertEquals(AnsiColor.BRIGHT_RED.getCode(), ESC + "[91m");
    Assert.assertEquals(AnsiColor.RESET.getCode(), ESC + "[0m");
    Assert.assertEquals(AnsiColor.DEFAULT.getCode(), ESC + "[39m");
  }
}
