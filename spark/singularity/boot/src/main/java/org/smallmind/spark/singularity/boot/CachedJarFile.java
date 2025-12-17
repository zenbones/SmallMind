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
package org.smallmind.spark.singularity.boot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Lightweight jar cache that stores compressed entry data from an embedded jar to accelerate repeat lookups.
 */
public class CachedJarFile {

  private final HashMap<String, byte[]> entryMap = new HashMap<>();
  private final String entryName;

  /**
   * Reads each entry from the supplied inner jar stream and stores a compressed copy for later retrieval.
   *
   * @param entryName      name of the enclosing jar entry being cached
   * @param jarInputStream stream of the nested jar content positioned at its first entry
   * @throws IOException if any entry cannot be read or buffered
   */
  public CachedJarFile (String entryName, JarInputStream jarInputStream)
    throws IOException {

    JarEntry innerJarEntry;

    this.entryName = entryName;

    while ((innerJarEntry = jarInputStream.getNextJarEntry()) != null) {
      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream)) {

          byte[] buffer = new byte[8192];
          int bytesRead;

          while ((bytesRead = jarInputStream.read(buffer)) >= 0) {
            deflaterOutputStream.write(buffer, 0, bytesRead);
          }
        }

        entryMap.put(innerJarEntry.getName(), byteArrayOutputStream.toByteArray());
      }
    }
  }

  /**
   * @return the name of the jar entry that produced this cache
   */
  public String getEntryName () {

    return entryName;
  }

  /**
   * Returns an {@link InputStream} for a previously cached entry or {@code null} if the name is unknown.
   *
   * @param name the inner entry name to resolve
   * @return an inflated stream for the cached entry or {@code null} when not cached
   */
  public InputStream getInputStream (String name) {

    byte[] contents;

    if ((contents = entryMap.get(name)) != null) {

      return new InflaterInputStream(new ByteArrayInputStream(contents));
    }

    return null;
  }
}
