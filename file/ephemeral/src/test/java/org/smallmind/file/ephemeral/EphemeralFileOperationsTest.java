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
package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises the {@link EphemeralFileStore} behavioural surfaces through the standard
 * {@link java.nio.file.Files} API: {@code newByteChannel} option parsing, {@code copy},
 * {@code move}, {@code createDirectory}, {@code delete}, and attribute reads. Test cases
 * are chosen to drive distinct branches (existing vs missing target, replace vs no-replace,
 * empty vs non-empty directory) rather than re-covering trivial round-trips.
 */
@Test(groups = "unit")
public class EphemeralFileOperationsTest {

  private EphemeralFileSystem ephemeralFileSystem;

  @BeforeClass
  public void beforeClass () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider("ephemeral");

    ephemeralFileSystem = (EphemeralFileSystem)provider.getFileSystem(URI.create("ephemeral:///"));
  }

  @AfterMethod
  public void afterMethod () {

    ephemeralFileSystem.clear();
  }

  private Path path (String text) {

    return ephemeralFileSystem.getPath(text);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testReadAndWriteOptionsRejected ()
    throws IOException {

    Path file = path("/a.txt");

    Files.writeString(file, "hi", StandardCharsets.UTF_8);
    Files.newByteChannel(file, Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAppendAndTruncateOptionsRejected ()
    throws IOException {

    Path file = path("/a.txt");

    Files.writeString(file, "hi", StandardCharsets.UTF_8);
    Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING));
  }

  public void testDefaultOpenModeIsReadOnly ()
    throws IOException {

    Path file = path("/a.txt");

    Files.writeString(file, "payload", StandardCharsets.UTF_8);

    try (SeekableByteChannel channel = Files.newByteChannel(file)) {

      Assert.assertEquals(channel.size(), "payload".getBytes(StandardCharsets.UTF_8).length);
    }
  }

  @Test(expectedExceptions = NoSuchFileException.class)
  public void testReadOnMissingFileRejected ()
    throws IOException {

    Files.newByteChannel(path("/missing.txt"));
  }

  @Test(expectedExceptions = FileAlreadyExistsException.class)
  public void testCreateNewOnExistingRejected ()
    throws IOException {

    Path file = path("/x.txt");

    Files.writeString(file, "hi", StandardCharsets.UTF_8);
    Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
  }

  @Test(expectedExceptions = IOException.class)
  public void testOpenDirectoryForReadingRejected ()
    throws IOException {

    Path dir = path("/dir");

    Files.createDirectory(dir);
    Files.newByteChannel(dir);
  }

  @Test(expectedExceptions = NoSuchFileException.class)
  public void testCreateDirectoryMissingParentRejected ()
    throws IOException {

    Files.createDirectory(path("/no/such/parent/leaf"));
  }

  @Test(expectedExceptions = FileAlreadyExistsException.class)
  public void testCreateDirectoryAlreadyExistsRejected ()
    throws IOException {

    Path dir = path("/dir");

    Files.createDirectory(dir);
    Files.createDirectory(dir);
  }

  @Test(expectedExceptions = NoSuchFileException.class)
  public void testDeleteMissingFileRejected ()
    throws IOException {

    Files.delete(path("/missing.txt"));
  }

  @Test(expectedExceptions = DirectoryNotEmptyException.class)
  public void testDeleteNonEmptyDirectoryRejected ()
    throws IOException {

    Files.createDirectory(path("/dir"));
    Files.writeString(path("/dir/file.txt"), "hi", StandardCharsets.UTF_8);
    Files.delete(path("/dir"));
  }

  public void testDeleteEmptyDirectory ()
    throws IOException {

    Path dir = path("/dir");

    Files.createDirectory(dir);
    Files.delete(dir);

    Assert.assertFalse(Files.exists(dir));
  }

  @Test(expectedExceptions = IOException.class)
  public void testDeleteRootRejected ()
    throws IOException {

    Files.delete(path("/"));
  }

  @Test(expectedExceptions = FileAlreadyExistsException.class)
  public void testCopyOntoExistingWithoutReplaceRejected ()
    throws IOException {

    Files.writeString(path("/src.txt"), "source", StandardCharsets.UTF_8);
    Files.writeString(path("/dst.txt"), "target", StandardCharsets.UTF_8);
    Files.copy(path("/src.txt"), path("/dst.txt"));
  }

  public void testCopyOntoExistingWithReplaceOverwrites ()
    throws IOException {

    Files.writeString(path("/src.txt"), "source", StandardCharsets.UTF_8);
    Files.writeString(path("/dst.txt"), "target", StandardCharsets.UTF_8);
    Files.copy(path("/src.txt"), path("/dst.txt"), StandardCopyOption.REPLACE_EXISTING);

    Assert.assertEquals(Files.readString(path("/dst.txt"), StandardCharsets.UTF_8), "source");
  }

  public void testCopyToNewTargetLeavesSourceIntact ()
    throws IOException {

    Files.writeString(path("/src.txt"), "payload", StandardCharsets.UTF_8);
    Files.copy(path("/src.txt"), path("/dst.txt"));

    Assert.assertEquals(Files.readString(path("/dst.txt"), StandardCharsets.UTF_8), "payload");
    Assert.assertEquals(Files.readString(path("/src.txt"), StandardCharsets.UTF_8), "payload");
  }

  public void testMoveWithReplaceTransfersContent ()
    throws IOException {

    Files.writeString(path("/src.txt"), "payload", StandardCharsets.UTF_8);
    Files.writeString(path("/dst.txt"), "target", StandardCharsets.UTF_8);
    Files.move(path("/src.txt"), path("/dst.txt"), StandardCopyOption.REPLACE_EXISTING);

    Assert.assertFalse(Files.exists(path("/src.txt")));
    Assert.assertEquals(Files.readString(path("/dst.txt"), StandardCharsets.UTF_8), "payload");
  }

  public void testMoveToNewTargetTransfersContent ()
    throws IOException {

    Files.writeString(path("/src.txt"), "payload", StandardCharsets.UTF_8);
    Files.move(path("/src.txt"), path("/dst.txt"));

    Assert.assertFalse(Files.exists(path("/src.txt")));
    Assert.assertEquals(Files.readString(path("/dst.txt"), StandardCharsets.UTF_8), "payload");
  }

  @Test(expectedExceptions = NoSuchFileException.class)
  public void testMoveToMissingParentRejected ()
    throws IOException {

    Files.writeString(path("/src.txt"), "payload", StandardCharsets.UTF_8);
    Files.move(path("/src.txt"), path("/no/such/parent/dst.txt"));
  }

  public void testReadAttributesByNameSelectsSubset ()
    throws IOException {

    Files.writeString(path("/x.txt"), "abcdef", StandardCharsets.UTF_8);

    Map<String, Object> attributes = Files.readAttributes(path("/x.txt"), "basic:size,isDirectory");

    Assert.assertEquals(attributes.get("size"), 6L);
    Assert.assertEquals(attributes.get("isDirectory"), Boolean.FALSE);
    Assert.assertFalse(attributes.containsKey("creationTime"));
  }

  public void testReadAttributesAsteriskExpandsToAllBasic ()
    throws IOException {

    Files.writeString(path("/x.txt"), "abc", StandardCharsets.UTF_8);

    Map<String, Object> attributes = Files.readAttributes(path("/x.txt"), "basic:*");

    Assert.assertTrue(attributes.containsKey("size"));
    Assert.assertTrue(attributes.containsKey("creationTime"));
    Assert.assertTrue(attributes.containsKey("lastModifiedTime"));
    Assert.assertTrue(attributes.containsKey("isRegularFile"));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testReadAttributesUnknownViewRejected ()
    throws IOException {

    Files.writeString(path("/x.txt"), "abc", StandardCharsets.UTF_8);
    Files.readAttributes(path("/x.txt"), "posix:size");
  }
}
