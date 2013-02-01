/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathClassGate implements ClassGate {

  private final HashMap<String, String> filePathMap;

  private String[] pathComponents;

  public ClasspathClassGate () {

    this(System.getProperty("java.class.path"));
  }

  public ClasspathClassGate (String classPath) {

    this(classPath.split(System.getProperty("path.separator"), -1));
  }

  public ClasspathClassGate (String... pathComponents) {

    this.pathComponents = pathComponents;

    filePathMap = new HashMap<String, String>();
  }

  public long getLastModDate (String name) {

    File classFile;
    String filePath;

    synchronized (filePathMap) {
      if ((filePath = filePathMap.get(name)) != null) {
        classFile = new File(filePath);

        return classFile.lastModified();
      }
    }

    return ClassGate.STATIC_CLASS;
  }

  public ClassStreamTicket getClassAsTicket (String name)
    throws Exception {

    String classFileName;

    classFileName = name.replace('.', '/') + ".class";

    for (String pathComponent : pathComponents) {

      InputStream classStream;

      if (pathComponent.endsWith(".jar")) {
        if ((classStream = findJarStream(pathComponent, classFileName)) != null) {
          return new ClassStreamTicket(classStream, ClassGate.STATIC_CLASS);
        }
      }
      else {

        File classFile;
        long timeStamp;

        if ((classFile = findFile(pathComponent, classFileName)) != null) {
          synchronized (filePathMap) {
            filePathMap.put(name, classFile.getAbsolutePath());
            timeStamp = classFile.lastModified();
            return new ClassStreamTicket(new BufferedInputStream(new FileInputStream(classFile)), timeStamp);
          }
        }
      }
    }

    return null;
  }

  public URL getResource (String path) throws Exception {

    for (String pathComponent : pathComponents) {

      JarLocator jarLocator;

      if (pathComponent.endsWith(".jar")) {
        if ((jarLocator = findJarLocator(pathComponent, path)) != null) {

          return new URL("jar:file://" + rectifyPath(pathComponent) + "!/" + jarLocator.getJarEntry().getName());
        }
      }
      else {

        File resourceFile;

        if ((resourceFile = findFile(pathComponent, path)) != null) {
          return new URL("file://" + rectifyPath(resourceFile.getAbsolutePath()));
        }
      }
    }

    return null;
  }

  private String rectifyPath (String path) {

    String rectifiedPath = path.replace('\\', '/');

    return (rectifiedPath.charAt(0) == '/') ? rectifiedPath : '/' + rectifiedPath;
  }

  public InputStream getResourceAsStream (String path)
    throws Exception {

    for (String pathComponent : pathComponents) {

      InputStream resourceStream;

      if (pathComponent.endsWith(".jar")) {
        if ((resourceStream = findJarStream(pathComponent, path)) != null) {
          return resourceStream;
        }
      }
      else {

        File resourceFile;

        if ((resourceFile = findFile(pathComponent, path)) != null) {
          return new BufferedInputStream(new FileInputStream(resourceFile));
        }
      }
    }

    return null;
  }

  private InputStream findJarStream (String jarComponentPath, String path)
    throws IOException {

    JarLocator jarLocator;

    if ((jarLocator = findJarLocator(jarComponentPath, path)) != null) {
      return new BufferedInputStream(jarLocator.getInputStream());
    }

    return null;
  }

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

  private File findFile (String fileComponentPath, String path) {

    File pathFile;

    pathFile = new File((path.charAt(0) == '/') ? fileComponentPath + path : fileComponentPath + '/' + path);
    if (pathFile.isFile()) {

      return pathFile;
    }

    return null;
  }

  private class JarLocator {

    private JarFile jarFile;
    private JarEntry jarEntry;

    private JarLocator (JarFile jarFile, JarEntry jarEntry) {

      this.jarFile = jarFile;
      this.jarEntry = jarEntry;
    }

    public JarEntry getJarEntry () {

      return jarEntry;
    }

    public InputStream getInputStream ()
      throws IOException {

      return jarFile.getInputStream(jarFile.getEntry(jarEntry.getName()));
    }
  }
}
