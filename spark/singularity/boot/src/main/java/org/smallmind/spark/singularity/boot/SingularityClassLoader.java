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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class SingularityClassLoader extends ClassLoader {

  private final HashMap<String, URL> urlMap = new HashMap<>();

  static {

    URL.setURLStreamHandlerFactory(new SingularityJarURLStreamHandlerFactory());
    ClassLoader.registerAsParallelCapable();
  }

  public SingularityClassLoader (ClassLoader parent, JarInputStream jarInputStream)
    throws IOException {

    super(parent);

    JarEntry jarEntry;

    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
      if (jarEntry.getName().startsWith("META-INF/singularity/") && jarEntry.getName().endsWith(".jar")) {
        try (JarInputStream innerJarInputStream = new JarInputStream(new JarJarInputStream(jarInputStream))) {

          JarEntry innerJarEntry;

          while ((innerJarEntry = innerJarInputStream.getNextJarEntry()) != null) {
            if (!innerJarEntry.getName().startsWith("META-INF/")) {
              urlMap.put(innerJarEntry.getName(), new URL("singularity", "localhost", jarEntry.getName() + "!" + innerJarEntry.getName()));
            }
          }
        }
      }
    }
  }

  @Override
  public Class findClass (String name)
    throws ClassNotFoundException {

    Class definedClass;
    ClassStreamTicket classStreamTicket;
    InputStream classInputStream;
    byte[] classData;

    for (ClassGate classGate : classGates) {
      try {
        if ((classStreamTicket = classGate.getClassAsTicket(name)) != null) {
          classInputStream = classStreamTicket.getInputStream();
          classData = getClassData(classInputStream);
          classInputStream.close();

          definedClass = defineClass(name, classData, 0, classData.length);

          if (gracePeriodSeconds >= 0) {
            synchronized (ticketMap) {
              ticketMap.put(name, new ClassGateTicket(classGate, classStreamTicket.getTimeStamp()));
            }
          }

          return definedClass;
        }
      } catch (Exception exception) {
        throw new ClassNotFoundException("Exception encountered while attempting to define class (" + name + ")", exception);
      }
    }

    throw new ClassNotFoundException(name);
  }

  private byte[] getClassData (InputStream classInputStream)
    throws IOException {

    byte[] classData;
    int dataLength;
    int totalBytesRead = 0;
    int bytesRead;

    dataLength = classInputStream.available();
    classData = new byte[dataLength];
    while (totalBytesRead < dataLength) {
      bytesRead = classInputStream.read(classData, totalBytesRead, dataLength - totalBytesRead);
      totalBytesRead += bytesRead;
    }
    return classData;
  }

  @Override
  public URL findResource (String name) {

    URL resourceURL;

    for (ClassGate classGate : classGates) {
      try {
        if ((resourceURL = classGate.getResource(name)) != null) {

          return resourceURL;
        }
      } catch (Exception exception) {
      }
    }

    return null;
  }

  @Override
  protected Enumeration<URL> findResources (String name) {

    LinkedList<URL> urlList = new LinkedList<>();

    for (ClassGate classGate : classGates) {

      URL resourceURL;

      try {
        if ((resourceURL = classGate.getResource(name)) != null) {
          urlList.add(resourceURL);
        }
      } catch (Exception exception) {
      }
    }

    return new IteratorEnumeration<>(urlList.iterator());
  }
}



