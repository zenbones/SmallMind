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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.cert.Certificate;

/**
 * {@link ClasspathClassGate} that associates the classes it loads with a {@link CodeSource} naming
 * the classpath entry they were read from. As a {@link CodeSource} carries a single location, this
 * gate covers a single classpath entry, so compose a {@link GatingClassLoader} from one gate per
 * entry rather than handing a multi-entry class path to a single gate.
 */
public class SecureClasspathClassGate extends ClasspathClassGate {

  private final CodeSource codeSource;

  /**
   * Constructs a gate over a single classpath entry, which may be either a directory or a JAR file,
   * and builds the {@link CodeSource} that names it. The entry is taken whole, and so is never split
   * on the platform path separator.
   *
   * @param pathComponent the sole classpath entry this gate searches
   * @throws MalformedURLException    if the derived code source URL is malformed
   * @throws IllegalArgumentException if the entry is null or blank
   */
  public SecureClasspathClassGate (String pathComponent)
    throws MalformedURLException {

    super(new String[] {vetted(pathComponent)});

    codeSource = new CodeSource(asCodeSourceURI(pathComponent).toURL(), (Certificate[])null);
  }

  /**
   * Returns the given classpath entry once confirmed usable, so that no entry the {@link CodeSource}
   * cannot name is ever passed to the superclass as somewhere to search.
   *
   * @param pathComponent the classpath entry to check
   * @return the checked classpath entry
   * @throws IllegalArgumentException if the entry is null or blank
   */
  private static String vetted (String pathComponent) {

    if ((pathComponent == null) || pathComponent.isBlank()) {
      throw new IllegalArgumentException("A classpath entry is required from which to construct a code source");
    }

    return pathComponent;
  }

  /**
   * Renders a classpath entry as a code source location, relying on {@link Path#toUri()} to absolutize
   * the entry and to escape any characters, such as spaces, that are not legal within a URI. Directory
   * entries gain the trailing {@code /-} that matches every class beneath them, while JAR entries name
   * the archive itself.
   *
   * @param pathComponent the classpath entry to render
   * @return the code source location naming the entry
   */
  private static URI asCodeSourceURI (String pathComponent) {

    String uriSpec = Paths.get(pathComponent).toAbsolutePath().normalize().toUri().toString();

    if (pathComponent.endsWith(".jar")) {

      return URI.create(uriSpec);
    }

    return URI.create((uriSpec.endsWith("/") ? uriSpec : uriSpec + '/') + '-');
  }

  /**
   * Returns the {@link CodeSource} constructed from the classpath entry provided at creation.
   *
   * @return the code source associated with this gate
   */
  @Override
  public CodeSource getCodeSource () {

    return codeSource;
  }
}
