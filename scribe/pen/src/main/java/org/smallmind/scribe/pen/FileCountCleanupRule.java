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
package org.smallmind.scribe.pen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

public class FileCountCleanupRule implements CleanupRule<FileCountCleanupRule> {

  private final LinkedList<Path> pathList = new LinkedList<>();
  private int maximum;

  public FileCountCleanupRule () {

  }

  public FileCountCleanupRule (int maximum) {

    this.maximum = maximum;
  }

  public int getMaximum () {

    return maximum;
  }

  public void setMaximum (int maximum) {

    this.maximum = maximum;
  }

  @Override
  public FileCountCleanupRule copy () {

    return new FileCountCleanupRule(maximum);
  }

  @Override
  public boolean willCleanup (Path possiblePath) {

    pathList.add(possiblePath);

    return false;
  }

  @Override
  public void finish ()
    throws IOException {

    if (pathList.size() > maximum) {

      ModificationTimeComparator modificationTimeComparator;

      pathList.sort(modificationTimeComparator = new ModificationTimeComparator());

      if (modificationTimeComparator.getIoException() != null) {
        throw modificationTimeComparator.getIoException();
      }

      do {
        Files.deleteIfExists(pathList.removeLast());
      } while (pathList.size() > maximum);
    }
  }

  private class ModificationTimeComparator implements Comparator<Path> {

    private final HashMap<Path, Long> timeMap = new HashMap<>();
    private IOException ioException;

    public IOException getIoException () {

      return ioException;
    }

    @Override
    public int compare (Path path1, Path path2) {

      return getModificationTime(path1).compareTo(getModificationTime(path2));
    }

    private Long getModificationTime (Path path) {

      Long time;

      if ((time = timeMap.get(path)) == null) {
        try {
          timeMap.put(path, time = Files.getLastModifiedTime(path).toMillis());
        } catch (IOException ioException) {
          this.ioException = ioException;
          timeMap.put(path, 0L);
        }
      }

      return time;
    }
  }
}
