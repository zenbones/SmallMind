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
package org.smallmind.nutsnbolts.zip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.smallmind.nutsnbolts.io.PathUtility;
import org.smallmind.nutsnbolts.lang.WrappedException;

public enum CompressionType {

  JAR("jar") {
    @Override
    public ZipInputStream getInputStream (InputStream inputStream)
      throws IOException {

      return new JarInputStream(inputStream);
    }

    @Override
    public ZipOutputStream getOutputStream (OutputStream outputStream, Manifest manifest)
      throws IOException {

      return new JarOutputStream(outputStream, (manifest == null) ? new Manifest() : manifest);
    }

    @Override
    public ZipEntry getEntry (String name) {

      return new JarEntry(name);
    }
  },
  ZIP("zip") {
    @Override
    public ZipInputStream getInputStream (InputStream inputStream) {

      return new ZipInputStream(inputStream);
    }

    @Override
    public ZipOutputStream getOutputStream (OutputStream outputStream, Manifest manifest) {

      return new ZipOutputStream(outputStream);
    }

    @Override
    public ZipEntry getEntry (String name) {

      return new ZipEntry(name);
    }
  };

  private final String extension;

  CompressionType (String extension) {

    this.extension = extension;
  }

  public String getExtension () {

    return extension;
  }

  public abstract ZipInputStream getInputStream (InputStream inputStream)
    throws IOException;

  public abstract ZipOutputStream getOutputStream (OutputStream outputStream, Manifest manifest)
    throws IOException;

  public abstract ZipEntry getEntry (String name);

  public void walk (Path compressedFile, Consumer<ZipEntry> entryConsumer)
    throws IOException {

    try (ZipInputStream zipInputStream = getInputStream(Files.newInputStream(compressedFile, StandardOpenOption.READ))) {

      ZipEntry zipEntry;

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        entryConsumer.accept(zipEntry);
      }
    }
  }

  public void compress (Path sourceDir, Path outputFile)
    throws IOException {

    compress(sourceDir, outputFile, null);
  }

  public void compress (Path sourceDir, Path outputFile, Manifest manifest)
    throws IOException {

    Path normalizedSourceDir = sourceDir.toAbsolutePath().normalize();

    try (Stream<Path> pathStream = Files.walk(normalizedSourceDir)) {
      try (ZipOutputStream zipOutputStream = getOutputStream(Files.newOutputStream(outputFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), manifest)) {
        try {
          pathStream.forEach((compressionPath) -> {

            if (Files.isRegularFile(compressionPath) && (!compressionPath.equals(outputFile))) {
              try {

                ZipEntry zipEntry = getEntry(PathUtility.asResourceString(normalizedSourceDir.relativize(compressionPath)));

                zipEntry.setTime(Files.getLastModifiedTime(compressionPath).toMillis());
                zipOutputStream.putNextEntry(zipEntry);
                Files.copy(compressionPath, zipOutputStream);
                zipOutputStream.closeEntry();
              } catch (IOException ioException) {
                throw new WrappedException(ioException);
              }
            }
          });
        } catch (WrappedException wrappedException) {
          throw wrappedException.convert(IOException.class);
        }
      }
    }
  }

  public void explode (Path compressedFile, Path outputDir)
    throws IOException {

    explode(compressedFile, outputDir, null);
  }

  public void explode (Path compressedFile, Path outputDir, Consumer<String> loggingConsumer)
    throws IOException {

    Files.createDirectories(outputDir);

    try (ZipInputStream zipInputStream = getInputStream(Files.newInputStream(compressedFile, StandardOpenOption.READ))) {

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

          Path parentPath;

          if (((parentPath = entryPath.getParent()) != null)) {
            Files.createDirectories(parentPath);
          }

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
