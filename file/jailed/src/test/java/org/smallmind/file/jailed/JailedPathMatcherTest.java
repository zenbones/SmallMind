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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.regex.PatternSyntaxException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies that a jailed path matcher is interpreted in jail space rather than in the terms of
 * the backing native file system: the separator is the forward slash and matching is
 * case-sensitive, so the same pattern selects the same jailed paths on every host. Delegating to
 * the native matcher would instead inherit the host's separator and, on Windows, its
 * case-insensitivity.
 */
@Test(groups = "unit")
public class JailedPathMatcherTest {

  private FileSystem jailedFileSystem;

  @BeforeClass
  public void beforeClass () {

    jailedFileSystem = new JailedFileSystemProvider("jailed", new RootedPathTranslator(FileSystems.getDefault().getPath(System.getProperty("user.dir")))).getFileSystem(URI.create("jailed:///"));
  }

  private boolean matches (String syntaxAndPattern, String path) {

    PathMatcher pathMatcher = jailedFileSystem.getPathMatcher(syntaxAndPattern);

    return pathMatcher.matches(jailedFileSystem.getPath(path));
  }

  public void testSingleStarDoesNotCrossTheJailedSeparator () {

    Assert.assertTrue(matches("glob:*.txt", "notes.txt"));
    Assert.assertFalse(matches("glob:*.txt", "/a/notes.txt"));
    Assert.assertTrue(matches("glob:/a/*.txt", "/a/notes.txt"));
    Assert.assertFalse(matches("glob:/a/*.txt", "/a/b/notes.txt"));
  }

  public void testDoubleStarCrossesTheJailedSeparator () {

    Assert.assertTrue(matches("glob:**/*.txt", "/a/notes.txt"));
    Assert.assertTrue(matches("glob:**/*.txt", "/a/b/c/notes.txt"));
    Assert.assertTrue(matches("glob:/a/**", "/a/b/c"));
    Assert.assertFalse(matches("glob:/a/**", "/b/c"));
  }

  public void testQuestionMarkMatchesOneCharacterWithinAName () {

    Assert.assertTrue(matches("glob:/a/?/c", "/a/b/c"));
    Assert.assertFalse(matches("glob:/a/?/c", "/a/bb/c"));
    Assert.assertFalse(matches("glob:/a/?/c", "/a//c"));
  }

  public void testCharacterClasses () {

    Assert.assertTrue(matches("glob:/a/[a-c]eta", "/a/beta"));
    Assert.assertFalse(matches("glob:/a/[a-c]eta", "/a/zeta"));
    Assert.assertTrue(matches("glob:/a/[!a-c]eta", "/a/zeta"));
    Assert.assertFalse(matches("glob:/a/[!a-c]eta", "/a/beta"));
    Assert.assertTrue(matches("glob:/a/[abc]eta", "/a/beta"));
  }

  public void testAlternationGroups () {

    Assert.assertTrue(matches("glob:/a/{b,c}/d", "/a/b/d"));
    Assert.assertTrue(matches("glob:/a/{b,c}/d", "/a/c/d"));
    Assert.assertFalse(matches("glob:/a/{b,c}/d", "/a/e/d"));
    Assert.assertTrue(matches("glob:**.{txt,java}", "/a/Main.java"));
  }

  public void testMatchingIsCaseSensitive () {

    Assert.assertTrue(matches("glob:/a/beta", "/a/beta"));
    Assert.assertFalse(matches("glob:/a/Beta", "/a/beta"));
    Assert.assertFalse(matches("glob:/a/B*", "/a/beta"));
  }

  public void testEscapedMetaCharactersAreLiteral () {

    Assert.assertTrue(matches("glob:/a/\\*", "/a/*"));
    Assert.assertFalse(matches("glob:/a/\\*", "/a/beta"));
    Assert.assertTrue(matches("glob:/a/b.c", "/a/b.c"));
    Assert.assertFalse(matches("glob:/a/b.c", "/a/bxc"));
  }

  public void testRegexSyntax () {

    Assert.assertTrue(matches("regex:/a/[0-9]+", "/a/42"));
    Assert.assertFalse(matches("regex:/a/[0-9]+", "/a/x42"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingSyntaxRejected () {

    jailedFileSystem.getPathMatcher("**/*.txt");
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testUnknownSyntaxRejected () {

    jailedFileSystem.getPathMatcher("wildcard:*.txt");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testUnterminatedClassRejected () {

    jailedFileSystem.getPathMatcher("glob:/a/[abc");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testUnterminatedGroupRejected () {

    jailedFileSystem.getPathMatcher("glob:/a/{b,c");
  }

  @Test(expectedExceptions = PatternSyntaxException.class)
  public void testSeparatorInClassRejected () {

    jailedFileSystem.getPathMatcher("glob:/a/[b/c]");
  }
}
