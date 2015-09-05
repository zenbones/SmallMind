/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class SingularityClassLoader extends ClassLoader {

  private final HashMap<String, URL> urlMap = new HashMap<>();

  static {

    ClassLoader.registerAsParallelCapable();
    URL.setURLStreamHandlerFactory(new SingularityJarURLStreamHandlerFactory());
  }

  public SingularityClassLoader (ClassLoader parent, URL jarURL, JarInputStream jarInputStream)
    throws IOException {

    super(parent);

    JarEntry jarEntry;

    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
      if (!jarEntry.isDirectory()) {
        if (!jarEntry.getName().startsWith("META-INF/")) {
          urlMap.put(jarEntry.getName(), new URL("jar", "localhost", jarURL.toExternalForm() + "!/" + jarEntry.getName()));
        } else if (jarEntry.getName().startsWith("META-INF/singularity/") && jarEntry.getName().endsWith(".jar")) {
          try (JarInputStream innerJarInputStream = new JarInputStream(new CloseIgnoringInputStream(jarInputStream))) {

            JarEntry innerJarEntry;

            while ((innerJarEntry = innerJarInputStream.getNextJarEntry()) != null) {
              if (!(innerJarEntry.isDirectory() || innerJarEntry.getName().startsWith("META-INF/"))) {
                urlMap.put(innerJarEntry.getName(), new URL("singularity", "localhost", jarURL.toExternalForm() + "!!/" + jarEntry.getName() + "!/" + innerJarEntry.getName()));
              }
            }
          }
        }
      }
    }
  }

  @Override
  public synchronized Class loadClass (String name, boolean resolve)
    throws ClassNotFoundException {

    Class singularityClass;

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

  @Override
  public synchronized Class findClass (String name)
    throws ClassNotFoundException {

    URL classURL;

    if ((classURL = urlMap.get(name.replace('.', '/') + ".class")) != null) {
      try {

        InputStream classInputStream;
        byte[] classData;

        classInputStream = classURL.openStream();
        classData = getClassData(classInputStream);
        classInputStream.close();

        return defineClass(name, classData, 0, classData.length);
      } catch (Exception exception) {
        throw new ClassNotFoundException("Exception encountered while attempting to define class (" + name + ")", exception);
      }
    }

    throw new ClassNotFoundException(name);
  }

  private byte[] getClassData (InputStream classInputStream)
    throws IOException {

    ByteArrayOutputStream classDataOutputStream = new ByteArrayOutputStream();
    int singleByte;

    while ((singleByte = classInputStream.read()) >= 0) {
      classDataOutputStream.write(singleByte);
    }

    return classDataOutputStream.toByteArray();
  }

  @Override
  public URL findResource (String name) {

    return urlMap.get(name);
  }

  @Override
  protected Enumeration<URL> findResources (String name) {

    return null;
  }
}



