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

import java.net.MalformedURLException;
import java.net.URI;
import java.security.CodeSource;
import java.security.cert.Certificate;

/**
 * {@link ClasspathClassGate} that associates a {@link CodeSource} spanning the provided classpath
 * entries for use in secure class loading.
 */
public class SecureClasspathClassGate extends ClasspathClassGate {

  private final CodeSource codeSource;

  /**
   * Builds a gate using the JVM class path and a corresponding code source.
   *
   * @throws MalformedURLException if the generated code source URL is invalid
   */
  public SecureClasspathClassGate ()
    throws MalformedURLException {

    this(System.getProperty("java.class.path"));
  }

  /**
   * Builds a gate from a class path string using the platform path separator.
   *
   * @param classPath the class path to secure
   * @throws MalformedURLException if the generated code source URL is invalid
   */
  public SecureClasspathClassGate (String classPath)
    throws MalformedURLException {

    this(classPath.split(System.getProperty("path.separator"), -1));
  }

  /**
   * Builds a gate from explicit path components and prepares a code source that references them.
   *
   * @param pathComponents the class path components
   * @throws MalformedURLException if the generated code source URL is invalid
   */
  public SecureClasspathClassGate (String... pathComponents)
    throws MalformedURLException {

    super(pathComponents);

    StringBuilder urlSpecBuilder = new StringBuilder("file://");

    for (String pathComponent : pathComponents) {
      if (pathComponent.charAt(0) != '/') {
        urlSpecBuilder.append('/');
      }
      urlSpecBuilder.append(pathComponent);
    }
    urlSpecBuilder.append("/-");

    codeSource = new CodeSource(URI.create(urlSpecBuilder.toString()).toURL(), (Certificate[])null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CodeSource getCodeSource () {

    return codeSource;
  }
}
