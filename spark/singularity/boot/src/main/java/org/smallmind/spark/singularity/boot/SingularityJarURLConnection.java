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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * {@link URLConnection} implementation that reads an entry from a nested jar addressed by a {@code singularity:} URL.
 * <p>Two URL shapes are supported:
 * <ul>
 *   <li>{@code singularity:<outer>@/<entry>} for an entry that lives directly in the outer jar.</li>
 *   <li>{@code singularity:<outer>@/<inner-jar>!/<entry>} for an entry nested inside a jar bundled under
 *       {@code META-INF/singularity/lib/}; nested jars are memoized in a soft-reference cache, keyed by outer jar and
 *       nested jar, so that each is read out of the outer jar once. The outer jar is opened only to fill that cache.</li>
 * </ul>
 * <p>The outer jar's {@code Singularity-Jar-Cache} manifest attribute selects how a nested jar is held: {@code true},
 * or no attribute, holds it as a {@link CachedJarFile}; {@code false} holds it as a {@link RecompressedJarFile}.
 */
public class SingularityJarURLConnection extends URLConnection {

  private static final Attributes.Name JAR_CACHE_ATTRIBUTE = new Attributes.Name("Singularity-Jar-Cache");
  private static final ConcurrentHashMap<String, SoftReference<NestedJarFile>> NESTED_JAR_FILE_MAP = new ConcurrentHashMap<>();

  /**
   * Binds this connection to a {@code singularity:} URL without performing any I/O.
   *
   * @param url the URL whose content will be streamed on demand
   */
  public SingularityJarURLConnection (URL url) {

    super(url);
  }

  /**
   * No-op; {@code singularity} connections defer all I/O until {@link #getInputStream()} is invoked.
   */
  @Override
  public void connect () {

  }

  /**
   * Opens a stream over the entry addressed by this connection's URL, consulting the shared cache when the entry
   * lives inside a nested jar.
   *
   * @return a readable {@link InputStream} positioned at the start of the requested entry
   * @throws MalformedURLException if the URL path does not contain the required {@code @/} separator
   * @throws FileNotFoundException if the outer or inner entry is absent from the enclosing jar
   * @throws IOException           if the jar cannot be opened or the entry cannot be read
   */
  @Override
  public InputStream getInputStream ()
    throws IOException {

    int atPos;
    int bangPos;

    if ((atPos = url.getPath().indexOf("@/")) < 0) {
      throw new MalformedURLException("no @/ found in url spec:" + url.getPath());
    } else if ((bangPos = url.getPath().indexOf("!/", atPos + 3)) < 0) {
      try (JarFile jarFile = openOuterJarFile(atPos)) {

        JarEntry jarEntry;

        if ((jarEntry = jarFile.getJarEntry(url.getPath().substring(atPos + 2))) != null) {
          try (InputStream entryInputStream = jarFile.getInputStream(jarEntry)) {
            return new ByteArrayInputStream(entryInputStream.readAllBytes());
          }
        }
      }
    } else {

      NestedJarFile nestedJarFile;
      InputStream nestedInputStream;

      if (((nestedJarFile = getNestedJarFile(atPos, bangPos)) != null) && ((nestedInputStream = nestedJarFile.getInputStream(url.getPath().substring(bangPos + 2))) != null)) {

        return nestedInputStream;
      }
    }

    throw new FileNotFoundException(getURL().getPath());
  }

  /**
   * Returns the in-memory form of the nested jar this connection's URL points into, building it on first use. The
   * outer jar is opened only on a cache miss, so a read from an already held nested jar performs no file I/O.
   *
   * @param atPos   position of the {@code @/} separator in the URL path
   * @param bangPos position of the {@code !/} separator in the URL path
   * @return the nested jar, or {@code null} if the outer jar has no such nested jar
   * @throws IOException if the outer jar or the nested jar cannot be read
   */
  private NestedJarFile getNestedJarFile (int atPos, int bangPos)
    throws IOException {

    String cacheKey = url.getPath().substring(0, bangPos);
    SoftReference<NestedJarFile> nestedJarFileReference;
    NestedJarFile nestedJarFile;

    if (((nestedJarFileReference = NESTED_JAR_FILE_MAP.get(cacheKey)) == null) || ((nestedJarFile = nestedJarFileReference.get()) == null)) {
      synchronized (NESTED_JAR_FILE_MAP) {
        if (((nestedJarFileReference = NESTED_JAR_FILE_MAP.get(cacheKey)) == null) || ((nestedJarFile = nestedJarFileReference.get()) == null)) {

          String outerEntryName = url.getPath().substring(atPos + 2, bangPos);

          try (JarFile jarFile = openOuterJarFile(atPos)) {

            JarEntry jarEntry;

            if ((jarEntry = jarFile.getJarEntry(outerEntryName)) == null) {

              return null;
            }

            if (isJarCacheEnabled(jarFile)) {
              try (InputStream nestedJarInputStream = jarFile.getInputStream(jarEntry)) {
                nestedJarFile = new CachedJarFile(outerEntryName, nestedJarInputStream.readAllBytes());
              }
            } else {
              try (JarInputStream nestedJarInputStream = new JarInputStream(jarFile.getInputStream(jarEntry))) {
                nestedJarFile = new RecompressedJarFile(outerEntryName, nestedJarInputStream);
              }
            }

            NESTED_JAR_FILE_MAP.put(cacheKey, new SoftReference<>(nestedJarFile));
          }
        }
      }
    }

    return nestedJarFile;
  }

  /**
   * @param jarFile the outer Singularity jar
   * @return {@code false} only if the outer jar's manifest sets {@code Singularity-Jar-Cache} to {@code false}
   * @throws IOException if the manifest cannot be read
   */
  private boolean isJarCacheEnabled (JarFile jarFile)
    throws IOException {

    Manifest manifest;
    String jarCache;

    return ((manifest = jarFile.getManifest()) == null) || ((jarCache = manifest.getMainAttributes().getValue(JAR_CACHE_ATTRIBUTE)) == null) || Boolean.parseBoolean(jarCache);
  }

  /**
   * @param atPos position of the {@code @/} separator in the URL path
   * @return the outer Singularity jar named by the part of the URL path before {@code @/}; the caller closes it
   * @throws IOException if the jar cannot be opened
   */
  private JarFile openOuterJarFile (int atPos)
    throws IOException {

    return new JarFile(URI.create(url.getPath().substring(0, atPos)).toURL().getFile());
  }

  /**
   * Reports that the content length is indeterminate without opening the entry.
   *
   * @return always {@code 0}; callers should stream the content to determine its true size
   */
  @Override
  public int getContentLength () {

    return 0;
  }
}
