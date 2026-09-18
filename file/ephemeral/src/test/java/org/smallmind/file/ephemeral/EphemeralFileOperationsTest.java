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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
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

  public void testReadAndWriteOptionsAccepted ()
    throws IOException {

    Path file = path("/a.txt");

    Files.writeString(file, "hi", StandardCharsets.UTF_8);

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE))) {
      channel.write(ByteBuffer.wrap("HI".getBytes(StandardCharsets.UTF_8)));
      channel.position(0);

      ByteBuffer buffer = ByteBuffer.allocate(2);

      Assert.assertEquals(channel.read(buffer), 2);
      Assert.assertEquals(new String(buffer.array(), StandardCharsets.UTF_8), "HI");
    }
  }

  public void testSeparateChannelsHoldIndependentPositions ()
    throws IOException {

    Path file = path("/two.txt");

    Files.writeString(file, "abcdef", StandardCharsets.UTF_8);

    try (SeekableByteChannel first = Files.newByteChannel(file, Set.of(StandardOpenOption.READ));
         SeekableByteChannel second = Files.newByteChannel(file, Set.of(StandardOpenOption.READ))) {

      ByteBuffer firstBuffer = ByteBuffer.allocate(3);
      ByteBuffer secondBuffer = ByteBuffer.allocate(3);

      first.read(firstBuffer);
      second.read(secondBuffer);

      Assert.assertEquals(new String(firstBuffer.array(), StandardCharsets.UTF_8), "abc");
      Assert.assertEquals(new String(secondBuffer.array(), StandardCharsets.UTF_8), "abc");
    }
  }

  public void testWriteBeyondEndZeroFills ()
    throws IOException {

    Path file = path("/sparse.bin");

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
      channel.position(4);
      channel.write(ByteBuffer.wrap(new byte[] {1}));
    }

    Assert.assertEquals(Files.size(file), 5L);
    Assert.assertEquals(Files.readAllBytes(file), new byte[] {0, 0, 0, 0, 1});
  }

  public void testMovedDirectoryKeepsItsChildren ()
    throws IOException {

    Files.createDirectory(path("/from"));
    Files.createDirectory(path("/from/inner"));
    Files.writeString(path("/from/inner/leaf.txt"), "kept", StandardCharsets.UTF_8);

    Files.move(path("/from"), path("/to"));

    Assert.assertFalse(Files.exists(path("/from")));
    Assert.assertTrue(Files.isDirectory(path("/to/inner")));
    Assert.assertEquals(Files.readString(path("/to/inner/leaf.txt")), "kept");
  }

  public void testCopyUsesTheTargetName ()
    throws IOException {

    Files.createDirectory(path("/source"));

    Files.copy(path("/source"), path("/renamed"));

    Assert.assertTrue(Files.isDirectory(path("/renamed")));
  }

  public void testCapacityIsEnforced ()
    throws IOException {

    EphemeralFileSystem boundedFileSystem = new EphemeralFileSystem(new EphemeralFileSystemProvider("bounded"), new EphemeralFileSystemConfiguration(64L, 16, "/"));
    Path file = boundedFileSystem.getPath("/blob");

    try {
      Files.write(file, new byte[4096]);
      Assert.fail("A write past the capacity of the store should have been rejected");
    } catch (IOException ioException) {
      Assert.assertTrue(boundedFileSystem.getFileStore().getUnallocatedSpace() >= 0);
      Assert.assertTrue(boundedFileSystem.getFileStore().getUsableSpace() <= boundedFileSystem.getFileStore().getTotalSpace());
    }
  }

  public void testHardLinkSharesContentAndSurvivesOneDeletion ()
    throws IOException {

    Path original = path("/original.txt");
    Path linked = path("/linked.txt");

    Files.writeString(original, "shared", StandardCharsets.UTF_8);
    Files.createLink(linked, original);

    Assert.assertTrue(Files.isSameFile(original, linked));
    Assert.assertEquals(Files.readString(linked), "shared");

    Files.delete(original);

    Assert.assertEquals(Files.readString(linked), "shared");
  }

  public void testSymbolicLinkResolvesAndReports ()
    throws IOException {

    Files.createDirectory(path("/real"));
    Files.writeString(path("/real/leaf.txt"), "target", StandardCharsets.UTF_8);
    Files.createSymbolicLink(path("/link"), path("/real"));

    Assert.assertTrue(Files.isSymbolicLink(path("/link")));
    Assert.assertFalse(Files.isSymbolicLink(path("/real")));
    Assert.assertEquals(Files.readString(path("/link/leaf.txt")), "target");
    Assert.assertEquals(Files.readSymbolicLink(path("/link")).toString(), "/real");
    Assert.assertEquals(path("/link/leaf.txt").toRealPath().toString(), "/real/leaf.txt");
  }

  public void testSymbolicLinkCycleReportsAnError ()
    throws IOException {

    Files.createSymbolicLink(path("/loop"), path("/loop"));

    try {
      Files.readAllBytes(path("/loop"));
      Assert.fail("A symbolic link cycle should have been reported");
    } catch (FileSystemException fileSystemException) {
      Assert.assertTrue(fileSystemException.getMessage().contains("symbolic links"));
    }
  }

  public void testFileChannelOpenIsSupported ()
    throws IOException {

    Path file = path("/channel.bin");

    Files.writeString(file, "abc", StandardCharsets.UTF_8);

    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      Assert.assertEquals(channel.size(), 3L);
    }
  }

  public void testCreateTempFileSucceeds ()
    throws IOException {

    Files.createDirectory(path("/tmp"));

    Path temporaryFile = Files.createTempFile(path("/tmp"), "prefix", ".suffix");

    Assert.assertTrue(Files.exists(temporaryFile));
    Assert.assertEquals(temporaryFile.getParent().toString(), "/tmp");
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
