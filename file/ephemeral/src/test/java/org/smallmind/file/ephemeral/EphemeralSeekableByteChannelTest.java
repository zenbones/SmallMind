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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the {@link EphemeralSeekableByteChannel} state machine: read-vs-write modes are
 * enforced at the channel level, the close flag rejects further I/O with
 * {@link ClosedChannelException}, append and truncate operate on the underlying stream, and
 * {@link StandardOpenOption#DELETE_ON_CLOSE} removes the file when the channel closes.
 * The channel is exercised through {@link Files#newByteChannel} so the test crosses both the
 * provider and the file store.
 */
@Test(groups = "unit")
public class EphemeralSeekableByteChannelTest {

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

  @Test(expectedExceptions = NonReadableChannelException.class)
  public void testWriteOnlyChannelRejectsRead ()
    throws IOException {

    Path file = path("/write-only.txt");

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {
      channel.read(ByteBuffer.allocate(8));
    }
  }

  @Test(expectedExceptions = NonWritableChannelException.class)
  public void testReadOnlyChannelRejectsWrite ()
    throws IOException {

    Path file = path("/read-only.txt");

    Files.writeString(file, "seed", StandardCharsets.UTF_8);

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ))) {
      channel.write(ByteBuffer.wrap("nope".getBytes(StandardCharsets.UTF_8)));
    }
  }

  @Test(expectedExceptions = ClosedChannelException.class)
  public void testReadAfterCloseRejected ()
    throws IOException {

    Path file = path("/closed.txt");

    Files.writeString(file, "seed", StandardCharsets.UTF_8);

    SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ));

    channel.close();

    Assert.assertFalse(channel.isOpen());

    channel.read(ByteBuffer.allocate(8));
  }

  public void testAppendModeWritesAtEndOfFile ()
    throws IOException {

    Path file = path("/append.txt");

    Files.writeString(file, "AB", StandardCharsets.UTF_8);

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND))) {
      channel.write(ByteBuffer.wrap("CD".getBytes(StandardCharsets.UTF_8)));
    }

    Assert.assertEquals(Files.readString(file, StandardCharsets.UTF_8), "ABCD");
  }

  public void testTruncateExistingClearsContentOnOpen ()
    throws IOException {

    Path file = path("/truncated.txt");

    Files.writeString(file, "old-content", StandardCharsets.UTF_8);

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
      channel.write(ByteBuffer.wrap("new".getBytes(StandardCharsets.UTF_8)));
    }

    Assert.assertEquals(Files.readString(file, StandardCharsets.UTF_8), "new");
  }

  public void testDeleteOnCloseRemovesFile ()
    throws IOException {

    Path file = path("/transient.txt");

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE))) {
      channel.write(ByteBuffer.wrap("ephemeral".getBytes(StandardCharsets.UTF_8)));

      Assert.assertTrue(Files.exists(file));
    }

    Assert.assertFalse(Files.exists(file));
  }

  public void testReadHonoursPositionedStart ()
    throws IOException {

    Path file = path("/seekable.txt");

    Files.writeString(file, "ABCDEF", StandardCharsets.UTF_8);

    try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ))) {
      channel.position(2);

      ByteBuffer buffer = ByteBuffer.allocate(16);
      int bytesRead = channel.read(buffer);

      buffer.flip();
      byte[] tail = new byte[bytesRead];
      buffer.get(tail);

      Assert.assertEquals(new String(tail, StandardCharsets.UTF_8), "CDEF");
    }
  }
}
