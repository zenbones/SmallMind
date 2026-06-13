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
package org.smallmind.spark.singularity.boot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.testng.Assert;

@org.testng.annotations.Test(groups = "unit")
public class CachedJarFileTest {

  private static CachedJarFile cacheOf (String entryName, Map<String, String> entries)
    throws Exception {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
        jarOutputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        jarOutputStream.closeEntry();
      }
    }

    return new CachedJarFile(entryName, new JarInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
  }

  private static String read (InputStream inputStream)
    throws Exception {

    try (InputStream guarded = inputStream) {
      return new String(guarded.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  public void testEntryNameIsPreserved ()
    throws Exception {

    CachedJarFile cachedJarFile = cacheOf("META-INF/singularity/lib/thing.jar", Map.of("a.txt", "AAA"));

    Assert.assertEquals(cachedJarFile.getEntryName(), "META-INF/singularity/lib/thing.jar");
  }

  public void testStoredEntriesInflateBackToTheirOriginalBytes ()
    throws Exception {

    LinkedHashMap<String, String> entries = new LinkedHashMap<>();

    entries.put("org/lib/Thing.class", "the original class bytes, compressed and inflated again");
    entries.put("resource.properties", "key=value");

    CachedJarFile cachedJarFile = cacheOf("thing.jar", entries);

    Assert.assertEquals(read(cachedJarFile.getInputStream("org/lib/Thing.class")), "the original class bytes, compressed and inflated again");
    Assert.assertEquals(read(cachedJarFile.getInputStream("resource.properties")), "key=value");
  }

  public void testUnknownEntryReturnsNull ()
    throws Exception {

    CachedJarFile cachedJarFile = cacheOf("thing.jar", Map.of("present.txt", "here"));

    Assert.assertNull(cachedJarFile.getInputStream("absent.txt"));
  }

  // Each call must hand back an independent stream positioned at the start, so a second read sees the full content.
  public void testEachLookupYieldsAFreshStream ()
    throws Exception {

    CachedJarFile cachedJarFile = cacheOf("thing.jar", Map.of("a.txt", "REPEATABLE"));

    Assert.assertEquals(read(cachedJarFile.getInputStream("a.txt")), "REPEATABLE");
    Assert.assertEquals(read(cachedJarFile.getInputStream("a.txt")), "REPEATABLE");
  }
}
