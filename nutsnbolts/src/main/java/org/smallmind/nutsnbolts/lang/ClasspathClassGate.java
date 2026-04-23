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
 * {@link ClassGate} implementation that resolves classes and resources from an ordered list of classpath components, which may be directories or JAR files.
 */
public class ClasspathClassGate implements ClassGate {

  private final HashMap<String, Path> filePathMap;

  private final String[] pathComponents;

  /**
   * Creates a gate using the JVM class path derived from the {@code java.class.path} system property.
   */
  public ClasspathClassGate () {

    this(System.getProperty("java.class.path"));
  }

  /**
   * Creates a gate by splitting the supplied class path string on the platform path separator character.
   *
   * @param classPath the class path string containing directories and/or JAR file paths
   */
  public ClasspathClassGate (String classPath) {

    this(classPath.split(System.getProperty("path.separator"), -1));
  }

  /**
   * Creates a gate that searches the explicitly supplied path components in order.
   *
   * @param pathComponents the directories and/or JAR file paths to search
   */
  public ClasspathClassGate (String... pathComponents) {

    this.pathComponents = pathComponents;

    filePathMap = new HashMap<>();
  }

  /**
   * Returns the last-modified time of a previously located file-backed class.
   *
   * @param name the fully qualified class name previously returned by {@link #getTicket(String)}
   * @return the last-modified time in milliseconds, or {@link ClassGate#STATIC_CLASS} if the path is not recorded
   * @throws IOException if the file metadata cannot be read
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
   * Returns {@code null} because this gate does not associate classes with a specific code source.
   *
   * @return {@code null}
   */
  @Override
  public CodeSource getCodeSource () {

    return null;
  }

  /**
   * Searches the configured path components for the named class and returns a stream ticket providing access to its bytes.
   *
   * @param name the fully qualified binary class name to locate
   * @return a {@link ClassStreamTicket} wrapping the class byte stream and its timestamp, or {@code null} if not found
   * @throws Exception if an error occurs while locating or opening the class
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
   * Searches the configured path components for the named resource and returns a URL to it.
   *
   * @param path the resource path to locate
   * @return a URL pointing to the resource, or {@code null} if not found
   * @throws IOException if an I/O error occurs while searching
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
   * Normalizes a file system path to a URI-safe form by replacing backslashes with forward slashes and prepending a leading slash if absent.
   *
   * @param path the raw file path to normalize
   * @return a URI-safe path string beginning with {@code /}
   */
  private String rectifyPath (String path) {

    String rectifiedPath = path.replace('\\', '/');

    return (rectifiedPath.charAt(0) == '/') ? rectifiedPath : '/' + rectifiedPath;
  }

  /**
   * Searches the configured path components for the named resource and returns an open stream to it.
   *
   * @param path the resource path to locate
   * @return an open {@link InputStream} for the resource, or {@code null} if not found
   * @throws IOException if an I/O error occurs while opening the stream
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
   * Searches the specified JAR file for the given entry path and returns a buffered stream to it.
   *
   * @param jarComponentPath the file system path of the JAR to search
   * @param path             the entry name to locate within the JAR
   * @return a buffered {@link InputStream} to the entry, or {@code null} if not found
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
   * Searches the specified JAR file for an entry matching the given path and returns a locator wrapping both.
   *
   * @param jarComponentPath the file system path of the JAR to search
   * @param path             the entry name to locate within the JAR
   * @return a {@code JarLocator} holding the matched JAR and entry, or {@code null} if not found
   * @throws IOException if the JAR file cannot be opened or read
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
   * Resolves the given relative resource path under the specified directory and returns it if it is a regular file.
   *
   * @param fileComponentPath a directory entry in the classpath to search under
   * @param path              the relative resource path to resolve
   * @return the resolved {@link Path} if it exists as a regular file, or {@code null} otherwise
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
   * Holds a reference to a {@link JarFile} and a matching {@link JarEntry} found within it, providing stream access to the entry's bytes.
   */
  private static class JarLocator {

    private final JarFile jarFile;
    private final JarEntry jarEntry;

    /**
     * Creates a locator pairing the given JAR file with one of its entries.
     *
     * @param jarFile  the JAR file that contains the entry
     * @param jarEntry the located entry within the JAR
     */
    private JarLocator (JarFile jarFile, JarEntry jarEntry) {

      this.jarFile = jarFile;
      this.jarEntry = jarEntry;
    }

    /**
     * Returns the JAR entry held by this locator.
     *
     * @return the matched {@link JarEntry}
     */
    private JarEntry getJarEntry () {

      return jarEntry;
    }

    /**
     * Opens and returns an input stream for the held JAR entry.
     *
     * @return an {@link InputStream} to the entry's bytes
     * @throws IOException if the entry cannot be opened
     */
    public InputStream getInputStream ()
      throws IOException {

      return jarFile.getInputStream(jarFile.getEntry(jarEntry.getName()));
    }
  }
}
