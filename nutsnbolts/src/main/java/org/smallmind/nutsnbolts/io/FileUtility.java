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

public class FileUtility {

  public static CopyTreeConfiguration copyConfiguration (Path source, Path destination) {

    return new CopyTreeConfiguration(source, destination);
  }

  public static DeleteTreeConfiguration deleteConfiguration (Path target) {

    return new DeleteTreeConfiguration(target);
  }

  public static boolean isDirectoryEmpty (Path directory) throws IOException {

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {

      return !directoryStream.iterator().hasNext();
    }
  }

  public static void copyTree (Path source, Path destination, boolean includeSourceDirectory, PathFilter... pathFilters)
    throws IOException {

    if (Files.exists(source)) {

      Files.createDirectories(destination);
      Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
          throws IOException {

          if (filter(file, pathFilters)) {
            Files.copy(source, (destination.resolve(source.relativize(file))), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
          }

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs) throws IOException {

          if ((!source.equals(dir)) || includeSourceDirectory) {
            Files.createDirectories(destination.resolve(source.relativize(dir)));
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

  public static void deleteTree (Path target, boolean includeTargetDirectory, boolean throwErrorOnDirectoryNotEmpty, PathFilter... pathFilters)
    throws IOException {

    if (Files.exists(target)) {
      Files.walkFileTree(target, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
          throws IOException {

          if (filter(file, pathFilters)) {
            Files.delete(target);
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