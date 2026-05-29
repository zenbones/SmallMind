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
package org.smallmind.nutsnbolts.command;

import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OptionSetTest {

  public void testAddOptionIsIdempotent () {

    OptionSet set = new OptionSet();

    set.addOption("verbose");
    set.addOption("verbose");

    Assert.assertEquals(set.getOptions().length, 1);
    Assert.assertTrue(set.containsOption("verbose"));
  }

  public void testAddArgumentImplicitlyAddsOption () {

    OptionSet set = new OptionSet();

    set.addArgument("input", "/etc/x");

    Assert.assertTrue(set.containsOption("input"));
    Assert.assertEquals(set.getArgument("input"), "/etc/x");
  }

  public void testAddArgumentAppendsToExistingOption () {

    OptionSet set = new OptionSet();

    set.addArgument("define", "a=1");
    set.addArgument("define", "b=2");

    String[] all = set.getArguments("define");
    Assert.assertEquals(all.length, 2);
    Assert.assertEquals(all[0], "a=1");
    Assert.assertEquals(all[1], "b=2");
  }

  public void testGetArgumentReturnsNullForUnknownOption () {

    OptionSet set = new OptionSet();

    Assert.assertNull(set.getArgument("missing"));
    Assert.assertNull(set.getArgument('m'));
    Assert.assertNull(set.getArgument("missing", null));
  }

  public void testGetArgumentReturnsNullWhenOptionHasNoArguments () {

    OptionSet set = new OptionSet();

    set.addOption("verbose");

    Assert.assertNull(set.getArgument("verbose"));
  }

  public void testGetArgumentsReturnsNullForUnknownOption () {

    OptionSet set = new OptionSet();

    Assert.assertNull(set.getArguments("missing"));
    Assert.assertNull(set.getArguments('m'));
  }

  public void testFlagFallbackUsedWhenNameAbsent () {

    OptionSet set = new OptionSet();

    set.addArgument("v", "x");

    Assert.assertEquals(set.getArgument("verbose", 'v'), "x");
    Assert.assertTrue(set.containsOption("verbose", 'v'));
    Assert.assertEquals(set.getArguments("verbose", 'v').length, 1);
  }

  public void testNameTakesPrecedenceOverFlag () {

    OptionSet set = new OptionSet();

    set.addArgument("verbose", "from-name");
    set.addArgument("v", "from-flag");

    Assert.assertEquals(set.getArgument("verbose", 'v'), "from-name");
  }

  public void testRemainingPreservesInsertionOrder () {

    OptionSet set = new OptionSet();

    set.addRemaining("one");
    set.addRemaining("two");
    set.addRemaining("three");

    Assert.assertEquals(set.getRemaining(), new String[] {"one", "two", "three"});
  }

  public void testRemainingEmptyByDefault () {

    OptionSet set = new OptionSet();

    Assert.assertEquals(set.getRemaining().length, 0);
  }

  public void testToStringShowsBareOptionWithoutEquals () {

    OptionSet set = new OptionSet();

    set.addOption("verbose");

    Assert.assertEquals(set.toString(), "[verbose]");
  }

  public void testToStringQuotesSingleArgument () {

    OptionSet set = new OptionSet();

    set.addArgument("input", "/etc/x");

    Assert.assertEquals(set.toString(), "[input=\"/etc/x\"]");
  }

  public void testToStringBracketsMultipleArguments () {

    OptionSet set = new OptionSet();

    set.addArgument("define", "a=1");
    set.addArgument("define", "b=2");

    Assert.assertEquals(set.toString(), "[define=[\"a=1\", \"b=2\"]]");
  }

  public void testGetOptionsReturnsEveryAddedKey () {

    OptionSet set = new OptionSet();

    set.addOption("alpha");
    set.addArgument("beta", "x");

    String[] keys = set.getOptions();
    Arrays.sort(keys);

    Assert.assertEquals(keys, new String[] {"alpha", "beta"});
  }
}
