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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the boundary between jail space, where a name element is opaque text delimited only
 * by the forward slash, and the backing native file system, where that same text may be an
 * entire path.
 *
 * <p>The expectation depends on the native file system, and this test states both halves. Where
 * names are separated by a backslash - Windows - text such as {@code a\b} or {@code C:} is
 * native path syntax and must be refused, as must the text that Win32 canonicalization would
 * rewrite ({@code ".. "}, which loses its trailing space and becomes {@code ".."}) or resolve to
 * a device ({@code NUL} in any directory). Where names are separated by a forward slash - Linux
 * and the other POSIX file systems - that same text is nothing more than an unusual but
 * perfectly legal file name, and must round-trip into the jail untouched.
 */
@Test(groups = "unit")
public class JailedNativeNameTest {

  /**
   * Text that is a single name element in jail space, but that carries native path syntax or
   * native canonicalization meaning on Windows.
   */
  private static final List<String> WINDOWS_SENSITIVE_NAMES = List.of("a\\b", "..\\..", "C:", "C:foo", "\\\\host\\share", "\\", ".. ", "...", "foo.", "a..", "foo ", "NUL", "nul.txt", "COM1", "con", "a<b", "a>b", "a|b", "a\"b", "a*b", "a?b", "a:b");

  /**
   * Text that is an unremarkable name element on every supported native file system.
   */
  private static final List<String> PORTABLE_NAMES = List.of("alpha", "a b", "a-b_c.d", "..a", "nullify", "communications", "\u00e4\u00f6\u00fc");

  private Path nativeRoot;
  private JailedPathTranslator translator;
  private JailedFileSystem jailedFileSystem;

  @BeforeMethod
  public void beforeMethod ()
    throws IOException {

    nativeRoot = Files.createTempDirectory("jailed-name-test-");
    translator = new RootedPathTranslator(nativeRoot);
    jailedFileSystem = (JailedFileSystem)new JailedFileSystemProvider("jailed", translator).getFileSystem(URI.create("jailed:///"));
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

  private boolean isBackslashSeparated () {

    return "\\".equals(FileSystems.getDefault().getSeparator());
  }

  private void assertRejected (String name) {

    try {
      translator.unwrapPath(jailedFileSystem.getPath("/" + name));
      Assert.fail("The name element(=" + name + ") should not have been accepted");
    } catch (SecurityException securityException) {
      // the jail refused to translate the name element, as expected
    } catch (IOException ioException) {
      Assert.fail("Unexpected exception for name element(=" + name + "): " + ioException);
    }
  }

  private void assertContainedAsSingleName (String name)
    throws IOException {

    Path nativePath = translator.unwrapPath(jailedFileSystem.getPath("/" + name));

    Assert.assertEquals(nativePath, nativeRoot.resolve(name));
    Assert.assertEquals(nativePath.getNameCount(), nativeRoot.getNameCount() + 1);
    Assert.assertEquals(nativePath.getFileName().toString(), name);
  }

  public void testNamesSensitiveToWindowsSyntax ()
    throws IOException {

    for (String name : WINDOWS_SENSITIVE_NAMES) {
      if (isBackslashSeparated()) {
        assertRejected(name);
      } else {
        assertContainedAsSingleName(name);
      }
    }
  }

  public void testPortableNamesAlwaysAccepted ()
    throws IOException {

    for (String name : PORTABLE_NAMES) {
      assertContainedAsSingleName(name);
    }
  }

  public void testNulCharacterAlwaysRejected () {

    assertRejected("a\u0000b");
  }

  public void testWindowsSyntaxRejectedForEveryOperation ()
    throws IOException {

    if (isBackslashSeparated()) {
      try {
        Files.createDirectory(jailedFileSystem.getPath("/..\\.."));
        Assert.fail("The name element should not have been accepted");
      } catch (SecurityException securityException) {
        // the jail refused the operation, as expected
      }

      try (Stream<Path> stream = Files.list(nativeRoot)) {
        Assert.assertEquals(stream.count(), 0L);
      }
    }
  }

  public void testLegalNameSurvivesAFullRoundTrip ()
    throws IOException {

    String name = isBackslashSeparated() ? "a b" : "a\\b";

    Files.createDirectory(jailedFileSystem.getPath("/" + name));

    Assert.assertTrue(Files.isDirectory(nativeRoot.resolve(name)));
    Assert.assertEquals(translator.wrapPath(jailedFileSystem, nativeRoot.resolve(name)).toString(), "/" + name);
  }
}
