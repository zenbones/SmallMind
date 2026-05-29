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

import org.smallmind.nutsnbolts.command.template.EnumeratedArgument;
import org.smallmind.nutsnbolts.command.template.ListArgument;
import org.smallmind.nutsnbolts.command.template.MultipleArgument;
import org.smallmind.nutsnbolts.command.template.NoneArgument;
import org.smallmind.nutsnbolts.command.template.Option;
import org.smallmind.nutsnbolts.command.template.SingleArgument;
import org.smallmind.nutsnbolts.command.template.Template;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CommandLineParserTest {

  public void testLongOptionWithSingleArgumentIsParsed ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("input", 'i', true, new SingleArgument("path")));

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {"--input", "/etc/app.conf"});

    Assert.assertTrue(result.containsOption("input"));
    Assert.assertEquals(result.getArgument("input"), "/etc/app.conf");
  }

  public void testShortFlagWithoutArgumentIsParsed ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("verbose", 'v', false, NoneArgument.instance()));

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {"-v"});

    Assert.assertTrue(result.containsOption("verbose"));
  }

  public void testMultipleArgumentAcceptsSecondInvocation ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("define", 'D', false, new MultipleArgument("kv")));

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {"--define", "a=1", "--define", "b=2"});

    String[] arguments = result.getArguments("define");
    Assert.assertEquals(arguments.length, 2);
    Assert.assertEquals(arguments[0], "a=1");
    Assert.assertEquals(arguments[1], "b=2");
  }

  public void testListArgumentCollectsUntilDash ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("files", 'f', false, new ListArgument("paths")),
      new Option("verbose", 'v', false, NoneArgument.instance()));

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {"--files", "a.txt", "b.txt", "c.txt", "-v"});

    String[] files = result.getArguments("files");
    Assert.assertEquals(files.length, 3);
    Assert.assertEquals(files[0], "a.txt");
    Assert.assertEquals(files[2], "c.txt");
    Assert.assertTrue(result.containsOption("verbose"));
  }

  public void testEnumeratedArgumentMatchesAllowedValue ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("level", 'l', false, new EnumeratedArgument(new String[] {"low", "medium", "high"})));

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {"--level", "medium"});

    Assert.assertEquals(result.getArgument("level"), "medium");
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testEnumeratedRejectsValueOutsideAllowedSet ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("level", 'l', false, new EnumeratedArgument(new String[] {"low", "high"})));

    CommandLineParser.parseCommands(template, new String[] {"--level", "medium"});
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testUnknownLongOptionIsRejected ()
    throws CommandLineException {

    Template template = new Template("test", new Option("known", 'k', false, NoneArgument.instance()));

    CommandLineParser.parseCommands(template, new String[] {"--unknown"});
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testRequiredOptionMissingThrows ()
    throws CommandLineException {

    Template template = new Template("test", new Option("input", 'i', true, new SingleArgument("path")));

    CommandLineParser.parseCommands(template, new String[] {});
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testDependentOptionWithoutParentThrows ()
    throws CommandLineException {

    Option child = new Option("nested", 'n', false, NoneArgument.instance());
    Option parent = new Option("outer", 'o', false, NoneArgument.instance(), child);
    Template template = new Template("test", parent);

    CommandLineParser.parseCommands(template, new String[] {"--nested"});
  }

  public void testDependentOptionWithParentSucceeds ()
    throws CommandLineException {

    Option child = new Option("nested", 'n', false, NoneArgument.instance());
    Option parent = new Option("outer", 'o', false, NoneArgument.instance(), child);
    Template template = new Template("test", parent);

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {"--outer", "--nested"});

    Assert.assertTrue(result.containsOption("outer"));
    Assert.assertTrue(result.containsOption("nested"));
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testUndeclaredTrailingTokenRejectedByDefault ()
    throws CommandLineException {

    Template template = new Template("test", new Option("flag", 'f', false, NoneArgument.instance()));

    CommandLineParser.parseCommands(template, new String[] {"-f", "trailing"});
  }

  public void testAllowUndeclaredCollectsTrailingTokens ()
    throws CommandLineException {

    Template template = new Template("test", new Option("flag", 'f', false, NoneArgument.instance()));

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {"-f", "one", "two"}, true);

    Assert.assertTrue(result.containsOption("flag"));
    Assert.assertEquals(result.getRemaining(), new String[] {"one", "two"});
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testDuplicateNonMultipleOptionThrows ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("input", 'i', false, new SingleArgument("path")));

    CommandLineParser.parseCommands(template, new String[] {"--input", "a", "--input", "b"});
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testLongOptionRequiringValueWithoutValueThrows ()
    throws CommandLineException {

    Template template = new Template("test",
      new Option("input", 'i', false, new SingleArgument("path")));

    CommandLineParser.parseCommands(template, new String[] {"--input"});
  }
}
