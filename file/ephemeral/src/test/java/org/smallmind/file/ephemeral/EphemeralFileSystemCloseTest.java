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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers what {@link EphemeralFileSystem#close()} is obliged to do.
 *
 * <p>{@link java.nio.file.FileSystem#close()} does not merely mark a file system unusable: it
 * closes the channels, directory streams, and watch services opened through it, so that a caller
 * still holding one of them is told it is gone rather than being allowed to operate on a file system
 * that no longer exists. A file system that only flipped a flag would leave a channel apparently
 * healthy while the heap beneath it had been abandoned.
 *
 * <p>Each test builds its own file system, since closing the shared one would strand every other
 * test.
 */
@Test(groups = "unit")
public class EphemeralFileSystemCloseTest {

  private EphemeralFileSystem closeableFileSystem () {

    return new EphemeralFileSystem(new EphemeralFileSystemProvider("closeable"), new EphemeralFileSystemConfiguration(1048576L, 64, "/"));
  }

  private Path seededFile (EphemeralFileSystem fileSystem, String text)
    throws IOException {

    Path file = fileSystem.getPath(text);

    Files.writeString(file, "content", StandardCharsets.UTF_8);

    return file;
  }

  public void testClosingTheFileSystemClosesAnOpenByteChannel ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();
    SeekableByteChannel channel = Files.newByteChannel(seededFile(fileSystem, "/a.txt"), Set.of(StandardOpenOption.READ));

    Assert.assertTrue(channel.isOpen());

    fileSystem.close();

    Assert.assertFalse(channel.isOpen(), "The channel outlived the file system that opened it");
    Assert.assertThrows(ClosedChannelException.class, () -> channel.read(ByteBuffer.allocate(4)));
  }

  public void testClosingTheFileSystemClosesAnOpenFileChannel ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();
    FileChannel channel = fileSystem.provider().newFileChannel(seededFile(fileSystem, "/b.txt"), Set.of(StandardOpenOption.READ));

    Assert.assertTrue(channel.isOpen());

    fileSystem.close();

    Assert.assertFalse(channel.isOpen(), "The file channel outlived the file system that opened it");
    Assert.assertThrows(ClosedChannelException.class, () -> channel.read(ByteBuffer.allocate(4)));
  }

  public void testClosingTheFileSystemClosesAnOpenDirectoryStream ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();

    Files.createDirectory(fileSystem.getPath("/dir"));
    seededFile(fileSystem, "/dir/leaf.txt");

    DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/dir"));

    fileSystem.close();

    Assert.assertThrows(ClosedDirectoryStreamException.class, directoryStream::iterator);
  }

  public void testClosingTheFileSystemClosesAnOpenWatchService ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();

    Files.createDirectory(fileSystem.getPath("/watched"));

    WatchService watchService = fileSystem.newWatchService();

    ((EphemeralPath)fileSystem.getPath("/watched")).register(watchService, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_CREATE});

    fileSystem.close();

    Assert.assertThrows(ClosedWatchServiceException.class, watchService::poll);
  }

  public void testEveryOpenResourceIsClosed ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();

    Files.createDirectory(fileSystem.getPath("/many"));

    SeekableByteChannel first = Files.newByteChannel(seededFile(fileSystem, "/many/one.txt"), Set.of(StandardOpenOption.READ));
    SeekableByteChannel second = Files.newByteChannel(seededFile(fileSystem, "/many/two.txt"), Set.of(StandardOpenOption.WRITE));
    DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/many"));
    WatchService watchService = fileSystem.newWatchService();

    fileSystem.close();

    Assert.assertFalse(first.isOpen());
    Assert.assertFalse(second.isOpen());
    Assert.assertThrows(ClosedDirectoryStreamException.class, directoryStream::iterator);
    Assert.assertThrows(ClosedWatchServiceException.class, watchService::poll);
  }

  /**
   * A resource that closed itself must not be revisited, and must not keep the registry growing.
   * The observable part is that closing the file system afterwards is uneventful.
   */
  public void testAResourceClosedByItsCallerIsNotClosedAgain ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();
    Path file = seededFile(fileSystem, "/c.txt");

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ))) {
      Assert.assertTrue(channel.isOpen());
    }

    fileSystem.close();

    Assert.assertFalse(fileSystem.isOpen());
  }

  /**
   * A channel opened with {@code DELETE_ON_CLOSE} has work to do while closing, and the file system
   * is closing underneath it. It must not fail on the way out.
   */
  public void testAChannelPendingDeletionClosesCleanly ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();
    Path file = fileSystem.getPath("/doomed.txt");
    SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE));

    channel.write(ByteBuffer.wrap("x".getBytes(StandardCharsets.UTF_8)));

    fileSystem.close();

    Assert.assertFalse(channel.isOpen());
  }

  public void testClosingIsIdempotent ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();

    seededFile(fileSystem, "/d.txt");

    fileSystem.close();
    fileSystem.close();

    Assert.assertFalse(fileSystem.isOpen());
  }

  public void testTheFileSystemIsUnusableOnceClosed ()
    throws IOException {

    EphemeralFileSystem fileSystem = closeableFileSystem();
    Path file = seededFile(fileSystem, "/e.txt");

    fileSystem.close();

    Assert.assertFalse(fileSystem.isOpen());
    Assert.assertThrows(ClosedFileSystemException.class, () -> Files.readString(file));
    Assert.assertThrows(ClosedFileSystemException.class, fileSystem::getFileStore);
  }

  /**
   * The default file system cannot be closed, so a request to close one is ignored and its
   * resources are left alone.
   */
  public void testTheDefaultFileSystemIsNotClosed ()
    throws IOException {

    EphemeralFileSystem fileSystem = new EphemeralFileSystem(new EphemeralFileSystemProvider(java.nio.file.FileSystems.getDefault().provider()), new EphemeralFileSystemConfiguration(1048576L, 64, "/"));
    SeekableByteChannel channel = Files.newByteChannel(seededFile(fileSystem, "/f.txt"), Set.of(StandardOpenOption.READ));

    Assert.assertTrue(fileSystem.provider().isDefault());

    fileSystem.close();

    Assert.assertTrue(fileSystem.isOpen(), "The default file system should not have closed");
    Assert.assertTrue(channel.isOpen(), "The default file system should not have closed its resources");

    channel.close();
  }
}
