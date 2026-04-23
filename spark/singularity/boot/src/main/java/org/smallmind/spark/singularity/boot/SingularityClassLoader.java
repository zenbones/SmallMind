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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Class loader that resolves classes and resources out of a Singularity bundle. The bundle packages application
 * classes, boot classes, and every runtime dependency (as nested jars under {@code META-INF/singularity/lib/}) into
 * a single outer jar, and is navigated via the serialized {@link SingularityIndex} written at build time.
 * <p>On first touch the loader installs a {@link SingularityJarURLStreamHandlerFactory} so that {@code singularity:}
 * URLs generated from the index can be opened. Classes in certain JDK-shadowed namespaces (XML/W3C, for example) are
 * refused so that platform-provided implementations are used instead.
 */
public class SingularityClassLoader extends ClassLoader {

  private static final PermissionCollection ALL_PERMISSION_COLLECTION;
  private static final String[] INOPERABLE_NAMESPACES = new String[] {"jakarta.xml.", "org.xml.", "org.w3c."};
  private static final String[] OPERABLE_NAMESPACES = new String[] {"jakarta.xml.bind."};
  private final Map<String, URL> urlMap;
  private final HashSet<String> packageSet = new HashSet<>();
  private final URL sealBase;
  private final String specificationTitle;
  private final String specificationVersion;
  private final String specificationVendor;
  private final String implementationTitle;
  private final String implementationVersion;
  private final String implementationVendor;

  static {

    AllPermission allPermission = new AllPermission();

    ALL_PERMISSION_COLLECTION = allPermission.newPermissionCollection();
    ALL_PERMISSION_COLLECTION.add(allPermission);

    ClassLoader.registerAsParallelCapable();
    URL.setURLStreamHandlerFactory(new SingularityJarURLStreamHandlerFactory());
  }

