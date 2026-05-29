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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class FileUtilityTest {

  private Path workingDir;

  @BeforeMethod
  public void setUp ()
    throws IOException {

    workingDir = Files.createTempDirectory("nutsnbolts-fileutil-");
  }

  @AfterMethod
  public void tearDown ()
    throws IOException {

    if (workingDir != null && Files.exists(workingDir)) {
      FileUtility.deleteTree(workingDir, true, false);
    }
  }

  public void testIsDirectoryEmptyOnEmptyDirectory ()
    throws IOException {

    Assert.assertTrue(FileUtility.isDirectoryEmpty(workingDir));
  }

  public void testIsDirectoryEmptyAfterFileCreated ()
    throws IOException {

    Files.writeString(workingDir.resolve("file.txt"), "data");

    Assert.assertFalse(FileUtility.isDirectoryEmpty(workingDir));
  }

  public void testCopyTreeReplicatesFiles ()
    throws IOException {

    Path source = Files.createDirectory(workingDir.resolve("src"));
    Path destination = workingDir.resolve("dst");

    Files.writeString(source.resolve("a.txt"), "alpha");
    Files.writeString(source.resolve("b.txt"), "beta");

    FileUtility.copyTree(source, destination, false);

    Assert.assertEquals(Files.readString(destination.resolve("a.txt")), "alpha");
    Assert.assertEquals(Files.readString(destination.resolve("b.txt")), "beta");
  }

  public void testCopyTreeRecursesIntoSubdirectories ()
    throws IOException {

    Path source = Files.createDirectory(workingDir.resolve("src"));
    Path subDir = Files.createDirectory(source.resolve("sub"));
    Path destination = workingDir.resolve("dst");

    Files.writeString(subDir.resolve("nested.txt"), "nested");

    FileUtility.copyTree(source, destination, false);

    Assert.assertEquals(Files.readString(destination.resolve("sub").resolve("nested.txt")), "nested");
  }

  public void testCopyTreeRespectsPathFilter ()
    throws IOException {

    Path source = Files.createDirectory(workingDir.resolve("src"));
    Path destination = workingDir.resolve("dst");

    Files.writeString(source.resolve("keep.txt"), "k");
    Files.writeString(source.resolve("skip.bin"), "s");

    FileUtility.copyTree(source, destination, false, new WildcardFileNamePathFilter("*.txt"));

    Assert.assertTrue(Files.exists(destination.resolve("keep.txt")));
    Assert.assertFalse(Files.exists(destination.resolve("skip.bin")));
  }

  public void testCopyTreeForSingleFile ()
    throws IOException {

    Path source = workingDir.resolve("single.txt");
    Path destination = workingDir.resolve("copy.txt");

    Files.writeString(source, "content", StandardCharsets.UTF_8);
    FileUtility.copyTree(source, destination, false);

    Assert.assertEquals(Files.readString(destination), "content");
  }

  public void testDeleteTreeRemovesFilesAndChildDirectories ()
    throws IOException {

    Path target = Files.createDirectory(workingDir.resolve("doomed"));
    Path subDir = Files.createDirectory(target.resolve("sub"));

    Files.writeString(target.resolve("a.txt"), "a");
    Files.writeString(subDir.resolve("b.txt"), "b");

    FileUtility.deleteTree(target, true, false);

    Assert.assertFalse(Files.exists(target));
  }

  public void testDeleteTreeKeepsRootWhenIncludeTargetFalse ()
    throws IOException {

    Path target = Files.createDirectory(workingDir.resolve("rooted"));

    Files.writeString(target.resolve("a.txt"), "a");
    FileUtility.deleteTree(target, false, false);

    Assert.assertTrue(Files.exists(target));
    Assert.assertTrue(FileUtility.isDirectoryEmpty(target));
  }

  public void testCopyBuilderReturnsConfigurationBuilder () {

    Path source = workingDir.resolve("a");
    Path destination = workingDir.resolve("b");

    Assert.assertNotNull(FileUtility.copyBuilder(source, destination));
    Assert.assertNotNull(FileUtility.deleteBuilder(source));
  }
}
