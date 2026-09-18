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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Confirms that a jailed path can not address anything outside its jail. Parent traversal is
 * expected to be clamped at the jail root rather than rejected, so that a path such as
 * {@code "/../../etc/passwd"} names a file inside the jail, exactly as it would inside a chroot;
 * a path that belongs to another file system, or to another jail, is expected to be refused
 * outright.
 */
@Test(groups = "unit")
public class JailedEscapeTest {

  private Path nativeRoot;
  private JailedFileSystemProvider provider;
  private FileSystem jailedFileSystem;
  private JailedPathTranslator translator;

  @BeforeMethod
  public void beforeMethod ()
    throws IOException {

    nativeRoot = Files.createTempDirectory("jailed-escape-test-");
    translator = new RootedPathTranslator(nativeRoot);
    provider = new JailedFileSystemProvider("jailed", translator);
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

  private Path unwrap (String text)
    throws IOException {

    return translator.unwrapPath(jailedFileSystem.getPath(text));
  }

  public void testParentTraversalClampedAtRoot ()
    throws IOException {

    Assert.assertEquals(unwrap("/.."), nativeRoot);
    Assert.assertEquals(unwrap("/../.."), nativeRoot);
    Assert.assertEquals(unwrap("/a/../.."), nativeRoot);
    Assert.assertEquals(unwrap("//..//..//"), nativeRoot);
    Assert.assertEquals(unwrap("/."), nativeRoot);
    Assert.assertEquals(unwrap(".."), nativeRoot);
    Assert.assertEquals(unwrap("/../../etc/passwd"), nativeRoot.resolve("etc").resolve("passwd"));
    Assert.assertEquals(unwrap("foo/../../bar"), nativeRoot.resolve("bar"));
    Assert.assertEquals(unwrap("/a/b/../../../../c"), nativeRoot.resolve("c"));
  }

  public void testRelativePathsResolveAgainstRoot ()
    throws IOException {

    Assert.assertEquals(unwrap(""), nativeRoot);
    Assert.assertEquals(unwrap("alpha/beta"), nativeRoot.resolve("alpha").resolve("beta"));
    Assert.assertEquals(unwrap("./alpha"), nativeRoot.resolve("alpha"));
  }

  public void testWriteThroughTraversalLandsInsideJail ()
    throws IOException {

    Files.writeString(jailedFileSystem.getPath("/../../escaped.txt"), "contained", StandardCharsets.UTF_8);

    Assert.assertTrue(Files.exists(nativeRoot.resolve("escaped.txt")));
    Assert.assertFalse(Files.exists(nativeRoot.getParent().resolve("escaped.txt")));
    Assert.assertEquals(Files.readString(jailedFileSystem.getPath("/escaped.txt"), StandardCharsets.UTF_8), "contained");
  }

  public void testDirectoryListingOfTraversedRootStaysInsideJail ()
    throws IOException {

    Files.createDirectory(jailedFileSystem.getPath("/visible"));

    try (Stream<Path> stream = Files.list(jailedFileSystem.getPath("/../.."))) {
      Assert.assertEquals(stream.map(Path::toString).toList(), List.of("/visible"));
    }
  }

  @Test(expectedExceptions = ProviderMismatchException.class)
  public void testNativePathRejectedByProvider ()
    throws IOException {

    provider.newByteChannel(nativeRoot.resolve("x.txt"), Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));
  }

  @Test(expectedExceptions = ProviderMismatchException.class)
  public void testPathFromAnotherJailRejectedByProvider ()
    throws IOException {

    JailedFileSystemProvider otherProvider = new JailedFileSystemProvider("jailed", new RootedPathTranslator(nativeRoot));

    provider.delete(otherProvider.getFileSystem(URI.create("jailed:///")).getPath("/x.txt"));
  }

  public void testRootedPathTranslatorNormalizesItsRoot ()
    throws IOException {

    RootedPathTranslator unnormalizedTranslator = new RootedPathTranslator(nativeRoot.resolve("sub").resolve(".."));

    Assert.assertEquals(unnormalizedTranslator.getRootPath(), nativeRoot);
    Assert.assertEquals(unnormalizedTranslator.unwrapPath(jailedFileSystem.getPath("/../a")), nativeRoot.resolve("a"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRootRejected () {

    new RootedPathTranslator(null);
  }
}
