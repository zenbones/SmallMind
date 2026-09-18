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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EphemeralPathTest {

  private EphemeralFileSystem ephemeralFileSystem;

  @BeforeClass
  public void beforeClass () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider("ephemeral");

    ephemeralFileSystem = (EphemeralFileSystem)provider.getFileSystem(URI.create("ephemeral:///"));
  }

  private EphemeralPath path (String text) {

    return new EphemeralPath(ephemeralFileSystem, text);
  }

  public void testConstructionFromString () {

    EphemeralPath path = path("/a/b/c");

    Assert.assertEquals(path.toString(), "/a/b/c");
    Assert.assertTrue(path.isAbsolute());
    Assert.assertEquals(path.getNameCount(), 3);
    Assert.assertSame(path.getFileSystem(), ephemeralFileSystem);
  }

  public void testConstructionWithMoreSegments () {

    EphemeralPath path = new EphemeralPath(ephemeralFileSystem, "/a", "b", "c");

    Assert.assertEquals(path.toString(), "/a/b/c");
    Assert.assertTrue(path.isAbsolute());
    Assert.assertEquals(path.getNameCount(), 3);
  }

  public void testRelativePath () {

    EphemeralPath path = path("a/b/c");

    Assert.assertEquals(path.toString(), "a/b/c");
    Assert.assertFalse(path.isAbsolute());
    Assert.assertNull(path.getRoot());
    Assert.assertEquals(path.getNameCount(), 3);
  }

  public void testEmptyPath () {

    EphemeralPath empty = path("");

    Assert.assertEquals(empty.toString(), "");
    Assert.assertFalse(empty.isAbsolute());
    Assert.assertEquals(empty.getNameCount(), 1);
    Assert.assertNull(empty.getParent());
    Assert.assertEquals(empty.getFileName().toString(), "");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testNulCharacterRejected () {

    path("/a/\u0000/b");
  }

  public void testRedundantSeparatorsCollapse () {

    Assert.assertEquals(path("/a//b").toString(), "/a/b");
    Assert.assertEquals(path("/a/b/").toString(), "/a/b");
    Assert.assertEquals(path("/").toString(), "/");
    Assert.assertEquals(path("/").getNameCount(), 0);
  }

  public void testRootRenders () {

    Assert.assertEquals(path("/a/b").getRoot().toString(), "/");
    Assert.assertEquals(path("/a/../..").normalize().toString(), "/");
  }

  public void testStartsWithRequiresMatchingAbsoluteness () {

    Assert.assertFalse(path("/a/b").startsWith(path("a")));
    Assert.assertTrue(path("/a/b").startsWith(path("/")));
  }

  public void testRelativizeToShorterPath () {

    Assert.assertEquals(path("/a/b/c").relativize(path("/a")).toString(), "../..");
    Assert.assertEquals(path("/a/b").relativize(path("/a/b")).toString(), "");
  }

  public void testNormalizeRetainsLeadingParentOfRelativePath () {

    Assert.assertEquals(path("../a").normalize().toString(), "../a");
    Assert.assertEquals(path("a/..").normalize().toString(), "");
  }

  public void testResolveEmptyHasNoTrailingSeparator () {

    Assert.assertEquals(path("/a").resolve(path("")).toString(), "/a");
  }

  public void testGetRoot () {

    EphemeralPath absoluteRoot = path("/a/b").getRoot();

    Assert.assertNotNull(absoluteRoot);
    Assert.assertTrue(absoluteRoot.isAbsolute());
    Assert.assertEquals(absoluteRoot.getNameCount(), 0);
    Assert.assertNull(path("a/b").getRoot());
  }

  public void testGetFileName () {

    Assert.assertEquals(path("/a/b/c").getFileName().toString(), "c");
    Assert.assertEquals(path("solo").getFileName().toString(), "solo");
  }

  public void testGetParent () {

    Assert.assertEquals(path("/a/b/c").getParent().toString(), "/a/b");
    Assert.assertNull(path("/").getParent());
    Assert.assertNull(path("a").getParent());
  }

  public void testGetName () {

    EphemeralPath path = path("/a/b/c");

    Assert.assertEquals(path.getName(0).toString(), "a");
    Assert.assertEquals(path.getName(1).toString(), "b");
    Assert.assertEquals(path.getName(2).toString(), "c");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetNameOutOfRange () {

    path("/a/b/c").getName(3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubpathInvalidRange () {

    path("/a/b/c").subpath(2, 1);
  }

  public void testStartsWith () {

    EphemeralPath path = path("/a/b/c");

    Assert.assertTrue(path.startsWith(path("/a/b")));
    Assert.assertTrue(path.startsWith(path("/a/b/c")));
    Assert.assertFalse(path.startsWith(path("/a/c")));
    Assert.assertFalse(path.startsWith(path("/a/b/c/d")));
  }

  public void testEndsWith () {

    EphemeralPath path = path("/a/b/c");

    Assert.assertTrue(path.endsWith(path("c")));
    Assert.assertTrue(path.endsWith(path("b/c")));
    Assert.assertTrue(path.endsWith(path("/a/b/c")));
    Assert.assertFalse(path.endsWith(path("b")));
    Assert.assertFalse(path("a/b/c").endsWith(path("/a/b/c")));
  }

  public void testNormalizeNoOp () {

    EphemeralPath path = path("/a/b/c");

    Assert.assertSame(path.normalize(), path);
  }

  public void testNormalizeDot () {

    Assert.assertEquals(path("/a/./b").normalize().toString(), "/a/b");
  }

  public void testNormalizeDotDot () {

    Assert.assertEquals(path("/a/b/../c").normalize().toString(), "/a/c");
  }

  public void testResolveAbsoluteOther () {

    Path resolved = path("/a/b").resolve(path("/x"));

    Assert.assertEquals(resolved.toString(), "/x");
  }

  public void testResolveRelativeOther () {

    Assert.assertEquals(path("/a/b").resolve(path("c/d")).toString(), "/a/b/c/d");
  }

  public void testRelativize () {

    Assert.assertEquals(path("/a/b").relativize(path("/a/c")).toString(), "../c");
    Assert.assertEquals(path("/a/b").relativize(path("/a/b/c/d")).toString(), "c/d");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRelativizeMismatchedAbsoluteness () {

    path("/a").relativize(path("b"));
  }

  public void testCompareTo () {

    Assert.assertEquals(path("/a/b").compareTo(path("/a/b")), 0);
    Assert.assertTrue(path("/a/b").compareTo(path("/a/c")) < 0);
    Assert.assertTrue(path("/a/c").compareTo(path("/a/b")) > 0);
    Assert.assertTrue(path("/a").compareTo(path("/a/b")) < 0);
  }

  public void testEqualsAndHashCode () {

    Assert.assertEquals(path("/a/b/c"), path("/a/b/c"));
    Assert.assertEquals(path("/a/b/c").hashCode(), path("/a/b/c").hashCode());
    Assert.assertNotEquals(path("/a/b/c"), path("/a/b/d"));
    Assert.assertNotEquals(path("/a/b/c"), path("a/b/c"));
  }

  public void testToAbsolutePath () {

    Assert.assertEquals(path("a/b").toAbsolutePath().toString(), "/a/b");

    EphemeralPath absolute = path("/a/b");

    Assert.assertSame(absolute.toAbsolutePath(), absolute);
  }

  @Test(expectedExceptions = NoSuchFileException.class)
  public void testToRealPathOfMissingFile ()
    throws IOException {

    path("/a/./b/../c").toRealPath();
  }

  public void testToRealPath ()
    throws IOException {

    Files.createDirectory(path("/a"));
    try {
      Files.writeString(path("/a/c"), "x");

      Assert.assertEquals(path("/a/./b/../c").toRealPath().toString(), "/a/c");
    } finally {
      ephemeralFileSystem.clear();
    }
  }

  public void testToUri () {

    Assert.assertEquals(path("/a/b").toUri().toString(), "ephemeral:///a/b");
    Assert.assertEquals(path("a/b").toUri().toString(), "ephemeral:///a/b");
  }
}
