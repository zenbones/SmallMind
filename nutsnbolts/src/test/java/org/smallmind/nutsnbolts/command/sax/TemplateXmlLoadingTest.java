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
package org.smallmind.nutsnbolts.command.sax;

import java.io.InputStream;
import org.smallmind.nutsnbolts.command.CommandLineException;
import org.smallmind.nutsnbolts.command.CommandLineParser;
import org.smallmind.nutsnbolts.command.OptionSet;
import org.smallmind.nutsnbolts.command.template.Template;
import org.smallmind.nutsnbolts.xml.XMLEntityResolver;
import org.smallmind.nutsnbolts.xml.sax.ExtensibleSAXParser;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

@Test(groups = "integration")
public class TemplateXmlLoadingTest {

  private static Template loadTemplateNonValidating (Class<?> entryClass)
    throws Exception {

    Template template = new Template(entryClass);
    OptionsDocumentExtender extender = new OptionsDocumentExtender(template);

    try (InputStream stream = TemplateXmlLoadingTest.class.getClassLoader()
                                .getResourceAsStream(entryClass.getCanonicalName().replace('.', '/') + ".arguments.xml")) {

      Assert.assertNotNull(stream, "Arguments fixture missing from classpath");
      ExtensibleSAXParser.parse(extender, new InputSource(stream), XMLEntityResolver.getInstance(), false);
    }

    return template;
  }

  public void testTemplateFromXmlParsesEveryArgumentType ()
    throws Exception {

    Template template = loadTemplateNonValidating(SampleApp.class);

    OptionSet result = CommandLineParser.parseCommands(template, new String[] {
      "--input", "/etc/app.conf",
      "--verbose",
      "--files", "a.txt", "b.txt",
      "--level", "medium"
    });

    Assert.assertEquals(result.getArgument("input"), "/etc/app.conf");
    Assert.assertTrue(result.containsOption("verbose"));

    String[] files = result.getArguments("files");
    Assert.assertEquals(files.length, 2);
    Assert.assertEquals(files[0], "a.txt");
    Assert.assertEquals(files[1], "b.txt");

    Assert.assertEquals(result.getArgument("level"), "medium");
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testRequiredOptionFromXmlEnforced ()
    throws Exception {

    Template template = loadTemplateNonValidating(SampleApp.class);

    CommandLineParser.parseCommands(template, new String[] {"--verbose"});
  }

  @Test(expectedExceptions = CommandLineException.class)
  public void testEnumeratedArgumentFromXmlRejectsOutOfRange ()
    throws Exception {

    Template template = loadTemplateNonValidating(SampleApp.class);

    CommandLineParser.parseCommands(template, new String[] {"--input", "/x", "--level", "extreme"});
  }
}
