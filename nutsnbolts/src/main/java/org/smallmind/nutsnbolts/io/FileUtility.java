/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;

public class FileUtility {

  private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = new FileAttribute[0];

  public static CopyTreeConfigurationBuilder copyBuilder (Path source, Path destination) {

    return new CopyTreeConfigurationBuilder(source, destination);
  }

  public static DeleteTreeConfigurationBuilder deleteBuilder (Path target) {

    return new DeleteTreeConfigurationBuilder(target);
  }

  public static boolean isDirectoryEmpty (Path directory)
    throws IOException {

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {

      return !directoryStream.iterator().hasNext();
    }
  }

  public static void copyTree (Path source, Path destination, boolean includeSourceDirectory, PathFilter... pathFilters)
    throws IOException {

    copyTree(source, destination, includeSourceDirectory, pathFilters, NO_FILE_ATTRIBUTES);
  }

  public static void copyTree (Path source, Path destination, boolean includeSourceDirectory, PathFilter[] pathFilters, FileAttribute<?>... directoryAttributes)
    throws IOException {

    if (Files.exists(source)) {
      if (Files.isRegularFile(source)) {
        if (filter(source, pathFilters)) {
          if (Files.exists(destination)) {
            if (Files.isDirectory(destination)) {
              Files.copy(source, destination.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            } else {
              Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
          } else {
            createDirectories(destination.getParent(), directoryAttributes);
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
          }
        }
      } else if (Files.exists(destination) && (!Files.isDirectory(destination))) {
        throw new IOException("Can not move directory(" + source + ") to file(" + destination + ")");
      } else {

        createDirectories(destination, directoryAttributes);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {

          @Override
          public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
            throws IOException {

            if (filter(file, pathFilters)) {
              Files.copy(file, (destination.resolve(includeSourceDirectory ? source.getParent().relativize(file) : source.relativize(file))), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }

            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs)
            throws IOException {

            if ((!source.equals(dir)) || includeSourceDirectory) {
              createDirectories(destination.resolve(includeSourceDirectory ? source.getParent().relativize(dir) : source.relativize(dir)), directoryAttributes);
            }

            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory (Path dir, IOException ioException)
            throws IOException {

            if (ioException != null) {
              throw ioException;
            }

            return FileVisitResult.CONTINUE;
          }
        });
      }
    }
  }

  public static void deleteTree (Path target, boolean includeTargetDirectory, boolean throwErrorOnDirectoryNotEmpty, PathFilter... pathFilters)
    throws IOException {

    if (Files.exists(target)) {
      Files.walkFileTree(target, new SimpleFileVisitor<>() {

        @Override
        public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
          throws IOException {

          if (filter(file, pathFilters)) {
            Files.delete(file);
          }

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory (Path dir, IOException ioException)
          throws IOException {

          if (ioException != null) {
            throw ioException;
          }

          if ((!target.equals(dir)) || includeTargetDirectory) {
            try {
              Files.delete(dir);
            } catch (DirectoryNotEmptyException directoryNotEmptyException) {
              if (throwErrorOnDirectoryNotEmpty) {
                throw directoryNotEmptyException;
              }
            }
          }

          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  private static void createDirectories (Path path, FileAttribute<?>... fileAttributes)
    throws IOException {

    // Why this is necessary I don't know, but passing in a nul FileAttribute[] causes a NUllPointerException despite the signature
    if ((fileAttributes == null) || (fileAttributes.length == 0)) {
      Files.createDirectories(path);
    } else {
      Files.createDirectories(path, fileAttributes);
    }
  }

  private static boolean filter (Path path, PathFilter... pathFilters)
    throws IOException {

    if ((pathFilters == null) || (pathFilters.length == 0)) {

      return true;
    } else {
      for (PathFilter pathFilter : pathFilters) {
        if (!pathFilter.accept(path)) {

          return false;
        }
      }

      return true;
    }
  }
}
