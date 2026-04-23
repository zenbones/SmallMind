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
package org.smallmind.nutsnbolts.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A {@link Resource} backed by a named entry inside a JAR file, specified with the path
 * format {@code /path/to/archive.jar!/entry/name}.
 */
public class JarResource extends AbstractResource {

  /**
   * Constructs a {@code JarResource} for the given jar-entry path.
   *
   * @param path combined path containing the jar file location and entry name separated by {@code !}
   */
  public JarResource (String path) {

    super(path);
  }

  /**
   * Returns the scheme identifier for this resource type.
   *
   * @return the string {@code "jar"}
   */
  @Override
  public String getScheme () {

    return "jar";
  }

  /**
   * Opens the specified entry within the JAR archive for reading.
   *
   * @return an input stream for the JAR entry, or {@code null} if no matching entry is found
   * @throws ResourceException if the JAR file cannot be opened due to an I/O error
   */
  @Override
  public InputStream getInputStream ()
    throws ResourceException {

    try {

      JarFile jarFile;
      JarEntry jarEntry;
      Enumeration<JarEntry> entryEnumeration;
      String elementPath;
      int bangPos = getPath().indexOf('!');

      jarFile = new JarFile(getPath().substring(0, bangPos));
      elementPath = (getPath().charAt(bangPos + 1) == '/') ? getPath().substring(bangPos + 2) : getPath().substring(bangPos + 1);
      entryEnumeration = jarFile.entries();
      while (entryEnumeration.hasMoreElements()) {
        if ((jarEntry = entryEnumeration.nextElement()).getName().equals(elementPath)) {
          return jarFile.getInputStream(jarEntry);
        }
      }

      return null;
    } catch (IOException ioException) {
      throw new ResourceException(ioException);
    }
  }
}
