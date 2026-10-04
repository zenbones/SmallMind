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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import org.testng.Assert;

@org.testng.annotations.Test(groups = "unit")
public class CachedJarFileTest {

  private static byte[] jarOf (Map<String, byte[]> entries, boolean stored)
    throws Exception {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream)) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {

        JarEntry jarEntry = new JarEntry(entry.getKey());

        if (stored) {

          CRC32 crc32 = new CRC32();

          crc32.update(entry.getValue());
          jarEntry.setMethod(ZipEntry.STORED);
          jarEntry.setSize(entry.getValue().length);
          jarEntry.setCompressedSize(entry.getValue().length);
          jarEntry.setCrc(crc32.getValue());
        }

        jarOutputStream.putNextEntry(jarEntry);
        jarOutputStream.write(entry.getValue());
        jarOutputStream.closeEntry();
      }
    }

    return byteArrayOutputStream.toByteArray();
  }

  private static Map<String, byte[]> textEntries (Map<String, String> entries) {

    LinkedHashMap<String, byte[]> byteEntries = new LinkedHashMap<>();

    for (Map.Entry<String, String> entry : entries.entrySet()) {
      byteEntries.put(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
    }

    return byteEntries;
  }

  private static CachedJarFile cacheOf (String entryName, Map<String, String> entries)
    throws Exception {

    return new CachedJarFile(entryName, jarOf(textEntries(entries), false));
  }

  private static byte[] prepend (byte[] prefix, byte[] jarBytes) {

    byte[] combined = new byte[prefix.length + jarBytes.length];

    System.arraycopy(prefix, 0, combined, 0, prefix.length);
    System.arraycopy(jarBytes, 0, combined, prefix.length, jarBytes.length);

    return combined;
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

  // JarOutputStream deflates by default and, because it streams, follows each entry with a data descriptor.
  public void testDeflatedEntriesInflateBackToTheirOriginalBytes ()
    throws Exception {

    LinkedHashMap<String, String> entries = new LinkedHashMap<>();

    entries.put("org/lib/Thing.class", "the original class bytes, compressed and inflated again");
    entries.put("resource.properties", "key=value");

    CachedJarFile cachedJarFile = cacheOf("thing.jar", entries);

    Assert.assertEquals(read(cachedJarFile.getInputStream("org/lib/Thing.class")), "the original class bytes, compressed and inflated again");
    Assert.assertEquals(read(cachedJarFile.getInputStream("resource.properties")), "key=value");
  }

  public void testStoredEntriesAreServedAsIs ()
    throws Exception {

    CachedJarFile cachedJarFile = new CachedJarFile("thing.jar", jarOf(textEntries(Map.of("plain.txt", "not compressed", "empty.txt", "")), true));

    Assert.assertEquals(read(cachedJarFile.getInputStream("plain.txt")), "not compressed");
    Assert.assertEquals(read(cachedJarFile.getInputStream("empty.txt")), "");
  }

  public void testDirectoryEntryYieldsAnEmptyStream ()
    throws Exception {

    CachedJarFile cachedJarFile = cacheOf("thing.jar", Map.of("org/", "", "org/a.txt", "A"));

    Assert.assertEquals(read(cachedJarFile.getInputStream("org/")), "");
    Assert.assertEquals(read(cachedJarFile.getInputStream("org/a.txt")), "A");
  }

  public void testEntryLargerThanTheReadBufferInflatesCompletely ()
    throws Exception {

    byte[] content = new byte[300_000];
    CachedJarFile cachedJarFile;

    new Random(7).nextBytes(content);
    for (int index = 0; index < content.length; index += 3) {
      content[index] = 'x';
    }

    cachedJarFile = new CachedJarFile("thing.jar", jarOf(Map.of("large.bin", content), false));

    try (InputStream inputStream = cachedJarFile.getInputStream("large.bin")) {
      Assert.assertEquals(inputStream.readAllBytes(), content);
    }
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

  public void testClosingAnEntryStreamTwiceIsHarmless ()
    throws Exception {

    InputStream inputStream = cacheOf("thing.jar", Map.of("a.txt", "AAA")).getInputStream("a.txt");

    inputStream.close();
    inputStream.close();
  }

  // More than 65,535 entries forces ZipOutputStream to write the ZIP64 end records.
  public void testZip64ArchiveIsIndexed ()
    throws Exception {

    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    CachedJarFile cachedJarFile;

    for (int index = 0; index < 70_000; index++) {
      entries.put("e/" + index + ".txt", Integer.toString(index).getBytes(StandardCharsets.UTF_8));
    }

    cachedJarFile = new CachedJarFile("big.jar", jarOf(entries, false));

    Assert.assertEquals(read(cachedJarFile.getInputStream("e/0.txt")), "0");
    Assert.assertEquals(read(cachedJarFile.getInputStream("e/65535.txt")), "65535");
    Assert.assertEquals(read(cachedJarFile.getInputStream("e/69999.txt")), "69999");
  }

  // Bytes ahead of the zip data (a launcher script, say) leave every recorded offset short by their length.
  public void testPrependedBytesAreTolerated ()
    throws Exception {

    CachedJarFile cachedJarFile = new CachedJarFile("thing.jar", prepend("#!/bin/sh\nexec java -jar \"$0\" \"$@\"\n".getBytes(StandardCharsets.UTF_8), jarOf(textEntries(Map.of("a.txt", "AAA", "b/c.txt", "CCC")), false)));

    Assert.assertEquals(read(cachedJarFile.getInputStream("a.txt")), "AAA");
    Assert.assertEquals(read(cachedJarFile.getInputStream("b/c.txt")), "CCC");
  }

  public void testPrependedBytesAreToleratedInAZip64Archive ()
    throws Exception {

    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    CachedJarFile cachedJarFile;

    for (int index = 0; index < 70_000; index++) {
      entries.put("e/" + index + ".txt", Integer.toString(index).getBytes(StandardCharsets.UTF_8));
    }

    cachedJarFile = new CachedJarFile("big.jar", prepend(new byte[1234], jarOf(entries, false)));

    Assert.assertEquals(read(cachedJarFile.getInputStream("e/69999.txt")), "69999");
  }

  public void testEncryptedEntryIsRejectedWhenRead ()
    throws Exception {

    byte[] jarBytes = jarOf(textEntries(Map.of("secret.txt", "S")), false);
    byte[] name = "secret.txt".getBytes(StandardCharsets.UTF_8);
    CachedJarFile cachedJarFile;

    // set the encryption bit in the entry's central directory header
    for (int position = 0; position + 46 + name.length <= jarBytes.length; position++) {
      if ((jarBytes[position] == 0x50) && (jarBytes[position + 1] == 0x4b) && (jarBytes[position + 2] == 0x01) && (jarBytes[position + 3] == 0x02)) {
        jarBytes[position + 8] |= 0x01;
      }
    }

    cachedJarFile = new CachedJarFile("thing.jar", jarBytes);

    Assert.assertThrows(ZipException.class, () -> cachedJarFile.getInputStream("secret.txt"));
  }

  public void testBytesThatAreNotAZipArchiveAreRejected () {

    Assert.assertThrows(ZipException.class, () -> new CachedJarFile("thing.jar", "this is not a jar".getBytes(StandardCharsets.UTF_8)));
    Assert.assertThrows(ZipException.class, () -> new CachedJarFile("thing.jar", new byte[0]));
  }
}
