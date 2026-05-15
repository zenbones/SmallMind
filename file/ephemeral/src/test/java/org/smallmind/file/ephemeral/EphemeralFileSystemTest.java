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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EphemeralFileSystemTest {

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

  public void testGetPathReturnsEphemeralPath () {

    Assert.assertTrue(ephemeralFileSystem.getPath("/opt/whatsit/twimble/farkle") instanceof EphemeralPath);
  }

  public void testCreateDirectoriesWriteAndRead ()
    throws IOException {

    Path directory = ephemeralFileSystem.getPath("/opt/whatsit/twimble/farkle");

    Files.createDirectories(directory);
    Assert.assertTrue(Files.isDirectory(directory));

    Path file = ephemeralFileSystem.getPath("/opt/whatsit/twimble/farkle/sparkle.txt");
    byte[] payload = "Hello out there!".getBytes(StandardCharsets.UTF_8);

    try (SeekableByteChannel byteChannel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {

      ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

      byteBuffer.put(payload);
      byteBuffer.flip();
      byteChannel.write(byteBuffer);
    }

    Assert.assertTrue(Files.isRegularFile(file));
    Assert.assertEquals(Files.readAllBytes(file), payload);
    Assert.assertEquals(Files.readString(file, StandardCharsets.UTF_8), "Hello out there!");
  }

  public void testWriteStringAndReadStringRoundTrip ()
    throws IOException {

    Path directory = ephemeralFileSystem.getPath("/opt/whatsit/notes");
    Path file = ephemeralFileSystem.getPath("/opt/whatsit/notes/greeting.txt");

    Files.createDirectories(directory);
    Files.writeString(file, "Hello out there!", StandardCharsets.UTF_8);

    Assert.assertEquals(Files.readString(file, StandardCharsets.UTF_8), "Hello out there!");
  }

  public void testClearRemovesAllEntries ()
    throws IOException {

    Path directory = ephemeralFileSystem.getPath("/opt/whatsit/twimble");
    Path file = ephemeralFileSystem.getPath("/opt/whatsit/twimble/spark.txt");

    Files.createDirectories(directory);
    Files.writeString(file, "hello", StandardCharsets.UTF_8);

    Assert.assertTrue(Files.isRegularFile(file));

    ephemeralFileSystem.clear();

    Assert.assertFalse(Files.exists(file));
    Assert.assertFalse(Files.exists(directory));
  }
}
