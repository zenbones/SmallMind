/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.io.File;
import org.smallmind.nutsnbolts.lang.ClasspathClassGate;
import org.smallmind.nutsnbolts.lang.GatingClassLoader;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class ExtensionLoader<E extends ExtensionInstance> {

  private E extensionInstance;
  private GatingClassLoader classLoader;

  public ExtensionLoader (Class<E> extensionInstanceClass, String springFileName)
    throws ExtensionLoaderException {

    File extensionFile = new File(System.getProperty("user.dir") + "/" + springFileName);

    if (extensionFile.exists()) {

      FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(extensionFile.getAbsolutePath());

      try {
        extensionInstance = applicationContext.getBean(extensionInstanceClass);
      } catch (Throwable throwable) {
        throw new ExtensionLoaderException(throwable, "Unable to execute extension configuration(%s)", extensionFile.getAbsolutePath());
      }

      if ((extensionInstance.getClasspathComponents() != null) && (extensionInstance.getClasspathComponents().length > 0)) {

        File classpathComponentFile;
        String[] normalizedPathComponents = new String[extensionInstance.getClasspathComponents().length];
        int componentIndex = 0;

        for (String classpathComponent : extensionInstance.getClasspathComponents()) {
          classpathComponentFile = new File(classpathComponent);
          normalizedPathComponents[componentIndex++] = classpathComponentFile.isAbsolute() ? classpathComponent : System.getProperty("user.dir") + '/' + classpathComponent;
        }

        Thread.currentThread().setContextClassLoader(classLoader = new GatingClassLoader(Thread.currentThread().getContextClassLoader(), -1, new ClasspathClassGate(normalizedPathComponents)));
      }
    }
  }

  protected E getExtensionInstance () {

    return extensionInstance;
  }

  public GatingClassLoader getClassLoader () {

    return classLoader;
  }
}