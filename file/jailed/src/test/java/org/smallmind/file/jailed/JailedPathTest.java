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

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JailedPathTest {

  private JailedFileSystem jailedFileSystem;

  @BeforeClass
  public void beforeClass () {

    JailedFileSystemProvider provider = new JailedFileSystemProvider("jailed", new RootedPathTranslator(FileSystems.getDefault().getPath(System.getProperty("user.dir"))));

    jailedFileSystem = (JailedFileSystem)provider.getFileSystem(URI.create("jailed:///"));
  }

  private JailedPath path (String text) {

    return new JailedPath(jailedFileSystem, text);
  }

  public void testConstructionFromString () {

    JailedPath path = path("/a/b/c");

    Assert.assertEquals(path.toString(), "/a/b/c");
    Assert.assertTrue(path.isAbsolute());
    Assert.assertEquals(path.getNameCount(), 3);
    Assert.assertSame(path.getFileSystem(), jailedFileSystem);
  }

  public void testConstructionFromChars () {

    JailedPath path = new JailedPath(jailedFileSystem, '/', 'x', '/', 'y');

    Assert.assertEquals(path.toString(), "/x/y");
    Assert.assertTrue(path.isAbsolute());
    Assert.assertEquals(path.getNameCount(), 2);
  }

  public void testEmptyPath () {

    JailedPath path = path("");

    Assert.assertEquals(path.toString(), "");
    Assert.assertFalse(path.isAbsolute());
    Assert.assertEquals(path.getNameCount(), 0);
    Assert.assertNull(path.getRoot());
    Assert.assertNull(path.getFileName());
    Assert.assertNull(path.getParent());
  }

  public void testRootPath () {

    JailedPath root = path("/");

    Assert.assertTrue(root.isAbsolute());
    Assert.assertEquals(root.getNameCount(), 0);
    Assert.assertNotNull(root.getRoot());
    Assert.assertEquals(root.getRoot().toString(), "/");
    Assert.assertNull(root.getFileName());
    Assert.assertNull(root.getParent());
  }

  public void testRelativePath () {

    JailedPath path = path("a/b/c");

    Assert.assertFalse(path.isAbsolute());
    Assert.assertNull(path.getRoot());
    Assert.assertEquals(path.getNameCount(), 3);
  }

  public void testCollapsedSeparators () {

    JailedPath path = path("//a//b//");

    Assert.assertTrue(path.isAbsolute());
    Assert.assertEquals(path.getNameCount(), 2);
    Assert.assertEquals(path.getName(0).toString(), "a");
    Assert.assertEquals(path.getName(1).toString(), "b");
  }

  public void testGetFileName () {

    Assert.assertEquals(path("/a/b/c").getFileName().toString(), "c");
    Assert.assertEquals(path("solo").getFileName().toString(), "solo");
  }

  public void testGetParent () {

    Assert.assertEquals(path("/a/b/c").getParent().toString(), "/a/b");
    Assert.assertEquals(path("/a").getParent().toString(), "/");
    Assert.assertNull(path("/").getParent());
  }

  public void testGetName () {

    JailedPath path = path("/a/b/c");

    Assert.assertEquals(path.getName(0).toString(), "a");
    Assert.assertEquals(path.getName(1).toString(), "b");
    Assert.assertEquals(path.getName(2).toString(), "c");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetNameOutOfRange () {

    path("/a/b/c").getName(3);
  }

  public void testSubpath () {

    JailedPath path = path("/a/b/c/d");
    Path sub = path.subpath(1, 3);

    Assert.assertEquals(sub.toString(), "b/c");
    Assert.assertFalse(sub.isAbsolute());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubpathInvalidRange () {

    path("/a/b/c").subpath(2, 1);
  }

  public void testStartsWith () {

    JailedPath path = path("/a/b/c");

    Assert.assertTrue(path.startsWith(path("/a")));
    Assert.assertTrue(path.startsWith(path("/a/b")));
    Assert.assertTrue(path.startsWith(path("/a/b/c")));
    Assert.assertFalse(path.startsWith(path("/a/c")));
    Assert.assertFalse(path.startsWith(path("a/b")));
    Assert.assertFalse(path.startsWith(path("/a/b/c/d")));
  }

  public void testEndsWith () {

    JailedPath path = path("/a/b/c");

    Assert.assertTrue(path.endsWith(path("c")));
    Assert.assertTrue(path.endsWith(path("b/c")));
    Assert.assertTrue(path.endsWith(path("/a/b/c")));
    Assert.assertFalse(path.endsWith(path("b")));
    Assert.assertFalse(path.endsWith(path("/b/c")));
    Assert.assertFalse(path("a/b/c").endsWith(path("/a/b/c")));
  }

  public void testNormalizeNoOp () {

    JailedPath path = path("/a/b/c");

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

  public void testResolveEmptyOther () {

    JailedPath base = path("/a/b");

    Assert.assertSame(base.resolve(path("")), base);
  }

  public void testResolveRelativeOther () {

    Assert.assertEquals(path("/a/b").resolve(path("c/d")).toString(), "/a/b/c/d");
  }

  public void testResolveOntoEmpty () {

    Assert.assertEquals(path("").resolve(path("a/b")).toString(), "a/b");
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
    Assert.assertTrue(path("a").compareTo(path("/a")) < 0);
  }

  public void testToAbsolutePath () {

    Assert.assertEquals(path("a/b").toAbsolutePath().toString(), "/a/b");
    JailedPath absolute = path("/a/b");
    Assert.assertSame(absolute.toAbsolutePath(), absolute);
  }

  public void testToRealPath () {

    Assert.assertEquals(path("/a/./b/../c").toRealPath().toString(), "/a/c");
  }

  public void testToUri () {

    Assert.assertEquals(path("/a/b").toUri().toString(), "jailed:///a/b");
    Assert.assertEquals(path("a/b").toUri().toString(), "jailed:///a/b");
  }

  @Test(expectedExceptions = ProviderMismatchException.class)
  public void testStartsWithForeignPath () {

    path("/a").startsWith(Paths.get("/a"));
  }

  @Test(expectedExceptions = ProviderMismatchException.class)
  public void testEndsWithForeignPath () {

    path("/a").endsWith(Paths.get("/a"));
  }

  @Test(expectedExceptions = ProviderMismatchException.class)
  public void testResolveForeignPath () {

    path("/a").resolve(Paths.get("b"));
  }

  @Test(expectedExceptions = ProviderMismatchException.class)
  public void testRelativizeForeignPath () {

    path("/a").relativize(Paths.get("/b"));
  }

  @Test(expectedExceptions = ProviderMismatchException.class)
  public void testCompareToForeignPath () {

    path("/a").compareTo(Paths.get("/a"));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRegisterUnsupported ()
    throws Exception {

    path("/a").register(null, new java.nio.file.WatchEvent.Kind<?>[0]);
  }
}
