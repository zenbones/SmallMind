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
 * Enum of supported archive formats (JAR and ZIP) that provides helpers for creating, reading, compressing, and extracting archives.
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
   * Returns the conventional file extension for this archive type, without a leading dot.
   *
   * @return the file extension (e.g., {@code "jar"} or {@code "zip"})
   */
  public String getExtension () {

    return extension;
  }

  /**
   * Wraps the supplied stream in an archive input stream appropriate for this format.
   *
   * @param inputStream the raw input stream containing the archive data
   * @return a {@link ZipInputStream} (or {@link java.util.jar.JarInputStream} for JAR) over the input
   * @throws IOException if the stream cannot be created
   */
  public abstract ZipInputStream getInputStream (InputStream inputStream)
    throws IOException;

  /**
   * Wraps the supplied stream in an archive output stream appropriate for this format.
   *
   * @param outputStream the destination stream
   * @param manifest     the manifest to embed in the archive; used only for JAR, ignored for ZIP
   * @return a {@link ZipOutputStream} (or {@link java.util.jar.JarOutputStream} for JAR) over the output
   * @throws IOException if the stream cannot be created
   */
  public abstract ZipOutputStream getOutputStream (OutputStream outputStream, Manifest manifest)
    throws IOException;

  /**
   * Creates an archive entry with the given name appropriate for this format.
   *
   * @param name the entry name within the archive
   * @return a new {@link ZipEntry} (or {@link java.util.jar.JarEntry} for JAR)
   */
  public abstract ZipEntry getEntry (String name);

  /**
   * Iterates all entries in the archive file, invoking the consumer for each one.
   *
   * @param compressedFile the archive file to walk
   * @param entryConsumer  the callback invoked for each archive entry
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
   * Compresses all regular files in a directory tree into a new archive file at the given path.
   *
   * @param sourceDir  the root directory whose contents will be archived
   * @param outputFile the path of the archive file to create or overwrite
   * @throws IOException if reading the source directory or writing the archive fails
   */
  public void compress (Path sourceDir, Path outputFile)
    throws IOException {

    compress(sourceDir, outputFile, null);
  }

  /**
   * Compresses all regular files in a directory tree into a new archive file, embedding the supplied manifest.
   *
   * @param sourceDir  the root directory whose contents will be archived
   * @param outputFile the path of the archive file to create or overwrite
   * @param manifest   the manifest to embed in the archive; used only for JAR, ignored for ZIP
   * @throws IOException if reading the source directory or writing the archive fails
   */
  public void compress (Path sourceDir, Path outputFile, Manifest manifest)
    throws IOException {

    compress(sourceDir, Files.newOutputStream(outputFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), outputFile, manifest);
  }

  /**
   * Compresses all regular files in a directory tree and writes the archive to the supplied stream.
   *
   * @param sourceDir    the root directory whose contents will be archived
   * @param outputStream the stream to which the archive is written
   * @throws IOException if reading the source directory or writing the archive fails
   */
  public void compress (Path sourceDir, OutputStream outputStream)
    throws IOException {

    compress(sourceDir, outputStream, null, null);
  }

  /**
   * Compresses all regular files in a directory tree and writes the archive to the supplied stream, embedding the given manifest.
   *
   * @param sourceDir    the root directory whose contents will be archived
   * @param outputStream the stream to which the archive is written
   * @param manifest     the manifest to embed in the archive; used only for JAR, ignored for ZIP
   * @throws IOException if reading the source directory or writing the archive fails
   */
  public void compress (Path sourceDir, OutputStream outputStream, Manifest manifest)
    throws IOException {

    compress(sourceDir, outputStream, null, manifest);
  }

  /**
   * Core compression implementation that walks the source directory and writes each regular file (except the optionally excluded path) into the archive stream.
   *
   * @param sourceDir    the root directory whose contents will be archived
   * @param outputStream the stream to which the archive is written
   * @param ignoredPath  a path to exclude from the archive (typically the output file itself); may be {@code null}
   * @param manifest     the manifest to embed in the archive; used only for JAR, ignored for ZIP
   * @throws IOException if reading the source directory or writing the archive fails
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
   * Extracts all entries from an archive file into the specified output directory.
   *
   * @param compressedFile the archive file to extract
   * @param outputDir      the destination directory for extracted files
   * @throws IOException if the archive cannot be read or files cannot be written
   */
  public void explode (Path compressedFile, Path outputDir)
    throws IOException {

    explode(compressedFile, outputDir, null);
  }

  /**
   * Extracts all entries from an archive file into the specified output directory, invoking the consumer for each entry.
   *
   * @param compressedFile the archive file to extract
   * @param outputDir      the destination directory for extracted files
   * @param entryConsumer  an optional callback invoked for each entry; may be {@code null}
   * @throws IOException if the archive cannot be read or files cannot be written
   */
  public void explode (Path compressedFile, Path outputDir, ZipEntryConsumer entryConsumer)
    throws IOException {

    explode(Files.newInputStream(compressedFile, StandardOpenOption.READ), outputDir, entryConsumer);
  }

  /**
   * Extracts all entries from an archive stream into the specified output directory.
   *
   * @param inputStream the stream containing the archive data
   * @param outputDir   the destination directory for extracted files
   * @throws IOException if the archive cannot be read or files cannot be written
   */
  public void explode (InputStream inputStream, Path outputDir)
    throws IOException {

    explode(inputStream, outputDir, null);
  }

  /**
   * Extracts all entries from an archive stream into the specified output directory, invoking the consumer for each entry.
   *
   * @param inputStream   the stream containing the archive data
   * @param outputDir     the destination directory for extracted files
   * @param entryConsumer an optional callback invoked for each entry; may be {@code null}
   * @throws IOException if the archive cannot be read or files cannot be written
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
