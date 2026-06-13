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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

@org.testng.annotations.Test(groups = "unit")
public class SingularityJarURLConnectionTest {

  private Path bundlePath;
  private String bundleUrlPart;

  // Forming a singularity: URL requires the synthetic protocol handler, registered once by the class loader's static
  // initializer; the bundle itself must be a real file because the connection reopens it through java.util.jar.JarFile.
  @BeforeClass
  public void buildBundle ()
    throws Exception {

    Class.forName(SingularityClassLoader.class.getName());

    bundlePath = Files.createTempFile("singularity-connection", ".jar");

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(bundlePath))) {
      jarOutputStream.putNextEntry(new JarEntry("hello.txt"));
      jarOutputStream.write("HELLO".getBytes(StandardCharsets.UTF_8));
      jarOutputStream.closeEntry();

      jarOutputStream.putNextEntry(new JarEntry("META-INF/singularity/lib/inner.jar"));
      jarOutputStream.write(innerJar());
      jarOutputStream.closeEntry();
    }

    bundleUrlPart = bundlePath.toUri().toURL().toExternalForm();
  }

  @AfterClass(alwaysRun = true)
  public void deleteBundle ()
    throws Exception {

    if (bundlePath != null) {
      Files.deleteIfExists(bundlePath);
    }
  }

  private static byte[] innerJar ()
    throws Exception {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream)) {
      jarOutputStream.putNextEntry(new JarEntry("deep.txt"));
      jarOutputStream.write("DEEP".getBytes(StandardCharsets.UTF_8));
      jarOutputStream.closeEntry();
    }

    return byteArrayOutputStream.toByteArray();
  }

  private SingularityJarURLConnection connectionFor (String spec)
    throws Exception {

    return new SingularityJarURLConnection(URI.create("singularity:" + bundleUrlPart + spec).toURL());
  }

  private static String read (InputStream inputStream)
    throws Exception {

    try (InputStream guarded = inputStream) {
      return new String(guarded.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  public void testReadsEntryStoredDirectlyInTheOuterJar ()
    throws Exception {

    Assert.assertEquals(read(connectionFor("@/hello.txt").getInputStream()), "HELLO");
  }

  public void testReadsEntryNestedInsideABundledLibraryJar ()
    throws Exception {

    Assert.assertEquals(read(connectionFor("@/META-INF/singularity/lib/inner.jar!/deep.txt").getInputStream()), "DEEP");
  }

  public void testMissingSeparatorIsMalformed ()
    throws Exception {

    Assert.assertThrows(MalformedURLException.class, () -> connectionFor("/no-at-sign").getInputStream());
  }

  public void testAbsentDirectEntryIsNotFound ()
    throws Exception {

    Assert.assertThrows(FileNotFoundException.class, () -> connectionFor("@/missing.txt").getInputStream());
  }

  public void testAbsentNestedEntryIsNotFound ()
    throws Exception {

    Assert.assertThrows(FileNotFoundException.class, () -> connectionFor("@/META-INF/singularity/lib/inner.jar!/missing.txt").getInputStream());
  }

  public void testAbsentLibraryJarIsNotFound ()
    throws Exception {

    Assert.assertThrows(FileNotFoundException.class, () -> connectionFor("@/META-INF/singularity/lib/absent.jar!/deep.txt").getInputStream());
  }

  // The second read of an entry inside the same library jar is served from the soft-reference cache rather than
  // re-inflating the nested jar, and must still yield the same content.
  public void testNestedEntryIsServedFromCacheOnRepeatReads ()
    throws Exception {

    Assert.assertEquals(read(connectionFor("@/META-INF/singularity/lib/inner.jar!/deep.txt").getInputStream()), "DEEP");
    Assert.assertEquals(read(connectionFor("@/META-INF/singularity/lib/inner.jar!/deep.txt").getInputStream()), "DEEP");
  }

  public void testConnectIsANoOpAndContentLengthIsIndeterminate ()
    throws Exception {

    URL url = URI.create("singularity:" + bundleUrlPart + "@/hello.txt").toURL();
    SingularityJarURLConnection connection = new SingularityJarURLConnection(url);

    connection.connect();
    Assert.assertEquals(connection.getContentLength(), 0);
  }
}
