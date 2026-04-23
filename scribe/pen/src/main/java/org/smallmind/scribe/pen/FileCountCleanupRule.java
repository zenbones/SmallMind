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

/**
 * A {@link CleanupRule} that limits the number of retained rollover files to a configured maximum
 * by collecting all candidates during iteration and then deleting the oldest ones in {@link #finish()}.
 */
public class FileCountCleanupRule implements CleanupRule<FileCountCleanupRule> {

  private final LinkedList<Path> pathList = new LinkedList<>();
  private int maximum;

  /**
   * Constructs a rule that retains at most {@code maximum} rollover files.
   *
   * @param maximum the maximum number of rolled files to keep
   */
  public FileCountCleanupRule (int maximum) {

    this.maximum = maximum;
  }

  /**
   * Returns the maximum number of rollover files this rule will retain.
   *
   * @return the configured retention count
   */
  public int getMaximum () {

    return maximum;
  }

  /**
   * Sets the maximum number of rollover files to retain.
   *
   * @param maximum the new retention count
   */
  public void setMaximum (int maximum) {

    this.maximum = maximum;
  }

  /**
   * Returns a new {@code FileCountCleanupRule} with the same maximum, for use in a single vacuum pass.
   *
   * @return a fresh copy of this rule
   */
  @Override
  public FileCountCleanupRule copy () {

    return new FileCountCleanupRule(maximum);
  }

  /**
   * Adds the candidate path to the internal list for deferred evaluation; always returns {@code false}
   * so that no file is deleted during the iteration phase — deletion is deferred to {@link #finish()}.
   *
   * @param possiblePath a rolled log file to consider for cleanup
   * @return {@code false} unconditionally
   */
  @Override
  public boolean willCleanup (Path possiblePath) {

    pathList.add(possiblePath);

    return false;
  }

  /**
   * Sorts all collected paths by last modification time and deletes the oldest files
   * until the remaining count is at or below the configured maximum.
   *
   * @throws IOException if last-modified time cannot be read from a file or a deletion fails
   */
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

  /**
   * A {@link Comparator} that orders {@link Path} instances from oldest to newest by their
   * last-modified timestamp, caching each timestamp to avoid repeated filesystem calls.
   */
  private static class ModificationTimeComparator implements Comparator<Path> {

    private final HashMap<Path, Long> timeMap = new HashMap<>();
    private IOException ioException;

    /**
     * Returns the first {@link IOException} thrown while reading a file's last-modified time,
     * or {@code null} if all reads succeeded.
     *
     * @return the stored I/O exception, or {@code null}
     */
    public IOException getIoException () {

      return ioException;
    }

    /**
     * Compares two paths by last-modified time so that older files sort before newer ones.
     *
     * @param path1 the first path to compare
     * @param path2 the second path to compare
     * @return a negative value if {@code path1} is older, zero if equal, positive if newer
     */
    @Override
    public int compare (Path path1, Path path2) {

      return getModificationTime(path1).compareTo(getModificationTime(path2));
    }

    /**
     * Returns the last-modified time for {@code path} in milliseconds since epoch, reading it
     * from the filesystem on the first access and caching it for subsequent calls; stores any
     * {@link IOException} in {@link #ioException} and returns {@code 0L} on failure.
     *
     * @param path the path whose modification time is needed
     * @return the modification time in milliseconds, or {@code 0L} if the time could not be read
     */
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
