/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.smallmind.nutsnbolts.util.TimeArithmetic;

public class FileUtility {

  public static void deleteTree (Path directory)
    throws IOException {

    deleteTree(directory, null, null, true);
  }

  public static void deleteTree (Path directory, final FileFilter fileFilter)
    throws IOException {

    deleteTree(directory, fileFilter, null, true);
  }

  public static void deleteTree (Path directory, final TimeArithmetic timeArithmetic)
    throws IOException {

    deleteTree(directory, null, timeArithmetic, true);
  }

  public static void deleteTree (Path directory, final boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    deleteTree(directory, null, null, throwErrorOnDirectoryNotEmpty);
  }

  public static void deleteTree (Path directory, final FileFilter fileFilter, final TimeArithmetic timeArithmetic)
    throws IOException {

    deleteTree(directory, fileFilter, timeArithmetic, true);
  }

  public static void deleteTree (Path directory, final FileFilter fileFilter, final boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    deleteTree(directory, fileFilter, null, throwErrorOnDirectoryNotEmpty);
  }

  public static void deleteTree (Path directory, final TimeArithmetic timeArithmetic, final boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    deleteTree(directory, null, timeArithmetic, throwErrorOnDirectoryNotEmpty);
  }

  public static void deleteTree (Path directory, final FileFilter fileFilter, final TimeArithmetic timeArithmetic, final boolean throwErrorOnDirectoryNotEmpty)
    throws IOException {

    if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
      if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
        Files.delete(directory);
      } else {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
            throws IOException {

            if ((fileFilter == null) || fileFilter.accept(file.toFile())) {
              if ((timeArithmetic == null) || timeArithmetic.accept(Files.getLastModifiedTime(file).toMillis())) {
                Files.delete(file);
              }
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory (Path dir, IOException exc)
            throws IOException {

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
}