  /**
   * Reads the Singularity index from the jar and materializes the URL table used to resolve every class and resource
   * that the bundle provides.
   *
   * @param parent         optional parent class loader; {@code null} delegates to the system loader for fallthrough
   * @param manifest       the outer jar's manifest, consulted for package metadata and the optional {@code Sealed} attribute
   * @param jarURL         the URL of the outer jar, used both to anchor generated entry URLs and to seal packages
   * @param jarInputStream an open stream over the outer jar; advanced until the index entry is located and read
   * @throws IOException            if the {@code META-INF/singularity/index/singularity.idx} entry cannot be located or deserialized
   * @throws ClassNotFoundException if deserializing the index references a class that cannot be loaded
   */
  public SingularityClassLoader (ClassLoader parent, Manifest manifest, URL jarURL, JarInputStream jarInputStream)
    throws IOException, ClassNotFoundException {

    super(parent);

    HashMap<String, URL> underConstructionMap = new HashMap<>();
    SingularityIndex singularityIndex = null;
    Attributes mainAttributes = manifest.getMainAttributes();
    JarEntry jarEntry;
    String sealed;

    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
      if (!jarEntry.isDirectory()) {
        if (jarEntry.getName().equals("META-INF/singularity/index/singularity.idx")) {
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

          int singleByte;

          while ((singleByte = jarInputStream.read()) >= 0) {
            byteArrayOutputStream.write(singleByte);
          }
          byteArrayOutputStream.close();

          try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            singularityIndex = (SingularityIndex)objectInputStream.readObject();
          }
          break;
        }
      }
    }

    if (singularityIndex == null) {
      throw new IOException("Missing singularity index");
    }

    for (SingularityIndex.URLEntry urlEntry : singularityIndex.getJarURLEntryIterable(jarURL.toExternalForm())) {
      underConstructionMap.put(urlEntry.entryName(), urlEntry.entryURL());
    }
    for (SingularityIndex.URLEntry urlEntry : singularityIndex.getSingularityURLEntryIterable(jarURL.toExternalForm())) {
      underConstructionMap.put(urlEntry.entryName(), urlEntry.entryURL());
    }

    specificationTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
    specificationVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
    specificationVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
    implementationTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
    implementationVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    implementationVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);

    if ((sealed = mainAttributes.getValue(Attributes.Name.SEALED)) != null) {
      if (Boolean.parseBoolean(sealed)) {
        sealBase = jarURL;
      } else {
        sealBase = null;
      }
    } else {
      sealBase = null;
    }

    urlMap = Collections.unmodifiableMap(underConstructionMap);
  }

  /**
   * Resolves a class, preferring bundled definitions and falling back to the parent loader (or the system loader
   * when no parent is configured).
   *
   * @param name    fully qualified binary class name
   * @param resolve {@code true} to link the class after loading
   * @return the loaded {@link Class}
   * @throws ClassNotFoundException if no definition is available from this loader, its parent, or the system loader
   */
  @Override
  protected synchronized Class<?> loadClass (String name, boolean resolve)
    throws ClassNotFoundException {

    Class<?> singularityClass;

    if ((singularityClass = findLoadedClass(name)) == null) {
      try {
        singularityClass = findClass(name);
      } catch (ClassNotFoundException c) {
        if (getParent() != null) {
          singularityClass = getParent().loadClass(name);
        } else {
          singularityClass = findSystemClass(name);
        }
      }
    }

    if (resolve) {
      resolveClass(singularityClass);
    }

    return singularityClass;
  }

  /**
   * Reads the class bytes from the bundle and defines the class, taking care to construct a {@link CodeSource} whose
   * URL survives the quirks of {@code javax.crypto.JarVerifier} when the entry lives behind a {@code singularity:}
   * URL.
   *
   * @param name fully qualified binary class name
   * @return the newly defined class
   * @throws ClassNotFoundException if the class is not mapped by this loader, lives in a reserved namespace, or
   *                                cannot be read, deserialized, or defined for any reason
   */
  @Override
  protected Class<?> findClass (String name)
    throws ClassNotFoundException {

    if (isOperableNamespace(name)) {

      URL classURL;
      URL codeSourceUrl;

      if ((classURL = urlMap.get(name.replace('.', '/') + ".class")) != null) {
        try {

          String classURLExternalForm = classURL.toExternalForm();

          switch (classURL.getProtocol()) {
            case "jar":

              int jarBangSlashIndex;

              if ((jarBangSlashIndex = classURLExternalForm.indexOf("!/")) < 0) {
                codeSourceUrl = URI.create(classURLExternalForm + "!/").toURL();
              } else {
                codeSourceUrl = URI.create(classURLExternalForm.substring(0, jarBangSlashIndex + 2)).toURL();
              }
              break;
            case "singularity":
              // javax.crypto.JarVerifier will encase this URL in 'jar:<code source url>!/ (it incorrectly assumes any protocol not 'jar:' is 'file:'),
              // which then gets stripped again by JarUrlConnection, which will let SingularityJarURLConnection respond correctly...
              codeSourceUrl = URI.create(classURLExternalForm.substring(0, classURLExternalForm.indexOf("!/"))).toURL();
              break;
            default:
              throw new MalformedURLException("Unknown class url protocol(" + classURL.getProtocol() + ")");
          }

          CodeSource codeSource = new CodeSource(codeSourceUrl, (Certificate[])null);
          ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, ALL_PERMISSION_COLLECTION, this, null);
          InputStream classInputStream;
          byte[] classData;

          classInputStream = classURL.openStream();
          classData = getClassData(classInputStream);
          classInputStream.close();

          definePackage(name);

          return defineClass(name, classData, 0, classData.length, protectionDomain);
        } catch (Exception exception) {
          throw new ClassNotFoundException("Exception encountered while attempting to define class (" + name + ")", exception);
        }
      }
    }

    throw new ClassNotFoundException(name);
  }

  //TODO: Implement to load classes in modules (return null on not found)
  /*
  @Override
  protected Class<?> findClass (String moduleName, String name) {

    return super.findClass(moduleName, name);
  }
  */

  /**
   * Decides whether a class name falls within the subset this loader is allowed to resolve.
   * <p>Names matching an entry in the allow list are admitted unconditionally; names matching an entry in the deny
   * list are refused so that the platform's own copy is used; everything else is admitted.
   *
   * @param name fully qualified binary class name
   * @return {@code true} if this loader is willing to define the class, {@code false} if it must defer
   */
  private boolean isOperableNamespace (String name) {

    for (String operableNamespace : OPERABLE_NAMESPACES) {
      if (name.startsWith(operableNamespace)) {

        return true;
      }
    }
    for (String inoperableNamespace : INOPERABLE_NAMESPACES) {
      if (name.startsWith(inoperableNamespace)) {

        return false;
      }
    }

    return true;
  }

  /**
   * Defines the enclosing package the first time a class from that package is loaded, applying specification and
   * implementation metadata from the outer jar's manifest and the optional seal base.
   *
   * @param name fully qualified binary class name; its enclosing package is the text preceding the last dot
   */
  private synchronized void definePackage (String name) {

    String packageName;
    int lastDotPos = name.lastIndexOf('.');

    packageName = name.substring(0, lastDotPos);
    if (packageSet.add(packageName)) {
      definePackage(packageName, specificationTitle, specificationVersion, specificationVendor, implementationTitle, implementationVersion, implementationVendor, sealBase);
    }
  }

  /**
   * Slurps a stream of class bytes into a single byte array.
   *
   * @param classInputStream stream over the raw class file bytes
   * @return every byte read from the stream, ready to hand to {@link ClassLoader#defineClass}
   * @throws IOException if reading from the stream fails
   */
  private byte[] getClassData (InputStream classInputStream)
    throws IOException {

    ByteArrayOutputStream classDataOutputStream = new ByteArrayOutputStream();
    int singleByte;

    while ((singleByte = classInputStream.read()) >= 0) {
      classDataOutputStream.write(singleByte);
    }

    return classDataOutputStream.toByteArray();
  }

  /**
   * Looks up a single resource in the index, normalizing an optional leading slash.
   *
   * @param name resource name, with or without a leading {@code '/'}
   * @return the URL recorded for that resource, or {@code null} if the name is empty or unknown
   */
  @Override
  protected URL findResource (String name) {

    if ((name == null) || name.isEmpty()) {

      return null;
    } else {
      return urlMap.get((name.charAt(0) == '/') ? name.substring(1) : name);
    }
  }

  //TODO: Implement to find resources in modules
  /*
  @Override
  protected URL findResource (String moduleName, String name)
    throws IOException {

    return super.findResource(moduleName, name);
  }
  */

  /**
   * Enumerates matching resources. For names ending in a slash this method behaves as a directory listing: every
   * indexed file that begins with the given prefix (excluding directory placeholders) is returned.
   *
   * @param name resource name or directory-style prefix
   * @return an {@link Enumeration} of matching URLs, possibly empty
   */
  @Override
  protected Enumeration<URL> findResources (String name) {

    if ((name == null) || name.isEmpty()) {

      return Collections.emptyEnumeration();
    } else if (!name.endsWith("/")) {

      URL url;

      if ((url = findResource(name)) == null) {

        return Collections.emptyEnumeration();
      }

      return new SingleEnumeration<>(url);
    } else {

      LinkedList<URL> urlList = new LinkedList<>();

      for (Map.Entry<String, URL> resourceEntry : urlMap.entrySet()) {
        if (resourceEntry.getKey().startsWith(name) && (!resourceEntry.getKey().endsWith("/"))) {
          urlList.add(resourceEntry.getValue());
        }
      }

      if (urlList.isEmpty()) {

        return Collections.emptyEnumeration();
      } else {

        URL[] urls = new URL[urlList.size()];

        urlList.toArray(urls);

        return new ArrayEnumeration<>(urls);
      }
    }
  }

  /**
   * Single-shot {@link Enumeration} used when exactly one resource must be exposed to an API that requires an
   * enumeration.
   *
   * @param <T> the element type
   */
  private static class SingleEnumeration<T> implements Enumeration<T> {

    private final T value;
    private boolean used = false;

    /**
     * Stores the sole value to hand out.
     *
     * @param value the only element the enumeration will ever return
     */
    private SingleEnumeration (T value) {

      this.value = value;
    }

    /**
     * Reports whether the value is still available.
     *
     * @return {@code true} until {@link #nextElement()} has consumed the value
     */
    @Override
    public synchronized boolean hasMoreElements () {

      return !used;
    }

    /**
     * Returns the value once, then refuses further calls.
     *
     * @return the single element supplied at construction
     * @throws NoSuchElementException on every call after the first
     */
    @Override
    public synchronized T nextElement () {

      if (used) {
        throw new NoSuchElementException();
      }

      used = true;

      return value;
    }
  }

  /**
   * {@link Enumeration} view over a fixed-size array.
   *
   * @param <T> the element type
   */
  private static class ArrayEnumeration<T> implements Enumeration<T> {

    private final T[] values;
    private int index = 0;

    /**
     * Wraps the supplied array; the array is not copied, so callers must not mutate it afterward.
     *
     * @param values backing array whose elements will be returned in order
     */
    private ArrayEnumeration (T[] values) {

      this.values = values;
    }

    /**
     * Reports whether additional elements remain.
     *
     * @return {@code true} while the cursor has not yet reached the end of the array
     */
    @Override
    public boolean hasMoreElements () {

      return index < values.length;
    }

    /**
     * Advances the cursor and returns the next element.
     *
     * @return the next array element in order
     */
    @Override
    public T nextElement () {

      return values[index++];
    }
  }
}
