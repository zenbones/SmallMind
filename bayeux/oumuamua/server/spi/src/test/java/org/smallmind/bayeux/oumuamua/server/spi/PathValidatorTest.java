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
package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PathValidatorTest {

  public void testSingleSegment ()
    throws InvalidPathException {

    int[] segments = PathValidator.validate("/foo");

    Assert.assertEquals(segments.length, 0);
  }

  public void testMultipleSegments ()
    throws InvalidPathException {

    int[] segments = PathValidator.validate("/foo/bar/baz");

    Assert.assertEquals(segments.length, 2);
    Assert.assertEquals(segments[0], 4);
    Assert.assertEquals(segments[1], 8);
  }

  public void testPermittedSpecialCharacters ()
    throws InvalidPathException {

    PathValidator.validate("/a-b/c.d/e_f/g+h/i@j/k!l/m#n/o$p/q(r)/s{t}/u~v");
  }

  public void testAlphanumericSegments ()
    throws InvalidPathException {

    PathValidator.validate("/abc123/XYZ789/0a1b2c");
  }

  public void testSingleLevelWildcard ()
    throws InvalidPathException {

    PathValidator.validate("/foo/*");
  }

  public void testDeepWildcard ()
    throws InvalidPathException {

    PathValidator.validate("/foo/**");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testNullPath ()
    throws InvalidPathException {

    PathValidator.validate(null);
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testEmptyPath ()
    throws InvalidPathException {

    PathValidator.validate("");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testJustRoot ()
    throws InvalidPathException {

    PathValidator.validate("/");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testMissingLeadingSlash ()
    throws InvalidPathException {

    PathValidator.validate("foo/bar");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testEmptyMiddleSegment ()
    throws InvalidPathException {

    PathValidator.validate("/foo//bar");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testTrailingSlash ()
    throws InvalidPathException {

    PathValidator.validate("/foo/");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testIllegalCharacterColon ()
    throws InvalidPathException {

    PathValidator.validate("/foo:bar");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testIllegalCharacterComma ()
    throws InvalidPathException {

    PathValidator.validate("/foo,bar");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testTripleAsteriskWildcard ()
    throws InvalidPathException {

    PathValidator.validate("/foo/***");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testWildcardWithExtraCharacters ()
    throws InvalidPathException {

    PathValidator.validate("/foo/*bar");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testWildcardNotFinalSegment ()
    throws InvalidPathException {

    PathValidator.validate("/foo/*/bar");
  }

  public void testSpaceIsPermitted ()
    throws InvalidPathException {

    PathValidator.validate("/a b");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testDeepWildcardWithExtraCharacters ()
    throws InvalidPathException {

    PathValidator.validate("/foo/**bar");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testDeepWildcardNotFinalSegment ()
    throws InvalidPathException {

    PathValidator.validate("/foo/**/bar");
  }
}
