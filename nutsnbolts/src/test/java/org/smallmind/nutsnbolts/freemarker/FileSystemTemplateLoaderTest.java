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
package org.smallmind.nutsnbolts.freemarker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class FileSystemTemplateLoaderTest {

  private Path templateDir;
  private Path templateFile;

  @BeforeMethod
  public void setUp ()
    throws IOException {

    templateDir = Files.createTempDirectory("fs-template-");
    templateFile = templateDir.resolve("hello.ftl");
    Files.writeString(templateFile, "Hello, ${name}!");
  }

  @AfterMethod
  public void tearDown ()
    throws IOException {

    Files.deleteIfExists(templateFile);
    Files.deleteIfExists(templateDir);
  }

  public void testTemplateIsFoundRelativeToBasePath ()
    throws IOException {

    FileSystemTemplateLoader loader = new FileSystemTemplateLoader(templateDir);
    Object source = loader.findTemplateSource("hello.ftl");

    try (Reader reader = loader.getReader(source, StandardCharsets.UTF_8.name())) {

      Assert.assertEquals(new BufferedReader(reader).readLine(), "Hello, ${name}!");
    } finally {
      loader.closeTemplateSource(source);
    }
  }

  public void testLastModifiedTimestampIsPositive ()
    throws IOException {

    FileSystemTemplateLoader loader = new FileSystemTemplateLoader(templateDir);
    Object source = loader.findTemplateSource("hello.ftl");

    Assert.assertTrue(loader.getLastModified(source) > 0L);
  }

  public void testSetBasePathChangesResolutionRoot ()
    throws IOException {

    FileSystemTemplateLoader loader = new FileSystemTemplateLoader();

    loader.setBasePath(templateDir);

    Object source = loader.findTemplateSource("hello.ftl");

    try (Reader reader = loader.getReader(source, StandardCharsets.UTF_8.name())) {

      Assert.assertNotNull(reader);
    } finally {
      loader.closeTemplateSource(source);
    }
  }

  public void testLastModifiedOfMissingFileReturnsMinusOne () {

    FileSystemTemplateLoader loader = new FileSystemTemplateLoader(templateDir);
    Object source = loader.findTemplateSource("missing.ftl");

    Assert.assertEquals(loader.getLastModified(source), -1L);
  }
}
