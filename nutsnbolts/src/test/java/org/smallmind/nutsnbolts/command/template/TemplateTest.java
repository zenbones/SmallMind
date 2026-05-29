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
package org.smallmind.nutsnbolts.command.template;

import org.smallmind.nutsnbolts.command.CommandLineException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TemplateTest {

  public void testEntryClassConstructorTakesSimpleName () {

    Template template = new Template(TemplateTest.class);

    Assert.assertEquals(template.getShortName(), "TemplateTest");
    Assert.assertTrue(template.getRootOptionList().isEmpty());
  }

  public void testShortNameConstructorRegistersRootOptions ()
    throws CommandLineException {

    Option input = new Option("input", 'i', true, new SingleArgument("path"));
    Template template = new Template("test", input);

    Assert.assertEquals(template.getShortName(), "test");
    Assert.assertEquals(template.getRootOptionList().size(), 1);
    Assert.assertTrue(template.getOptionSet().contains(input));
  }

  public void testChildOptionsAreRegisteredInOptionSetButNotRootList ()
    throws CommandLineException {

    Option child = new Option("child", 'c', false, NoneArgument.instance());
    Option parent = new Option("parent", 'p', false, NoneArgument.instance(), child);
    Template template = new Template("test", parent);

    Assert.assertEquals(template.getRootOptionList().size(), 1);
    Assert.assertEquals(template.getOptionSet().size(), 2);
    Assert.assertTrue(template.getOptionSet().contains(child));
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testOptionWithoutNameOrFlagRejected ()
    throws CommandLineException {

    new Template("test", new Option(null, null, false, NoneArgument.instance()));
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testDuplicateNameRejected ()
    throws CommandLineException {

    new Template("test",
      new Option("name", 'a', false, NoneArgument.instance()),
      new Option("name", 'b', false, NoneArgument.instance()));
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testDuplicateFlagRejected ()
    throws CommandLineException {

    new Template("test",
      new Option("alpha", 'x', false, NoneArgument.instance()),
      new Option("beta", 'x', false, NoneArgument.instance()));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRootOptionListIsUnmodifiable ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("alpha", 'a', false, NoneArgument.instance()));

    template.getRootOptionList().clear();
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOptionSetIsUnmodifiable ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("alpha", 'a', false, NoneArgument.instance()));

    template.getOptionSet().clear();
  }

  public void testToStringShowsRequiredOptionInBrackets ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("input", 'i', true, new SingleArgument("path")));

    String output = template.toString();

    Assert.assertTrue(output.contains("--input"), "Expected --input in output: " + output);
    Assert.assertTrue(output.contains("[") && output.contains("]"), "Required option should appear in [...]: " + output);
    Assert.assertTrue(output.contains("path"), "Expected argument description: " + output);
  }

  public void testToStringShowsOptionalOptionInAngleBrackets ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("verbose", 'v', false, NoneArgument.instance()));

    String output = template.toString();

    Assert.assertTrue(output.contains("<") && output.contains(">"), "Optional option should appear in <...>: " + output);
  }
}
