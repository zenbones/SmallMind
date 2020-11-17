/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class SingularityJarURLConnection extends URLConnection {

  private static final AtomicReference<CachedJarFile> CACHED_JAR_FILE_REFERENCE = new AtomicReference<>();

  public SingularityJarURLConnection (URL url) {

    super(url);
  }

  @Override
  public void connect () {

  }

  @Override
  public InputStream getInputStream ()
    throws IOException {

    JarFile jarFile;
    JarEntry jarEntry;
    String outerEntryName;
    String innerEntryName;
    int atPos;

    if ((atPos = url.getPath().indexOf("@/")) < 0) {
      throw new MalformedURLException("no @/ found in url spec:" + url.getPath());
    } else {

      int bangPos;

      jarFile = new JarFile(new URL(url.getPath().substring(0, atPos)).getFile());

      if ((bangPos = url.getPath().indexOf("!/", atPos + 3)) < 0) {

        if ((jarEntry = jarFile.getJarEntry(url.getPath().substring(atPos + 2))) != null) {
          return jarFile.getInputStream(jarEntry);
        }
      } else {

        outerEntryName = url.getPath().substring(atPos + 2, bangPos);
        innerEntryName = url.getPath().substring(bangPos + 2);

        if ((jarEntry = jarFile.getJarEntry(outerEntryName)) != null) {

          CachedJarFile cachedJarFile;
          InputStream cachedInputStream;

          if (((cachedJarFile = CACHED_JAR_FILE_REFERENCE.get()) == null) || (!cachedJarFile.getEntryName().equals(outerEntryName))) {
            synchronized (CACHED_JAR_FILE_REFERENCE) {
              if (((cachedJarFile = CACHED_JAR_FILE_REFERENCE.get()) == null) || (!cachedJarFile.getEntryName().equals(outerEntryName))) {
                CACHED_JAR_FILE_REFERENCE.set(cachedJarFile = new CachedJarFile(outerEntryName, new JarInputStream(jarFile.getInputStream(jarEntry))));
              }
            }
          }
          if ((cachedInputStream = cachedJarFile.getInputStream(innerEntryName)) != null) {

            return cachedInputStream;
          }
        }
      }

      throw new FileNotFoundException(getURL().getPath());
    }
  }

  @Override
  public int getContentLength () {

    return 0;
  }
}
