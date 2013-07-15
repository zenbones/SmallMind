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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class GatingClassLoader extends ClassLoader {

  private final HashMap<String, ClassGateTicket> ticketMap;

  private ClassGate[] classGates;
  private int reloadInterval;

  public GatingClassLoader (int reloadInterval, ClassGate... classGates) {

    this(null, reloadInterval, classGates);
  }

  public GatingClassLoader (ClassLoader parent, int reloadInterval, ClassGate... classGates) {

    super(parent);

    this.classGates = classGates;
    this.reloadInterval = reloadInterval;

    ticketMap = new HashMap<String, ClassGateTicket>();
  }

  public ClassGate[] getClassGates () {

    return classGates;
  }

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

          if (reloadInterval >= 0) {
            synchronized (ticketMap) {
              ticketMap.put(name, new ClassGateTicket(classGate, classStreamTicket.getTimeStamp()));
            }
          }

          return definedClass;
        }
      }
      catch (Exception exception) {
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

  public Class loadClass (String name)
    throws ClassNotFoundException {

    return loadClass(name, false);
  }

  public synchronized Class loadClass (String name, boolean resolve)
    throws ClassNotFoundException {

    Class gatedClass;
    ClassGateTicket classGateTicket;

    if ((gatedClass = findLoadedClass(name)) != null) {
      if (reloadInterval >= 0) {
        synchronized (ticketMap) {
          classGateTicket = ticketMap.get(name);
        }

        if (classGateTicket.getTimeStamp() != ClassGate.STATIC_CLASS) {
          if (System.currentTimeMillis() >= (classGateTicket.getTimeStamp() + (reloadInterval * 1000))) {
            if (classGateTicket.getClassGate().getLastModDate(name) > classGateTicket.getTimeStamp()) {
              throw new StaleClassLoaderException(name);
            }
          }
        }
      }
    }

    if (gatedClass == null) {
      try {
        gatedClass = findClass(name);
      }
      catch (ClassNotFoundException c) {
      }
    }

    if (getParent() != null) {
      try {
        gatedClass = getParent().loadClass(name);
      }
      catch (ClassNotFoundException c) {
      }
    }

    if (gatedClass == null) {
      gatedClass = findSystemClass(name);
    }

    if (resolve) {
      resolveClass(gatedClass);
    }

    return gatedClass;
  }

  @Override
  public URL getResource (String name) {

    URL resourceURL;

    for (ClassGate classGate : classGates) {
      try {
        if ((resourceURL = classGate.getResource(name)) != null) {
          return resourceURL;
        }
      }
      catch (Exception exception) {
      }
    }

    return super.getResource(name);
  }

  public InputStream getResourceAsStream (String name) {

    InputStream resourceStream;

    for (ClassGate classGate : classGates) {
      try {
        if ((resourceStream = classGate.getResourceAsStream(name)) != null) {
          return resourceStream;
        }
      }
      catch (Exception exception) {
      }
    }

    return super.getResourceAsStream(name);
  }
}
