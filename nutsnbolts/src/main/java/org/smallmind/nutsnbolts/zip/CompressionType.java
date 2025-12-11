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
package org.smallmind.nutsnbolts.zip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

  public void walk (Path compressedFile, ZipEntryConsumer entryConsumer)
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

    compress(sourceDir, Files.newOutputStream(outputFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), outputFile, manifest);
  }

  public void compress (Path sourceDir, OutputStream outputStream)
    throws IOException {

    compress(sourceDir, outputStream, null, null);
  }

  public void compress (Path sourceDir, OutputStream outputStream, Manifest manifest)
    throws IOException {

    compress(sourceDir, outputStream, null, manifest);
  }

  private void compress (Path sourceDir, OutputStream outputStream, Path ignoredPath, Manifest manifest)
    throws IOException {

    Path normalizedSourceDir = sourceDir.toAbsolutePath().normalize();

    try (Stream<Path> pathStream = Files.walk(normalizedSourceDir)) {
      try (ZipOutputStream zipOutputStream = getOutputStream(outputStream, manifest)) {
        try {
          pathStream.forEach((compressionPath) -> {

            if (Files.isRegularFile(compressionPath) && (!compressionPath.equals(ignoredPath))) {
              try {

                ZipEntry zipEntry = getEntry(PathUtility.asResourceString(normalizedSourceDir.relativize(compressionPath)));

                zipEntry.setTime(Files.getLastModifiedTime(compressionPath).toMillis());
                zipEntry.setSize(Files.size(compressionPath));

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

  public void explode (Path compressedFile, Path outputDir, ZipEntryConsumer entryConsumer)
    throws IOException {

    explode(Files.newInputStream(compressedFile, StandardOpenOption.READ), outputDir, entryConsumer);
  }

  public void explode (InputStream inputStream, Path outputDir)
    throws IOException {

    explode(inputStream, outputDir, null);
  }

  public void explode (InputStream inputStream, Path outputDir, ZipEntryConsumer entryConsumer)
    throws IOException {

    Files.createDirectories(outputDir);

    try (ZipInputStream zipInputStream = getInputStream(inputStream)) {

      ZipEntry zipEntry;
      byte[] buffer = new byte[2048];

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {

        Path entryPath = outputDir.resolve(zipEntry.getName());

        if (entryConsumer != null) {
          entryConsumer.accept(zipEntry);
        }

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
