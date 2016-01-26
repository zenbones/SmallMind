/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.IteratorEnumeration;

public class GatingClassLoader extends ClassLoader {

  private final HashMap<String, ClassGateTicket> ticketMap;
  private final HashSet<String> packageSet = new HashSet<>();
  private final ClassGate[] classGates;
  private final int gracePeriodSeconds;

  static {

    ClassLoader.registerAsParallelCapable();
  }

  public GatingClassLoader (int gracePeriodSeconds, ClassGate... classGates) {

    this(null, gracePeriodSeconds, classGates);
  }

  public GatingClassLoader (ClassLoader parent, int gracePeriodSeconds, ClassGate... classGates) {

    super(parent);

    this.classGates = classGates;
    this.gracePeriodSeconds = gracePeriodSeconds;

    ticketMap = new HashMap<>();
  }

  public ClassGate[] getClassGates () {

    return classGates;
  }

  @Override
  public synchronized Class loadClass (String name, boolean resolve)
    throws ClassNotFoundException {

    Class gatedClass;
    ClassGateTicket classGateTicket;

    if ((gatedClass = findLoadedClass(name)) != null) {
      if (gracePeriodSeconds >= 0) {
        synchronized (ticketMap) {
          classGateTicket = ticketMap.get(name);
        }

        if (classGateTicket.getTimeStamp() != ClassGate.STATIC_CLASS) {
          if (System.currentTimeMillis() >= (classGateTicket.getTimeStamp() + (gracePeriodSeconds * 1000))) {
            if (classGateTicket.getClassGate().getLastModDate(name) > classGateTicket.getTimeStamp()) {
              throw new StaleClassLoaderException(name);
            }
          }
        }
      }
    } else {
      if (getParent() != null) {
        try {
          gatedClass = getParent().loadClass(name);
        } catch (ClassNotFoundException c) {
          gatedClass = findClass(name);
        }
      } else {
        try {
          gatedClass = findSystemClass(name);
        } catch (ClassNotFoundException c) {
          gatedClass = findClass(name);
        }
      }
    }

    if (resolve) {
      resolveClass(gatedClass);
    }

    return gatedClass;
  }

  @Override
  public synchronized Class findClass (String name)
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

          definePackage(name);

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

  private void definePackage (String name) {

    String packageName;
    int lastDotPos = name.lastIndexOf('.');

    packageName = name.substring(0, lastDotPos);
    if (packageSet.add(packageName)) {
      definePackage(packageName, System.getProperty("java.vm.specification.name"), System.getProperty("java.vm.specification.version"), System.getProperty("java.vm.specification.vendor"), System.getProperty("java.specification.name"), System.getProperty("java.specification.version"), System.getProperty("java.specification.vendor"), null);
    }
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
  public InputStream getResourceAsStream (String name) {

    InputStream resourceStream;

    if ((resourceStream = super.getResourceAsStream(name)) != null) {

      return resourceStream;
    }

    for (ClassGate classGate : classGates) {
      try {
        if ((resourceStream = classGate.getResourceAsStream(name)) != null) {

          return resourceStream;
        }
      } catch (Exception exception) {
      }
    }

    return null;
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
