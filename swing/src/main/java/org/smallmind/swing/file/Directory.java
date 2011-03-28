/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.swing.file;

import java.io.File;
import java.util.LinkedList;

public class Directory extends File {

  private static final Directory[] NO_CHILDREN = new Directory[0];

  public Directory (String pathName) {

    super(pathName);

    if (!isDirectory()) {
      throw new IllegalStateException("The path name(" + pathName + ") does not represent a directory structure");
    }
  }

  public boolean hasChildren () {

    File[] files;

    if ((files = listFiles()) == null) {
      return false;
    }

    for (File file : files) {
      if (file.isDirectory()) {
        return true;
      }
    }

    return false;
  }

  public Directory[] getChildren () {

    Directory[] directories;
    LinkedList<Directory> directoryList;
    File[] files;

    if ((files = listFiles()) == null) {
      return NO_CHILDREN;
    }

    directoryList = new LinkedList<Directory>();

    for (File file : files) {
      if (file.isDirectory()) {
        directoryList.add(new Directory(file.getAbsolutePath()));
      }
    }

    directories = new Directory[directoryList.size()];
    directoryList.toArray(directories);

    return directories;
  }

}
