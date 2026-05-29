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
package org.smallmind.nutsnbolts.zip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CompressionTypeTest {

  private Path workRoot;

  @BeforeMethod
  public void setUp ()
    throws IOException {

    workRoot = Files.createTempDirectory("nutsnbolts-zip");
  }

  @AfterMethod
  public void tearDown ()
    throws IOException {

    if (workRoot != null) {
      try (var paths = Files.walk(workRoot)) {
        paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ioException) {
            // ignore
          }
        });
      }
    }
  }

  private Path createSourceTree ()
    throws IOException {

    Path source = workRoot.resolve("src");
    Path inner = source.resolve("inner");

    Files.createDirectories(inner);
    Files.writeString(source.resolve("top.txt"), "hello", StandardCharsets.UTF_8);
    Files.writeString(inner.resolve("nested.txt"), "world", StandardCharsets.UTF_8);

    return source;
  }

  public void testZipCompressAndExplodeRoundTrip ()
    throws IOException {

    Path source = createSourceTree();
    Path archive = workRoot.resolve("out.zip");
    Path extracted = workRoot.resolve("extracted");

    CompressionType.ZIP.compress(source, archive);

    Assert.assertTrue(Files.size(archive) > 0L);

    CompressionType.ZIP.explode(archive, extracted);

    Assert.assertEquals(Files.readString(extracted.resolve("top.txt"), StandardCharsets.UTF_8), "hello");
    Assert.assertEquals(Files.readString(extracted.resolve("inner/nested.txt"), StandardCharsets.UTF_8), "world");
  }

  public void testJarCompressAndExplodeRoundTrip ()
    throws IOException {

    Path source = createSourceTree();
    Path archive = workRoot.resolve("out.jar");
    Path extracted = workRoot.resolve("extracted");

    CompressionType.JAR.compress(source, archive);
    CompressionType.JAR.explode(archive, extracted);

    Assert.assertEquals(Files.readString(extracted.resolve("top.txt"), StandardCharsets.UTF_8), "hello");
    Assert.assertEquals(Files.readString(extracted.resolve("inner/nested.txt"), StandardCharsets.UTF_8), "world");
  }

  public void testWalkVisitsEveryEntry ()
    throws IOException {

    Path source = createSourceTree();
    Path archive = workRoot.resolve("walk.zip");

    CompressionType.ZIP.compress(source, archive);

    Set<String> entries = new HashSet<>();
    CompressionType.ZIP.walk(archive, entry -> entries.add(entry.getName()));

    Assert.assertTrue(entries.contains("top.txt"));
    Assert.assertTrue(entries.contains("inner/nested.txt"));
  }

  public void testGetEntryReturnsCorrectImplementation () {

    Assert.assertTrue(CompressionType.JAR.getEntry("x") instanceof JarEntry);
    ZipEntry zipEntry = CompressionType.ZIP.getEntry("x");
    Assert.assertFalse(zipEntry instanceof JarEntry);
  }

  public void testExtensionsExposed () {

    Assert.assertEquals(CompressionType.JAR.getExtension(), "jar");
    Assert.assertEquals(CompressionType.ZIP.getExtension(), "zip");
  }
}
