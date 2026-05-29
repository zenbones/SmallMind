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
package org.smallmind.nutsnbolts.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ResourceParserTest {

  public void testFileSchemeProducesFileResource ()
    throws ResourceException {

    ResourceParser parser = new ResourceParser(new ResourceTypeResourceGenerator());

    Resource resource = parser.parseResource("file:/etc/app.conf");

    Assert.assertTrue(resource instanceof FileResource);
    Assert.assertEquals(resource.getScheme(), "file");
    Assert.assertEquals(resource.getPath(), "/etc/app.conf");
  }

  public void testClasspathSchemeProducesClasspathResource ()
    throws ResourceException {

    ResourceParser parser = new ResourceParser(new ResourceTypeResourceGenerator());

    Resource resource = parser.parseResource("classpath:config/app.yml");

    Assert.assertTrue(resource instanceof ClasspathResource);
    Assert.assertEquals(resource.getScheme(), "classpath");
    Assert.assertEquals(resource.getPath(), "config/app.yml");
  }

  public void testJarSchemeProducesJarResource ()
    throws ResourceException {

    ResourceParser parser = new ResourceParser(new ResourceTypeResourceGenerator());

    Resource resource = parser.parseResource("jar:/opt/app.jar!/META-INF/MANIFEST.MF");

    Assert.assertTrue(resource instanceof JarResource);
    Assert.assertEquals(resource.getPath(), "/opt/app.jar!/META-INF/MANIFEST.MF");
  }

  public void testIdentifierWithoutSchemeFallsBackToFile ()
    throws ResourceException {

    ResourceParser parser = new ResourceParser(new ResourceTypeResourceGenerator());

    Resource resource = parser.parseResource("/tmp/example");

    Assert.assertTrue(resource instanceof FileResource);
    Assert.assertEquals(resource.getPath(), "/tmp/example");
  }

  @Test(expectedExceptions = ResourceException.class)
  public void testUnknownSchemeThrows ()
    throws ResourceException {

    ResourceParser parser = new ResourceParser(new ResourceTypeResourceGenerator());

    parser.parseResource("nonsense:/etc/x");
  }

  public void testFileResourceReadsLocalFile ()
    throws IOException, ResourceException {

    Path tempFile = Files.createTempFile("nutsnbolts-resource", ".txt");
    Files.writeString(tempFile, "hello-resource", StandardCharsets.UTF_8);

    try {

      FileResource resource = new FileResource(tempFile.toString());

      try (InputStream stream = resource.getInputStream()) {
        Assert.assertEquals(new String(stream.readAllBytes(), StandardCharsets.UTF_8), "hello-resource");
      }
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test(expectedExceptions = ResourceException.class)
  public void testFileResourceMissingFileThrows ()
    throws ResourceException, IOException {

    FileResource resource = new FileResource("/no/such/file/" + System.nanoTime());

    try (InputStream stream = resource.getInputStream()) {
      Assert.fail("Expected ResourceException");
    }
  }

  public void testClasspathResourceFindsExistingEntry ()
    throws IOException {

    ClasspathResource resource = new ClasspathResource("org/smallmind/nutsnbolts/resource/ResourceParserTest.class");

    try (InputStream stream = resource.getInputStream()) {
      Assert.assertNotNull(stream);
      Assert.assertTrue(stream.read() != -1);
    }
  }

  public void testClasspathResourceReturnsNullForEmptyPath () {

    Assert.assertNull(new ClasspathResource("").getInputStream());
  }

  public void testClasspathResourceReturnsNullForMissingEntry () {

    Assert.assertNull(new ClasspathResource("no/such/classpath/entry.bin").getInputStream());
  }

  public void testClasspathResourceStripsLeadingSlash () {

    ClasspathResource resource = new ClasspathResource("/org/smallmind/nutsnbolts/resource/ResourceParserTest.class");

    Assert.assertNotNull(resource.getInputStream());
  }

  public void testResourceIdentifierIsSchemeColonPath () {

    FileResource resource = new FileResource("/etc/app.conf");

    Assert.assertEquals(resource.getIdentifier(), "file:/etc/app.conf");
    Assert.assertEquals(resource.toString(), "file:/etc/app.conf");
  }

  public void testResourceEqualityByValuesNotIdentity () {

    Assert.assertEquals(new FileResource("/a"), new FileResource("/a"));
    Assert.assertEquals(new FileResource("/a").hashCode(), new FileResource("/a").hashCode());
    Assert.assertNotEquals(new FileResource("/a"), new FileResource("/b"));
    Assert.assertNotEquals(new FileResource("/a"), new ClasspathResource("/a"));
  }
}
