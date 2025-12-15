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
package org.smallmind.nutsnbolts.lang;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.smallmind.nutsnbolts.io.PathUtility;

/**
 * {@link ClassGate} implementation that resolves classes and resources from a set of
 * classpath components (directories or JARs). Provides streams to class bytes as well
 * as resource URLs and timestamps for file-backed entries.
 */
public class ClasspathClassGate implements ClassGate {

  private final HashMap<String, Path> filePathMap;

  private final String[] pathComponents;

  /**
   * Builds a gate that uses the JVM {@code java.class.path} system property.
   */
  public ClasspathClassGate () {

    this(System.getProperty("java.class.path"));
  }

  /**
   * Builds a gate using a raw class path string delimited by the platform path separator.
   *
   * @param classPath the class path string (directories or JARs)
   */
  public ClasspathClassGate (String classPath) {

    this(classPath.split(System.getProperty("path.separator"), -1));
  }

  /**
   * Builds a gate from explicit path components.
   *
   * @param pathComponents directories or JAR files used for resolution
   */
  public ClasspathClassGate (String... pathComponents) {

    this.pathComponents = pathComponents;

    filePathMap = new HashMap<>();
  }

  /**
   * Returns the last modification date for a previously located class.
   *
   * @param name the fully qualified class name
   * @return the timestamp in milliseconds, or {@link #STATIC_CLASS} when unknown
   * @throws IOException if file metadata cannot be read
   */
  public long getLastModDate (String name)
    throws IOException {

    Path filePath;

    synchronized (filePathMap) {
      if ((filePath = filePathMap.get(name)) != null) {

        return Files.getLastModifiedTime(filePath).toMillis();
      }
    }

    return ClassGate.STATIC_CLASS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CodeSource getCodeSource () {

    return null;
  }

  /**
   * Locates a class on the configured path components and returns a stream ticket to it.
   *
   * @param name the fully qualified class name
   * @return a {@link ClassStreamTicket} containing the stream and timestamp, or {@code null} if not found
   * @throws Exception if a resource access error occurs
   */
  public ClassStreamTicket getTicket (String name)
    throws Exception {

    String classFileName;

    classFileName = name.replace('.', '/') + ".class";

    for (String pathComponent : pathComponents) {

      InputStream classStream;

      if (pathComponent.endsWith(".jar")) {
        if ((classStream = findJarStream(pathComponent, classFileName)) != null) {
          return new ClassStreamTicket(classStream, ClassGate.STATIC_CLASS);
        }
      } else {

        Path classFile;
        long timeStamp;

        if ((classFile = findPath(pathComponent, classFileName)) != null) {
          synchronized (filePathMap) {
            filePathMap.put(name, classFile.toAbsolutePath().normalize());
            timeStamp = Files.getLastModifiedTime(classFile).toMillis();
            return new ClassStreamTicket(Files.newInputStream(classFile), timeStamp);
          }
        }
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  public URL getResource (String path)
    throws IOException {

    for (String pathComponent : pathComponents) {

      JarLocator jarLocator;

      if (pathComponent.endsWith(".jar")) {
        if ((jarLocator = findJarLocator(pathComponent, path)) != null) {

          return URI.create("jar:file://" + rectifyPath(pathComponent) + "!/" + jarLocator.getJarEntry().getName()).toURL();
        }
      } else {

        Path resourcePath;

        if ((resourcePath = findPath(pathComponent, path)) != null) {
          return URI.create("file://" + rectifyPath(PathUtility.asNormalizedString(resourcePath))).toURL();
        }
      }
    }

    return null;
  }

  /**
   * Normalizes a file path for use in URI construction, ensuring forward slashes and a leading slash.
   *
   * @param path the path to normalize
   * @return a URI-safe path string
   */
  private String rectifyPath (String path) {

    String rectifiedPath = path.replace('\\', '/');

    return (rectifiedPath.charAt(0) == '/') ? rectifiedPath : '/' + rectifiedPath;
  }

  /**
   * {@inheritDoc}
   */
  public InputStream getResourceAsStream (String path)
    throws IOException {

    for (String pathComponent : pathComponents) {

      InputStream resourceStream;

      if (pathComponent.endsWith(".jar")) {
        if ((resourceStream = findJarStream(pathComponent, path)) != null) {
          return resourceStream;
        }
      } else {

        Path resourceFile;

        if ((resourceFile = findPath(pathComponent, path)) != null) {
          return Files.newInputStream(resourceFile, StandardOpenOption.READ);
        }
      }
    }

    return null;
  }

  /**
   * Attempts to locate a resource within a JAR file and returns an input stream if found.
   *
   * @param jarComponentPath the JAR file path
   * @param path             the entry path to locate
   * @return a buffered input stream to the entry, or {@code null} if not present
   * @throws IOException if the JAR cannot be read
   */
  private InputStream findJarStream (String jarComponentPath, String path)
    throws IOException {

    JarLocator jarLocator;

    if ((jarLocator = findJarLocator(jarComponentPath, path)) != null) {
      return new BufferedInputStream(jarLocator.getInputStream());
    }

    return null;
  }

  /**
   * Searches a JAR for a matching entry.
   *
   * @param jarComponentPath the JAR file path
   * @param path             the entry name to locate
   * @return a locator containing the JAR and entry, or {@code null} if not found
   * @throws IOException if the JAR cannot be read
   */
  private JarLocator findJarLocator (String jarComponentPath, String path)
    throws IOException {

    JarFile jarFile;
    JarEntry jarEntry;
    Enumeration<JarEntry> entryEnumeration;

    jarFile = new JarFile(jarComponentPath);
    entryEnumeration = jarFile.entries();
    while (entryEnumeration.hasMoreElements()) {
      if ((jarEntry = entryEnumeration.nextElement()).getName().equals((path.charAt(0) == '/') ? path.substring(1) : path)) {
        return new JarLocator(jarFile, jarEntry);
      }
    }

    return null;
  }

  /**
   * Resolves a file within a class path directory.
   *
   * @param fileComponentPath the directory in the class path
   * @param path              the relative resource path
   * @return the resolved {@link Path} if it exists as a file, otherwise {@code null}
   */
  private Path findPath (String fileComponentPath, String path) {

    Path completePath;

    completePath = Paths.get(fileComponentPath, (path.charAt(0) == '/') ? path.substring(1) : path);
    if (Files.isRegularFile(completePath)) {

      return completePath;
    }

    return null;
  }

  /**
   * Lightweight holder for a JAR and one of its entries, providing access to the entry stream.
   */
  private static class JarLocator {

    private final JarFile jarFile;
    private final JarEntry jarEntry;

    /**
     * Creates a locator for the provided JAR and entry.
     *
     * @param jarFile  the containing JAR
     * @param jarEntry the entry within the JAR
     */
    private JarLocator (JarFile jarFile, JarEntry jarEntry) {

      this.jarFile = jarFile;
      this.jarEntry = jarEntry;
    }

    /**
     * Returns the resolved JAR entry.
     *
     * @return the entry found in the JAR
     */
    private JarEntry getJarEntry () {

      return jarEntry;
    }

    /**
     * Opens an input stream for the held entry.
     *
     * @return an input stream to the entry bytes
     * @throws IOException if the entry cannot be read
     */
    public InputStream getInputStream ()
      throws IOException {

      return jarFile.getInputStream(jarFile.getEntry(jarEntry.getName()));
    }
  }
}
