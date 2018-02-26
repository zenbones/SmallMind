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
package org.smallmind.nutsnbolts.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtility {

  public static void walk (Path zipFile, Consumer<ZipEntry> entryConsumer)
    throws IOException {

    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile, StandardOpenOption.READ))) {

      ZipEntry zipEntry;

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        entryConsumer.accept(zipEntry);
      }
    }
  }

  public static void explode (Path zipFile, Path outputDir)
    throws IOException {

    explode(zipFile, outputDir, null);
  }

  public static void explode (Path zipFile, Path outputDir, Consumer<String> loggingConsumer)
    throws IOException {

    Files.createDirectories(outputDir);

    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile, StandardOpenOption.READ))) {

      ZipEntry zipEntry;
      byte[] buffer = new byte[2048];

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        if (loggingConsumer != null) {
          loggingConsumer.accept(zipEntry.getName());
        }

        Path entryPath = outputDir.resolve(zipEntry.getName());

        if (zipEntry.isDirectory()) {
          Files.createDirectories(entryPath);
        } else {
          try (OutputStream fileOutputStream = Files.newOutputStream(entryPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            int bytesRead;

            while ((bytesRead = zipInputStream.read(buffer)) >= 0) {
              fileOutputStream.write(buffer, 0, bytesRead);
            }
          }
        }
      }
    }
  }
}
