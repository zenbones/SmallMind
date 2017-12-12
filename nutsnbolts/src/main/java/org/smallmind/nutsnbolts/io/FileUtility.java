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

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.smallmind.nutsnbolts.time.TimeArithmetic;

public class FileUtility {

  public static boolean isDirectoryEmpty (Path directory) throws IOException {

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {

      return !directoryStream.iterator().hasNext();
    }
  }

  public static void copyTree (Path source, Path destination, FileManipulation... fileManipulations)
    throws IOException {

    copyTree(source, destination, null, fileManipulations);
  }

  public static void copyTree (Path source, Path destination, FileFilter fileFilter, FileManipulation... fileManipulations)
    throws IOException {

    if (Files.exists(source)) {

      Files.createDirectories(destination);
      Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
          throws IOException {

          if ((fileFilter == null) || fileFilter.accept(file.toFile())) {

            Path target;

            Files.copy(source, (target = destination.resolve(source.relativize(file))), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

            if (fileManipulations != null) {
              for (FileManipulation fileManipulation : fileManipulations) {
                fileManipulation.manipulateFile(target);
              }
            }
          }

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs) throws IOException {

          Path target;

          Files.createDirectories(target = destination.resolve(source.relativize(dir)));

          if (fileManipulations != null) {
            for (FileManipulation fileManipulation : fileManipulations) {
              fileManipulation.manipulateDirectory(target);
            }
          }

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory (Path dir, IOException exc)
          throws IOException {

          if (exc != null) {
            throw exc;
          }

          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  public static void deleteTree (Path target)
    throws IOException {

    deleteTree(target, null, null, true);
  }

  public static void deleteTree (Path target, FileFilter fileFilter)
    throws IOException {

    deleteTree(target, fileFilter, null, true);
  }

  public static void deleteTree (Path target, TimeArithmetic timeArithmetic)
    throws IOException {

    deleteTree(target, null, timeArithmetic, true);
  }

  public static void deleteTree (Path target, boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    deleteTree(target, null, null, throwErrorOnDirectoryNotEmpty);
  }

  public static void deleteTree (Path target, FileFilter fileFilter, TimeArithmetic timeArithmetic)
    throws IOException {

    deleteTree(target, fileFilter, timeArithmetic, true);
  }

  public static void deleteTree (Path target, FileFilter fileFilter, boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    deleteTree(target, fileFilter, null, throwErrorOnDirectoryNotEmpty);
  }

  public static void deleteTree (Path target, TimeArithmetic timeArithmetic, boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    deleteTree(target, null, timeArithmetic, throwErrorOnDirectoryNotEmpty);
  }

  public static void deleteTree (Path target, FileFilter fileFilter, TimeArithmetic timeArithmetic, boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    if (Files.exists(target)) {
      Files.walkFileTree(target, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
          throws IOException {

          if ((fileFilter == null) || fileFilter.accept(target.toFile())) {
            if ((timeArithmetic == null) || timeArithmetic.accept(Files.getLastModifiedTime(target).toInstant())) {
              Files.delete(target);
            }
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory (Path dir, IOException ioException)
          throws IOException {

          if (ioException != null) {
            throw ioException;
          }

          try {
            Files.delete(dir);
          } catch (DirectoryNotEmptyException directoryNotEmptyException) {
            if (throwErrorOnDirectoryNotEmpty) {
              throw directoryNotEmptyException;
            }
          }

          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
}