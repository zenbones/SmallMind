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

/**
 * Supports compressing and extracting archives in either JAR or ZIP formats.
 * Provides helpers for iterating entries, building archives, and exploding archives to disk.
 */
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

  /**
   * @return standard file extension (without dot) for the archive type
   */
  public String getExtension () {

    return extension;
  }

  /**
   * Builds an input stream capable of reading this archive type.
   *
   * @param inputStream raw input stream
   * @return {@link ZipInputStream} specialized for the archive type
   * @throws IOException if the stream cannot be created
   */
  public abstract ZipInputStream getInputStream (InputStream inputStream)
    throws IOException;

  /**
   * Builds an output stream capable of writing this archive type.
   *
   * @param outputStream destination stream
   * @param manifest     optional manifest (ignored for ZIP)
   * @return {@link ZipOutputStream} specialized for the archive type
   * @throws IOException if the stream cannot be created
   */
  public abstract ZipOutputStream getOutputStream (OutputStream outputStream, Manifest manifest)
    throws IOException;

  /**
   * Creates an archive entry instance for the given path.
   *
   * @param name entry name within the archive
   * @return archive entry appropriate to the type
   */
  public abstract ZipEntry getEntry (String name);

  /**
   * Iterates the entries in an archive file, invoking the consumer for each entry encountered.
   *
   * @param compressedFile archive to inspect
   * @param entryConsumer  callback receiving each entry
   * @throws IOException if the archive cannot be read
   */
  public void walk (Path compressedFile, ZipEntryConsumer entryConsumer)
    throws IOException {

    try (ZipInputStream zipInputStream = getInputStream(Files.newInputStream(compressedFile, StandardOpenOption.READ))) {

      ZipEntry zipEntry;

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        entryConsumer.accept(zipEntry);
      }
    }
  }

  /**
   * Compresses the contents of a directory into a new archive file.
   *
   * @param sourceDir  directory whose contents will be archived
   * @param outputFile archive file to create
   * @throws IOException if reading or writing fails
   */
  public void compress (Path sourceDir, Path outputFile)
    throws IOException {

    compress(sourceDir, outputFile, null);
  }

  /**
   * Compresses a directory into a new archive file with an optional manifest.
   *
   * @param sourceDir  directory whose contents will be archived
   * @param outputFile archive file to create
   * @param manifest   manifest to embed (JAR only)
   * @throws IOException if reading or writing fails
   */
  public void compress (Path sourceDir, Path outputFile, Manifest manifest)
    throws IOException {

    compress(sourceDir, Files.newOutputStream(outputFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), outputFile, manifest);
  }

  /**
   * Compresses a directory into an archive written to the provided stream.
   *
   * @param sourceDir    directory whose contents will be archived
   * @param outputStream destination stream
   * @throws IOException if reading or writing fails
   */
  public void compress (Path sourceDir, OutputStream outputStream)
    throws IOException {

    compress(sourceDir, outputStream, null, null);
  }

  /**
   * Compresses a directory into an archive written to the provided stream, including an optional manifest.
   *
   * @param sourceDir    directory whose contents will be archived
   * @param outputStream destination stream
   * @param manifest     manifest to embed (JAR only)
   * @throws IOException if reading or writing fails
   */
  public void compress (Path sourceDir, OutputStream outputStream, Manifest manifest)
    throws IOException {

    compress(sourceDir, outputStream, null, manifest);
  }

  /**
   * Internal compression helper allowing exclusion of a single path (to prevent archiving the output file itself).
   *
   * @param sourceDir    directory whose contents will be archived
   * @param outputStream destination stream
   * @param ignoredPath  optional path to skip while walking the source tree
   * @param manifest     manifest to embed (JAR only)
   * @throws IOException if reading or writing fails
   */
  private void compress (Path sourceDir, OutputStream outputStream, Path ignoredPath, Manifest manifest)
    throws IOException {

    Path normalizedSourceDir = sourceDir.toAbsolutePath().normalize();

    // Walk the source tree, copying each regular file into the archive while preserving timestamps and sizes.
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

  /**
   * Extracts an archive to the supplied output directory.
   *
   * @param compressedFile archive to explode
   * @param outputDir      destination directory
   * @throws IOException if extraction fails
   */
  public void explode (Path compressedFile, Path outputDir)
    throws IOException {

    explode(compressedFile, outputDir, null);
  }

  /**
   * Extracts an archive to the supplied output directory, invoking a consumer for each entry.
   *
   * @param compressedFile archive to explode
   * @param outputDir      destination directory
   * @param entryConsumer  optional callback receiving each entry
   * @throws IOException if extraction fails
   */
  public void explode (Path compressedFile, Path outputDir, ZipEntryConsumer entryConsumer)
    throws IOException {

    explode(Files.newInputStream(compressedFile, StandardOpenOption.READ), outputDir, entryConsumer);
  }

  /**
   * Extracts an archive from a stream to the supplied output directory.
   *
   * @param inputStream archive stream
   * @param outputDir   destination directory
   * @throws IOException if extraction fails
   */
  public void explode (InputStream inputStream, Path outputDir)
    throws IOException {

    explode(inputStream, outputDir, null);
  }

  /**
   * Extracts an archive from a stream to the supplied output directory, invoking a consumer for each entry.
   *
   * @param inputStream   archive stream
   * @param outputDir     destination directory
   * @param entryConsumer optional callback receiving each entry
   * @throws IOException if extraction fails
   */
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
