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
 * A {@link SecureClassLoader} that loads classes through one or more {@link ClassGate}s and optionally detects stale class definitions by comparing last-modified timestamps against a configurable grace period.
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
   * Creates a loader with no explicit parent that searches the supplied gates and checks for staleness after the given grace period.
   *
   * @param gracePeriodSeconds the number of seconds a loaded class is considered fresh before its source is checked; a negative value disables staleness checking
   * @param classGates         the ordered set of gates used to locate classes and resources
   */
  public GatingClassLoader (int gracePeriodSeconds, ClassGate... classGates) {

    this(null, gracePeriodSeconds, classGates);
  }

  /**
   * Creates a loader with the given parent that searches the supplied gates and checks for staleness after the given grace period.
   *
   * @param parent             the parent class loader, or {@code null} to fall back to the bootstrap and system loaders
   * @param gracePeriodSeconds the number of seconds a loaded class is considered fresh before its source is checked; a negative value disables staleness checking
   * @param classGates         the ordered set of gates used to locate classes and resources
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
   * Returns the array of {@link ClassGate}s this loader consults when resolving classes and resources.
   *
   * @return the configured class gates in search order
   */
  public ClassGate[] getClassGates () {

    return classGates;
  }

  /**
   * Delegates to the superclass permission lookup; subclasses may override to apply custom security policies.
   *
   * @param codesource the code source for which permissions are requested
   * @return the permission collection granted to the code source
   */
  @Override
  protected PermissionCollection getPermissions (CodeSource codesource) {

    return super.getPermissions(codesource);
  }

  /**
   * Loads the class with the specified name, delegating first to the parent or system loader and then to the configured gates.
   * If staleness detection is enabled and the grace period has elapsed for an already-loaded class, the source is checked and
   * {@link StaleClassLoaderException} is thrown when a newer version is found.
   *
   * @param name    the fully qualified name of the class to load
   * @param resolve whether to resolve the class after loading
   * @return the loaded {@link Class} object
   * @throws ClassNotFoundException if the class cannot be found by any loader or gate
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
   * Searches the configured gates in order to locate, read, and define the named class.
   * Records a {@link ClassGateTicket} in the internal map when staleness detection is enabled.
   *
   * @param name the fully qualified binary name of the class to define
   * @return the defined {@link Class} object
   * @throws ClassNotFoundException if no gate can supply the class bytes
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
   * Defines the package for the given class if it has not already been defined, populating it with JVM specification and vendor metadata.
   *
   * @param name the fully qualified binary class name from which the package name is derived
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
   * Reads all bytes from the given stream into a byte array suitable for passing to {@link ClassLoader#defineClass}.
   *
   * @param classInputStream the stream supplying the raw class bytes
   * @return a byte array containing the entire contents of the stream
   * @throws IOException if an I/O error occurs while reading
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
   * Returns a stream for the named resource, consulting the configured gates after the parent loader if the resource is not found via the standard delegation chain.
   *
   * @param name the resource name to locate
   * @return an open {@link InputStream} to the resource, or {@code null} if not found
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
   * Searches the configured gates in order for the named resource and returns the first matching URL.
   *
   * @param name the resource name to locate
   * @return a URL to the resource, or {@code null} if no gate can supply it
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
   * Collects URLs for the named resource from all configured gates and returns them as an enumeration.
   *
   * @param name the resource name to locate across all gates
   * @return an enumeration of all URLs found for the resource; never {@code null}
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
