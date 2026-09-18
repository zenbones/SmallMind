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
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.file.ephemeral.watch.EphemeralWatchService;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Covers the invariants of the heap layer that the differential suite cannot reach, either because
 * they are not observable through {@code java.nio.file} on every platform (symbolic links need
 * elevation on Windows), or because they are properties of this implementation rather than of the
 * specification (space accounting, the bounded watch queue, segment boundaries).
 *
 * <p>Each test here pins a behaviour that was either broken or absent, and that would fail silently
 * rather than loudly if it regressed.
 */
@Test(groups = "unit")
public class EphemeralHeapInvariantTest {

  private EphemeralFileSystem ephemeralFileSystem;

  @BeforeClass
  public void beforeClass () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider("heap-invariant");

    ephemeralFileSystem = (EphemeralFileSystem)provider.getFileSystem(URI.create("heap-invariant:///"));
  }

  @AfterMethod
  public void afterMethod () {

    ephemeralFileSystem.clear();
  }

  private Path path (String text) {

    return ephemeralFileSystem.getPath(text);
  }

  /**
   * Truncating and then growing again must expose zeros, not the bytes that used to be there. The
   * segments are pooled, so a truncate that failed to clear the discarded tail would leak the old
   * content back into the file.
   */
  public void testTruncateThenRegrowReadsBackZeros ()
    throws IOException {

    Path file = path("/t.bin");

    Files.write(file, "ABCDEFGHIJKLMNOP".getBytes(StandardCharsets.UTF_8));

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE))) {
      channel.truncate(4);
      channel.position(8);
      channel.write(ByteBuffer.wrap(new byte[] {90}));
    }

    Assert.assertEquals(Files.readAllBytes(file), new byte[] {65, 66, 67, 68, 0, 0, 0, 0, 90});
  }

  /**
   * The same invariant, across a segment boundary, where whole segments are released rather than
   * just a tail within one.
   */
  public void testTruncateAcrossSegmentBoundaryReadsBackZeros ()
    throws IOException {

    Path file = path("/big.bin");
    byte[] payload = new byte[3000];

    Arrays.fill(payload, (byte)120);
    Files.write(file, payload);

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE))) {
      channel.truncate(1000);
    }

    Assert.assertEquals(Files.size(file), 1000L);

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE))) {
      channel.position(2500);
      channel.write(ByteBuffer.wrap(new byte[] {1}));
    }

    byte[] regrown = Files.readAllBytes(file);

    Assert.assertEquals(regrown.length, 2501);
    for (int index = 1000; index < 2500; index++) {
      Assert.assertEquals(regrown[index], 0, "The gap left by a truncate was not zero at " + index);
    }
  }

  /**
   * A relative link target is interpreted against the directory holding the link, not against the
   * working directory or the root.
   */
  public void testRelativeSymbolicLinkResolvesAgainstItsDirectory ()
    throws IOException {

    Files.createDirectory(path("/dir"));
    Files.writeString(path("/dir/leaf.txt"), "relative-hit", StandardCharsets.UTF_8);
    Files.createSymbolicLink(path("/dir/alias"), path("leaf.txt"));

    Assert.assertEquals(Files.readString(path("/dir/alias")), "relative-hit");
    Assert.assertEquals(path("/dir/alias").toRealPath().toString(), "/dir/leaf.txt");
    Assert.assertEquals(Files.readSymbolicLink(path("/dir/alias")).toString(), "leaf.txt");
  }

  /**
   * A link in the middle of a path is always traversed, since the elements after it have to be
   * resolved somewhere, while the link itself remains a link.
   */
  public void testLinkAsIntermediateElementIsTraversed ()
    throws IOException {

    Files.createDirectory(path("/dir"));
    Files.writeString(path("/dir/leaf.txt"), "through", StandardCharsets.UTF_8);
    Files.createSymbolicLink(path("/shortcut"), path("/dir"));

    Assert.assertEquals(Files.readString(path("/shortcut/leaf.txt")), "through");
    Assert.assertTrue(Files.isSymbolicLink(path("/shortcut")));

    // deleting a link removes the link, never what it points at
    Assert.assertTrue(Files.deleteIfExists(path("/shortcut")));
    Assert.assertTrue(Files.exists(path("/dir/leaf.txt")));
  }

  /**
   * A cycle between two links must be reported rather than recursed until the stack runs out.
   */
  public void testMutualSymbolicLinkCycleIsReported ()
    throws IOException {

    Files.createSymbolicLink(path("/ping"), path("/pong"));
    Files.createSymbolicLink(path("/pong"), path("/ping"));

    try {
      Files.readAllBytes(path("/ping"));
      Assert.fail("A symbolic link cycle should have been reported");
    } catch (FileSystemException fileSystemException) {
      Assert.assertTrue(fileSystemException.getMessage().contains("symbolic links"));
    }
  }

  /**
   * Space is accounted for as it is consumed and returned, and a move re-parents a node without
   * disturbing the total.
   */
  public void testSpaceIsAccountedForAndReturned ()
    throws IOException {

    long free = ephemeralFileSystem.getFileStore().getUnallocatedSpace();

    Files.write(path("/a.bin"), new byte[5000]);
    Assert.assertTrue(ephemeralFileSystem.getFileStore().getUnallocatedSpace() < free);

    Files.delete(path("/a.bin"));
    Assert.assertEquals(ephemeralFileSystem.getFileStore().getUnallocatedSpace(), free);

    Files.write(path("/b.bin"), new byte[4096]);

    long afterWrite = ephemeralFileSystem.getFileStore().getUnallocatedSpace();

    Files.move(path("/b.bin"), path("/c.bin"));
    Assert.assertEquals(ephemeralFileSystem.getFileStore().getUnallocatedSpace(), afterWrite);

    Files.delete(path("/c.bin"));
    Assert.assertEquals(ephemeralFileSystem.getFileStore().getUnallocatedSpace(), free);
  }

  /**
   * A hard link is a second name for one body of content, so it costs nothing, and the content is
   * only released once every name for it is gone.
   */
  public void testHardLinkHoldsSpaceUntilTheLastNameGoes ()
    throws IOException {

    long free = ephemeralFileSystem.getFileStore().getUnallocatedSpace();

    Files.write(path("/d.bin"), new byte[2048]);
    Files.createLink(path("/d-link.bin"), path("/d.bin"));

    Assert.assertEquals(ephemeralFileSystem.getFileStore().getUnallocatedSpace(), free - 2048);

    Files.delete(path("/d.bin"));
    Assert.assertEquals(ephemeralFileSystem.getFileStore().getUnallocatedSpace(), free - 2048);

    Files.delete(path("/d-link.bin"));
    Assert.assertEquals(ephemeralFileSystem.getFileStore().getUnallocatedSpace(), free);
  }

  /**
   * A consumer that never collects its events must not grow the queue without bound; it is told
   * that events were lost instead.
   */
  public void testWatchKeyQueueIsBoundedAndReportsOverflow ()
    throws IOException {

    EphemeralPath watched = (EphemeralPath)path("/watched");

    Files.createDirectory(watched);

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.OVERFLOW});

      for (int index = 0; index < 1000; index++) {
        Files.writeString(path("/watched/entry" + index), "x", StandardCharsets.UTF_8);
      }
      service.poll();

      List<WatchEvent<?>> events = key.pollEvents();
      boolean overflowObserved = false;

      for (WatchEvent<?> event : events) {
        if (StandardWatchEventKinds.OVERFLOW.equals(event.kind())) {
          overflowObserved = true;
        }
      }

      Assert.assertTrue(events.size() <= 512, "The pending event queue grew without bound");
      Assert.assertTrue(overflowObserved, "The loss of events was not reported as an overflow");
    }
  }

  /**
   * Concurrent appenders must not overwrite one another.
   *
   * <p>Each appending channel resolves the end of the file and writes in a single atomic step.
   * Doing it in two steps loses bytes: four threads appending 200 bytes each produced 200 bytes in
   * the original implementation, and 746 when only the position was made per-channel.
   */
  public void testConcurrentAppendsDoNotOverwriteOneAnother ()
    throws Exception {

    Path file = path("/shared.txt");

    Files.write(file, new byte[0]);

    int threadCount = 4;
    int writesPerThread = 200;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    Thread[] threads = new Thread[threadCount];

    for (int index = 0; index < threadCount; index++) {

      final byte marker = (byte)(65 + index);

      threads[index] = new Thread(() -> {
        try {
          startLatch.await();
          try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND))) {
            for (int write = 0; write < writesPerThread; write++) {
              channel.write(ByteBuffer.wrap(new byte[] {marker}));
            }
          }
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        } finally {
          doneLatch.countDown();
        }
      });
      threads[index].start();
    }

    startLatch.countDown();
    Assert.assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "The appending threads did not finish");

    Assert.assertEquals(Files.size(file), (long)threadCount * writesPerThread);
  }
}
