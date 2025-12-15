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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.IteratorEnumeration;

/*
    If you want security...

    new GatingClassLoader(<parent>, <grace period seconds>, new SecureClasspathClassGate(<path copmponents>)) {

        @Override
        protected PermissionCollection getPermissions (CodeSource codesource) {

          Permissions permissions = new Permissions();

          permissions.add(new SandboxPermission());
          ...
          add all other permissions
          ...

          return permissions;
        }
      }
*/

/**
 * {@link SecureClassLoader} that loads classes from one or more {@link ClassGate}s and can detect
 * stale classes based on last-modified timestamps. A grace period governs how long a loaded class
 * remains valid before its source is checked for updates, throwing {@link StaleClassLoaderException}
 * when newer bytes are detected.
 */
public class GatingClassLoader extends SecureClassLoader {

  private final HashMap<String, ClassGateTicket> ticketMap;
  private final HashSet<String> packageSet = new HashSet<>();
  private final ClassGate[] classGates;
  private final int gracePeriodSeconds;

  static {

    ClassLoader.registerAsParallelCapable();
  }

  /**
   * Constructs a loader with no explicit parent using the supplied grace period and class gates.
   *
   * @param gracePeriodSeconds number of seconds before checking for class staleness; negative to disable checks
   * @param classGates         the gates used to resolve classes and resources
   */
  public GatingClassLoader (int gracePeriodSeconds, ClassGate... classGates) {

    this(null, gracePeriodSeconds, classGates);
  }

  /**
   * Constructs a loader with the given parent, grace period, and class gates.
   *
   * @param parent             the parent class loader, or {@code null} to use the bootstrap/system loaders
   * @param gracePeriodSeconds number of seconds before checking for class staleness; negative to disable checks
   * @param classGates         the gates used to resolve classes and resources
   */
  public GatingClassLoader (ClassLoader parent, int gracePeriodSeconds, ClassGate... classGates) {

    super(parent);

    SecurityManager security = System.getSecurityManager();

    if (security != null) {
      security.checkCreateClassLoader();
    }

    this.classGates = classGates;
    this.gracePeriodSeconds = gracePeriodSeconds;

    ticketMap = new HashMap<>();
  }

  /**
   * Returns the set of gates this loader consults.
   *
   * @return the configured class gates
   */
  public ClassGate[] getClassGates () {

    return classGates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected PermissionCollection getPermissions (CodeSource codesource) {

    return super.getPermissions(codesource);
  }

  /**
   * {@inheritDoc}
   * <p>
   * After loading a class, optionally checks for staleness once the configured grace period has elapsed.
   */
  @Override
  public synchronized Class<?> loadClass (String name, boolean resolve)
    throws ClassNotFoundException {

    Class<?> gatedClass;
    ClassGateTicket classGateTicket;

    if ((gatedClass = findLoadedClass(name)) != null) {
      if (gracePeriodSeconds >= 0) {
        synchronized (ticketMap) {
          classGateTicket = ticketMap.get(name);
        }

        if (classGateTicket.getTimeStamp() != ClassGate.STATIC_CLASS) {
          if (System.currentTimeMillis() >= (classGateTicket.getTimeStamp() + (gracePeriodSeconds * 1000L))) {

            long lastModTime;

            try {
              lastModTime = classGateTicket.getClassGate().getLastModDate(name);
            } catch (Exception exception) {
              throw new RuntimeException(exception);
            }

            if (lastModTime > classGateTicket.getTimeStamp()) {
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

  /**
   * {@inheritDoc}
   * <p>
   * Iterates through configured gates to locate and define the requested class. Updates the ticket map
   * when staleness detection is enabled.
   */
  @Override
  public synchronized Class<?> findClass (String name)
    throws ClassNotFoundException {

    for (ClassGate classGate : classGates) {
      try {

        ClassStreamTicket classStreamTicket;

        if ((classStreamTicket = classGate.getTicket(name)) != null) {

          CodeSource codeSource;
          Class<?> definedClass;
          byte[] classData;

          try (InputStream classInputStream = classStreamTicket.getInputStream()) {
            classData = getClassData(classInputStream);
          }

          definePackage(name);
          definedClass = ((codeSource = classGate.getCodeSource()) != null) ? defineClass(name, classData, 0, classData.length, codeSource) : defineClass(name, classData, 0, classData.length);

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

  /**
   * Defines a package for the supplied class name if it has not been defined already.
   *
   * @param name the fully qualified class name
   */
  private void definePackage (String name) {

    String packageName;
    int lastDotPos = name.lastIndexOf('.');

    packageName = name.substring(0, lastDotPos);
    if (packageSet.add(packageName)) {
      definePackage(packageName, System.getProperty("java.vm.specification.name"), System.getProperty("java.vm.specification.version"), System.getProperty("java.vm.specification.vendor"), System.getProperty("java.specification.name"), System.getProperty("java.specification.version"), System.getProperty("java.specification.vendor"), null);
    }
  }

  /**
   * Reads class bytes from an input stream into a byte array.
   *
   * @param classInputStream the stream containing class data
   * @return the class bytes
   * @throws IOException if reading fails
   */
  private byte[] getClassData (InputStream classInputStream)
    throws IOException {

    try (ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream()) {

      int bytesRead;
      byte[] buffer = new byte[8128];

      while ((bytesRead = classInputStream.read(buffer)) >= 0) {
        dataOutputStream.write(buffer, 0, bytesRead);
      }

      return dataOutputStream.toByteArray();
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Falls back to configured gates when the parent loader cannot supply the resource.
   */
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
      } catch (IOException ioException) {
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Attempts to locate a resource via the configured gates when not found by the parent loader.
   */
  @Override
  public URL findResource (String name) {

    URL resourceURL;

    for (ClassGate classGate : classGates) {
      try {
        if ((resourceURL = classGate.getResource(name)) != null) {

          return resourceURL;
        }
      } catch (IOException ioException) {
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Aggregates resource URLs from all configured gates.
   */
  @Override
  protected Enumeration<URL> findResources (String name) {

    LinkedList<URL> urlList = new LinkedList<>();

    for (ClassGate classGate : classGates) {

      URL resourceURL;

      try {
        if ((resourceURL = classGate.getResource(name)) != null) {
          urlList.add(resourceURL);
        }
      } catch (IOException ioException) {
      }
    }

    return new IteratorEnumeration<>(urlList.iterator());
  }
}
