/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class FileIterator implements Iterator<File>, Iterable<File> {

  private FileFilter fileFilter;
  private LinkedList<File> directoryStack;
  private File currentFile;

  public FileIterator (File directory) {

    this(directory, null);
  }

  public FileIterator (File directory, FileFilter fileFilter) {

    if (!directory.exists()) {
      throw new IllegalArgumentException("Specified directory(" + directory.getAbsolutePath() + ") doesn't exist");
    }
    else if (!directory.isDirectory()) {
      throw new IllegalArgumentException("Specified file(" + directory.getAbsolutePath() + ") isn't a directory");
    }

    this.fileFilter = fileFilter;
    directoryStack = new LinkedList<File>();
    directoryStack.add(directory);

    currentFile = getNextFile();
  }

  private File getNextFile () {

    File file;

    while (!directoryStack.isEmpty()) {
      file = directoryStack.removeFirst();
      if (file.isDirectory()) {

        LinkedList<File> appendedList = new LinkedList<File>();

        for (File child : file.listFiles(fileFilter)) {
          if (child.isFile()) {
            appendedList.addFirst(child);
          }
          else {
            appendedList.addLast(child);
          }
        }

        if (!appendedList.isEmpty()) {
          directoryStack.addAll(0, appendedList);
        }
      }
      else {
        return file;
      }
    }

    return null;
  }

  public boolean hasNext () {

    return currentFile != null;
  }

  public File next () {

    File nextFile;

    if (currentFile == null) {
      throw new NoSuchElementException();
    }

    nextFile = currentFile;
    currentFile = getNextFile();

    return nextFile;
  }

  public void remove () {

    throw new UnsupportedOperationException();
  }

  public Iterator<File> iterator () {

    return this;
  }
}

