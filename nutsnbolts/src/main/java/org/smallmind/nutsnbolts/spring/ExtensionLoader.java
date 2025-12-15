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
package org.smallmind.nutsnbolts.spring;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.smallmind.nutsnbolts.io.PathUtility;
import org.smallmind.nutsnbolts.lang.ClasspathClassGate;
import org.smallmind.nutsnbolts.lang.GatingClassLoader;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Loads an extension Spring context and optionally sets a gated classloader based on classpath components declared by the extension.
 *
 * @param <E> the extension instance type
 */
public class ExtensionLoader<E extends ExtensionInstance> {

  private E extensionInstance;
  private GatingClassLoader classLoader;

  /**
   * Constructs an extension loader that reads a Spring context and initializes the extension instance.
   *
   * @param extensionInstanceClass the bean type to retrieve from the Spring context
   * @param springFileName         the path to the Spring XML configuration file
   * @throws ExtensionLoaderException if the extension cannot be constructed or initialized
   */
  public ExtensionLoader (Class<E> extensionInstanceClass, String springFileName)
    throws ExtensionLoaderException {

    Path extensionFile = Paths.get(System.getProperty("user.dir"), springFileName);

    if (Files.isRegularFile(extensionFile)) {

      FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(PathUtility.asNormalizedString(extensionFile));

      try {
        extensionInstance = applicationContext.getBean(extensionInstanceClass);
      } catch (Throwable throwable) {
        throw new ExtensionLoaderException(throwable, "Unable to execute extension configuration(%s)", extensionFile);
      }

      if ((extensionInstance.getClasspathComponents() != null) && (extensionInstance.getClasspathComponents().length > 0)) {

        Path classpathComponentFile;
        String[] normalizedPathComponents = new String[extensionInstance.getClasspathComponents().length];
        int componentIndex = 0;

        for (String classpathComponent : extensionInstance.getClasspathComponents()) {
          classpathComponentFile = Paths.get(classpathComponent);
          normalizedPathComponents[componentIndex++] = PathUtility.asNormalizedString(classpathComponentFile.isAbsolute() ? classpathComponentFile : Paths.get(System.getProperty("user.dir")).resolve(classpathComponent));
        }

        Thread.currentThread().setContextClassLoader(classLoader = new GatingClassLoader(Thread.currentThread().getContextClassLoader(), -1, new ClasspathClassGate(normalizedPathComponents)));
      }
    }
  }

  /**
   * @return the initialized extension instance, or {@code null} if no configuration was loaded
   */
  protected E getExtensionInstance () {

    return extensionInstance;
  }

  /**
   * @return the gated classloader installed for the extension, or {@code null} if none was set
   */
  public GatingClassLoader getClassLoader () {

    return classLoader;
  }
}
