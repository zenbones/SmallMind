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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ExtensionFileFilterTest {

  private Path tempDir;
  private File matchingFile;
  private File nonMatchingFile;
  private File directory;

  @BeforeClass
  public void setUp ()
    throws Exception {

    tempDir = Files.createTempDirectory("ext-filter-test");
    matchingFile = Files.createFile(tempDir.resolve("doc.txt")).toFile();
    nonMatchingFile = Files.createFile(tempDir.resolve("img.png")).toFile();
    directory = Files.createDirectory(tempDir.resolve("subdir")).toFile();
  }

  @AfterClass
  public void tearDown ()
    throws Exception {

    Files.deleteIfExists(matchingFile.toPath());
    Files.deleteIfExists(nonMatchingFile.toPath());
    Files.deleteIfExists(directory.toPath());
    Files.deleteIfExists(tempDir);
  }

  public void testAcceptsFileWithRegisteredExtension () {

    ExtensionFileFilter filter = new ExtensionFileFilter("Text files", "txt");

    Assert.assertTrue(filter.accept(matchingFile));
    Assert.assertFalse(filter.accept(nonMatchingFile));
  }

  public void testAcceptsDirectoriesUnconditionally () {

    ExtensionFileFilter filter = new ExtensionFileFilter("Text files", "txt");

    Assert.assertTrue(filter.accept(directory));
  }

  public void testAdditionalExtensionRegistration () {

    ExtensionFileFilter filter = new ExtensionFileFilter("Text files", "txt");

    filter.addExtension("png");

    Assert.assertTrue(filter.accept(matchingFile));
    Assert.assertTrue(filter.accept(nonMatchingFile));
  }

  public void testDescriptionListsAllExtensions () {

    ExtensionFileFilter filter = new ExtensionFileFilter("Documents", "txt", "md");

    String description = filter.getDescription();

    Assert.assertTrue(description.startsWith("Documents"));
    Assert.assertTrue(description.contains("*.txt"));
    Assert.assertTrue(description.contains("*.md"));
  }

  public void testSetDescriptionReplacesText () {

    ExtensionFileFilter filter = new ExtensionFileFilter("old", "txt");

    filter.setDescription("new");
    Assert.assertTrue(filter.getDescription().startsWith("new"));
  }

  public void testGetExtensionReturnsFirstAdded () {

    ExtensionFileFilter filter = new ExtensionFileFilter("Documents", "txt", "md");

    Assert.assertEquals(filter.getExtension(), "txt");
  }
}
