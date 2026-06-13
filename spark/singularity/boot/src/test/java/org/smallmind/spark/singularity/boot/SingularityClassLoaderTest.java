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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

@org.testng.annotations.Test(groups = "unit")
public class SingularityClassLoaderTest {

  private static final String IMPLEMENTATION_TITLE = "Singularity Test Bundle";
  private static final String ALPHA_NAME = "org.smallmind.spark.singularity.boot.fixture.FixtureAlpha";
  private static final String BETA_NAME = "org.smallmind.spark.singularity.boot.fixture.FixtureBeta";
  private static final String FIXTURE_PACKAGE = "org.smallmind.spark.singularity.boot.fixture";

  private Path bundlePath;
  private SingularityClassLoader classLoader;

  @BeforeClass
  public void buildLoader ()
    throws Exception {

    SingularityIndex index = new SingularityIndex();

    index.addFileName("config/app.properties");
    index.addFileName("config/extra.txt");
    index.addFileName("top.txt");
    index.addInverseJarEntry("org/lib/Thing.class", "thing.jar");

    // A real class laid down directly in the outer jar exercises the jar: definition path; a real class drawn from a
    // nested library jar exercises the singularity: definition path. Both live in the same package so that the
    // first-time and already-present branches of package definition are both reached.
    index.addFileName(resourcePath(ALPHA_NAME));
    index.addInverseJarEntry(resourcePath(BETA_NAME), "fixtures.jar");

    Map<String, byte[]> bareEntries = new LinkedHashMap<>();

    bareEntries.put(resourcePath(ALPHA_NAME), classBytes(ALPHA_NAME));
    bareEntries.put("META-INF/singularity/lib/fixtures.jar", buildJar(Map.of(resourcePath(BETA_NAME), classBytes(BETA_NAME))));

    bundlePath = writeBundle(index, true, bareEntries);

    try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(bundlePath))) {
      classLoader = new SingularityClassLoader(null, jarInputStream.getManifest(), bundlePath.toUri().toURL(), jarInputStream);
    }
  }

  @AfterClass(alwaysRun = true)
  public void deleteBundle ()
    throws Exception {

    deleteBestEffort(bundlePath);
  }

  // The JDK's jar: URL handler caches the open JarFile once a class has been defined from it, which on Windows keeps
  // the bundle locked; the file is registered for deleteOnExit at creation, so an eager delete here is best effort.
  private static void deleteBestEffort (Path path) {

    if (path != null) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException ioException) {
        // left for the deleteOnExit hook registered when the bundle was written
      }
    }
  }

  private static String resourcePath (String binaryName) {

    return binaryName.replace('.', '/') + ".class";
  }

  private static byte[] classBytes (String binaryName)
    throws Exception {

    try (InputStream inputStream = SingularityClassLoaderTest.class.getResourceAsStream("/" + resourcePath(binaryName))) {
      Assert.assertNotNull(inputStream, "fixture not present on the test classpath: " + binaryName);

      return inputStream.readAllBytes();
    }
  }

  private static byte[] buildJar (Map<String, byte[]> entries)
    throws Exception {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream)) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
        jarOutputStream.write(entry.getValue());
        jarOutputStream.closeEntry();
      }
    }

    return byteArrayOutputStream.toByteArray();
  }

  private static Path writeBundle (SingularityIndex index, boolean includeIndex, Map<String, byte[]> bareEntries)
    throws Exception {

    return writeBundle(index, includeIndex, bareEntries, false);
  }

  private static Path writeBundle (SingularityIndex index, boolean includeIndex, Map<String, byte[]> bareEntries, boolean sealed)
    throws Exception {

    Manifest manifest = new Manifest();

    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_TITLE, IMPLEMENTATION_TITLE);

    if (sealed) {
      manifest.getMainAttributes().put(Attributes.Name.SEALED, "true");
    }

    Path path = Files.createTempFile("singularity-loader", ".jar");

    path.toFile().deleteOnExit();

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(path), manifest)) {
      if (includeIndex) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
          objectOutputStream.writeObject(index);
        }

        jarOutputStream.putNextEntry(new JarEntry("META-INF/singularity/index/singularity.idx"));
        jarOutputStream.write(byteArrayOutputStream.toByteArray());
        jarOutputStream.closeEntry();
      } else {
        jarOutputStream.putNextEntry(new JarEntry("placeholder.txt"));
        jarOutputStream.write("no index here".getBytes());
        jarOutputStream.closeEntry();
      }

      for (Map.Entry<String, byte[]> entry : bareEntries.entrySet()) {
        jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
        jarOutputStream.write(entry.getValue());
        jarOutputStream.closeEntry();
      }
    }

    return path;
  }

  private static int count (Enumeration<URL> enumeration) {

    int total = 0;

    while (enumeration.hasMoreElements()) {
      enumeration.nextElement();
      total++;
    }

    return total;
  }

  public void testBareFileResolvesThroughTheJarProtocol () {

    URL url = classLoader.findResource("config/app.properties");

    Assert.assertNotNull(url);
    Assert.assertEquals(url.getProtocol(), "jar");
  }

  public void testLibraryEntryResolvesThroughTheSingularityProtocol () {

    URL url = classLoader.findResource("org/lib/Thing.class");

    Assert.assertNotNull(url);
    Assert.assertEquals(url.getProtocol(), "singularity");
  }

  public void testLeadingSlashIsNormalizedAway () {

    Assert.assertEquals(classLoader.findResource("/config/app.properties"), classLoader.findResource("config/app.properties"));
  }

  public void testNullAndEmptyResourceNamesResolveToNothing () {

    Assert.assertNull(classLoader.findResource(null));
    Assert.assertNull(classLoader.findResource(""));
  }

  public void testUnknownResourceResolvesToNothing () {

    Assert.assertNull(classLoader.findResource("does/not/exist.txt"));
  }

  public void testDirectoryPrefixListsEveryFileBeneathIt () {

    Assert.assertEquals(count(classLoader.findResources("config/")), 2);
  }

  public void testDirectoryPrefixWithNoMatchesIsEmpty () {

    Assert.assertFalse(classLoader.findResources("missing/").hasMoreElements());
  }

  public void testExactNameYieldsASingleResource () {

    Assert.assertEquals(count(classLoader.findResources("top.txt")), 1);
  }

  public void testExactNameWithNoMatchIsEmpty () {

    Assert.assertFalse(classLoader.findResources("nope.txt").hasMoreElements());
  }

  public void testNullResourcesNameIsEmpty () {

    Assert.assertFalse(classLoader.findResources(null).hasMoreElements());
  }

  // The single-element enumeration handed back for an exact resource name must refuse a second draw.
  public void testSingleResourceEnumerationIsExhaustedAfterOneDraw () {

    Enumeration<URL> enumeration = classLoader.findResources("top.txt");

    Assert.assertTrue(enumeration.hasMoreElements());
    Assert.assertNotNull(enumeration.nextElement());
    Assert.assertFalse(enumeration.hasMoreElements());
    Assert.assertThrows(NoSuchElementException.class, enumeration::nextElement);
  }

  // Names in the JDK-shadowed namespaces are refused outright so the platform's own copy is used.
  public void testInoperableNamespaceIsRefused () {

    Assert.assertThrows(ClassNotFoundException.class, () -> classLoader.findClass("org.w3c.dom.Element"));
  }

  public void testOperableButUnmappedClassIsNotFound () {

    Assert.assertThrows(ClassNotFoundException.class, () -> classLoader.findClass("com.example.NotBundled"));
  }

  // A class stored directly in the outer jar must be defined from its jar: URL and be genuinely usable.
  public void testBundledClassDefinedThroughTheJarProtocol ()
    throws Exception {

    Class<?> alpha = classLoader.loadClass(ALPHA_NAME);

    Assert.assertEquals(alpha.getName(), ALPHA_NAME);
    Assert.assertSame(alpha.getClassLoader(), classLoader);
    Assert.assertEquals(invokePing(alpha), "alpha");
  }

  // A class drawn from a nested library jar must be defined from its singularity: URL and be genuinely usable.
  public void testBundledClassDefinedThroughTheSingularityProtocol ()
    throws Exception {

    Class<?> beta = classLoader.loadClass(BETA_NAME);

    Assert.assertEquals(beta.getName(), BETA_NAME);
    Assert.assertSame(beta.getClassLoader(), classLoader);
    Assert.assertEquals(invokePing(beta), "beta");
  }

  // findLoadedClass must short-circuit a repeat request rather than redefining the class.
  public void testRepeatLoadReturnsTheSameClass ()
    throws Exception {

    Assert.assertSame(classLoader.loadClass(ALPHA_NAME), classLoader.loadClass(ALPHA_NAME));
  }

  // Defining a bundled class also defines its enclosing package using the outer manifest's metadata.
  public void testEnclosingPackageIsDefinedFromTheManifest ()
    throws Exception {

    classLoader.loadClass(ALPHA_NAME);
    classLoader.loadClass(BETA_NAME);

    Package definedPackage = classLoader.getDefinedPackage(FIXTURE_PACKAGE);

    Assert.assertNotNull(definedPackage);
    Assert.assertEquals(definedPackage.getImplementationTitle(), IMPLEMENTATION_TITLE);
  }

  // With no parent configured, an unmapped class falls through to the system loader.
  public void testUnmappedClassFallsBackToTheSystemLoader ()
    throws Exception {

    Assert.assertSame(classLoader.loadClass("java.lang.String"), String.class);
  }

  // With a parent configured, an unmapped class is delegated to the parent rather than the system loader.
  public void testUnmappedClassDelegatesToTheParentWhenPresent ()
    throws Exception {

    try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(bundlePath))) {

      SingularityClassLoader childAwareLoader = new SingularityClassLoader(getClass().getClassLoader(), jarInputStream.getManifest(), bundlePath.toUri().toURL(), jarInputStream);

      Assert.assertSame(childAwareLoader.loadClass(Assert.class.getName()), Assert.class);
    }
  }

  // A Sealed:true manifest attribute seals every package the loader defines to the bundle URL.
  public void testSealedManifestSealsTheDefinedPackage ()
    throws Exception {

    SingularityIndex index = new SingularityIndex();

    index.addFileName(resourcePath(ALPHA_NAME));

    Path sealedBundle = writeBundle(index, true, Map.of(resourcePath(ALPHA_NAME), classBytes(ALPHA_NAME)), true);

    try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(sealedBundle))) {

      SingularityClassLoader loader = new SingularityClassLoader(null, jarInputStream.getManifest(), sealedBundle.toUri().toURL(), jarInputStream);

      loader.loadClass(ALPHA_NAME);

      Package definedPackage = loader.getDefinedPackage(FIXTURE_PACKAGE);

      Assert.assertNotNull(definedPackage);
      Assert.assertTrue(definedPackage.isSealed());
    } finally {
      deleteBestEffort(sealedBundle);
    }
  }

  public void testMissingIndexFailsConstruction ()
    throws Exception {

    Path indexlessBundle = writeBundle(new SingularityIndex(), false, Map.of());

    try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(indexlessBundle))) {

      Manifest manifest = jarInputStream.getManifest();
      URL jarURL = indexlessBundle.toUri().toURL();

      Assert.assertThrows(IOException.class, () -> new SingularityClassLoader(null, manifest, jarURL, jarInputStream));
    } finally {
      Files.deleteIfExists(indexlessBundle);
    }
  }

  // findResources for a directory prefix must skip directory placeholders and only list real files.
  public void testDirectoryPlaceholdersAreNotListed ()
    throws Exception {

    SingularityIndex index = new SingularityIndex();

    index.addFileName("nested/");
    index.addFileName("nested/leaf.txt");

    Path placeholderBundle = writeBundle(index, true, Map.of());

    try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(placeholderBundle))) {

      SingularityClassLoader loader = new SingularityClassLoader(null, jarInputStream.getManifest(), placeholderBundle.toUri().toURL(), jarInputStream);
      HashSet<String> protocols = new HashSet<>();
      Enumeration<URL> resources = loader.findResources("nested/");
      int total = 0;

      while (resources.hasMoreElements()) {
        protocols.add(resources.nextElement().getProtocol());
        total++;
      }

      Assert.assertEquals(total, 1);
      Assert.assertEquals(protocols, java.util.Set.of("jar"));
    } finally {
      Files.deleteIfExists(placeholderBundle);
    }
  }

  private static String invokePing (Class<?> clazz)
    throws Exception {

    Method ping = clazz.getMethod("ping");

    return (String)ping.invoke(null);
  }
}
