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
package org.smallmind.file.jailed;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * End-to-end coverage of {@link JailedFileSystemProvider} against a real native subtree. A
 * fresh temporary directory is used as the jail root for every test method so that the
 * provider's path translation, native delegation, and directory-stream wrapping are exercised
 * against actual file-system state without leaking between tests.
 */
@Test(groups = "unit")
public class JailedFileSystemProviderTest {

  private Path nativeRoot;
  private JailedFileSystemProvider provider;
  private FileSystem jailedFileSystem;

  @BeforeMethod
  public void beforeMethod ()
    throws IOException {

    nativeRoot = Files.createTempDirectory("jailed-provider-test-");
    provider = new JailedFileSystemProvider("jailed", new RootedPathTranslator(nativeRoot));
    jailedFileSystem = provider.getFileSystem(URI.create("jailed:///"));
  }

  @AfterMethod
  public void afterMethod ()
    throws IOException {

    if ((nativeRoot != null) && Files.exists(nativeRoot)) {
      try (Stream<Path> walk = Files.walk(nativeRoot)) {
        walk.sorted(Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ignored) {
            // best-effort cleanup
          }
        });
      }
    }
  }

  private Path jailedPath (String text) {

    return jailedFileSystem.getPath(text);
  }

  public void testGetFileSystemReturnsManagedSingleton () {

    Assert.assertSame(provider.getFileSystem(URI.create("jailed:///")), jailedFileSystem);
  }

  @Test(expectedExceptions = FileSystemAlreadyExistsException.class)
  public void testNewFileSystemAlwaysRejected () {

    provider.newFileSystem(URI.create("jailed:///"), java.util.Map.of());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetFileSystemRejectsWrongScheme () {

    provider.getFileSystem(URI.create("other:///"));
  }

  public void testGetPathFromUriReturnsJailedPath () {

    Path path = provider.getPath(URI.create("jailed:///alpha/beta"));

    Assert.assertTrue(path instanceof JailedPath);
    Assert.assertEquals(path.toString(), "/alpha/beta");
    Assert.assertSame(path.getFileSystem(), jailedFileSystem);
  }

  public void testCreateDirectoryLandsOnNative ()
    throws IOException {

    Files.createDirectory(jailedPath("/alpha"));

    Assert.assertTrue(Files.isDirectory(nativeRoot.resolve("alpha")));
  }

  public void testWriteAndReadRoundTripsThroughNative ()
    throws IOException {

    Files.createDirectory(jailedPath("/notes"));
    Files.writeString(jailedPath("/notes/greeting.txt"), "Hello jail!", StandardCharsets.UTF_8);

    Assert.assertEquals(Files.readString(nativeRoot.resolve("notes").resolve("greeting.txt"), StandardCharsets.UTF_8), "Hello jail!");
    Assert.assertEquals(Files.readString(jailedPath("/notes/greeting.txt"), StandardCharsets.UTF_8), "Hello jail!");
  }

  public void testDeleteRemovesNativeEntry ()
    throws IOException {

    Files.writeString(jailedPath("/transient.txt"), "bye", StandardCharsets.UTF_8);

    Assert.assertTrue(Files.exists(nativeRoot.resolve("transient.txt")));

    Files.delete(jailedPath("/transient.txt"));

    Assert.assertFalse(Files.exists(nativeRoot.resolve("transient.txt")));
  }

  public void testNewDirectoryStreamWrapsEntriesAsJailedPaths ()
    throws IOException {

    Files.createDirectory(jailedPath("/listing"));
    Files.writeString(jailedPath("/listing/a.txt"), "a", StandardCharsets.UTF_8);
    Files.writeString(jailedPath("/listing/b.txt"), "b", StandardCharsets.UTF_8);
    Files.createDirectory(jailedPath("/listing/sub"));

    Set<String> observed = new HashSet<>();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(jailedPath("/listing"))) {
      for (Path entry : stream) {

        Assert.assertTrue(entry instanceof JailedPath, "expected JailedPath, got " + entry.getClass());
        Assert.assertSame(entry.getFileSystem(), jailedFileSystem);

        observed.add(entry.toString());
      }
    }

    Assert.assertEquals(observed, Set.of("/listing/a.txt", "/listing/b.txt", "/listing/sub"));
  }

  public void testCopyBetweenJailedPaths ()
    throws IOException {

    Files.writeString(jailedPath("/src.txt"), "payload", StandardCharsets.UTF_8);
    Files.copy(jailedPath("/src.txt"), jailedPath("/dst.txt"));

    Assert.assertEquals(Files.readString(jailedPath("/dst.txt"), StandardCharsets.UTF_8), "payload");
    Assert.assertEquals(Files.readString(jailedPath("/src.txt"), StandardCharsets.UTF_8), "payload");
  }

  public void testCopyWithReplaceExistingOverwrites ()
    throws IOException {

    Files.writeString(jailedPath("/src.txt"), "fresh", StandardCharsets.UTF_8);
    Files.writeString(jailedPath("/dst.txt"), "stale", StandardCharsets.UTF_8);

    Files.copy(jailedPath("/src.txt"), jailedPath("/dst.txt"), StandardCopyOption.REPLACE_EXISTING);

    Assert.assertEquals(Files.readString(jailedPath("/dst.txt"), StandardCharsets.UTF_8), "fresh");
  }

  public void testMoveBetweenJailedPaths ()
    throws IOException {

    Files.writeString(jailedPath("/src.txt"), "payload", StandardCharsets.UTF_8);
    Files.move(jailedPath("/src.txt"), jailedPath("/dst.txt"));

    Assert.assertFalse(Files.exists(jailedPath("/src.txt")));
    Assert.assertEquals(Files.readString(jailedPath("/dst.txt"), StandardCharsets.UTF_8), "payload");
  }

  public void testReadAttributesReturnsNativeBasicAttributes ()
    throws IOException {

    Files.writeString(jailedPath("/x.txt"), "abcdef", StandardCharsets.UTF_8);

    java.nio.file.attribute.BasicFileAttributes attributes = Files.readAttributes(jailedPath("/x.txt"), java.nio.file.attribute.BasicFileAttributes.class);

    Assert.assertTrue(attributes.isRegularFile());
    Assert.assertEquals(attributes.size(), 6L);
  }

  public void testIsSameFileForSameJailedPath ()
    throws IOException {

    Files.writeString(jailedPath("/x.txt"), "abc", StandardCharsets.UTF_8);

    Assert.assertTrue(Files.isSameFile(jailedPath("/x.txt"), jailedPath("/x.txt")));
  }

  public void testIsSameFileForDifferentJailedPaths ()
    throws IOException {

    Files.writeString(jailedPath("/a.txt"), "a", StandardCharsets.UTF_8);
    Files.writeString(jailedPath("/b.txt"), "b", StandardCharsets.UTF_8);

    Assert.assertFalse(Files.isSameFile(jailedPath("/a.txt"), jailedPath("/b.txt")));
  }
}
