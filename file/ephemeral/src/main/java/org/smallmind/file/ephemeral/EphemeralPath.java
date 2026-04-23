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
package org.smallmind.file.ephemeral;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.LinkedList;
import org.smallmind.file.ephemeral.watch.EphemeralWatchKey;
import org.smallmind.file.ephemeral.watch.EphemeralWatchService;

/**
 * {@link Path} implementation representing a location within an {@link EphemeralFileSystem}.
 * Paths are stored as an ordered array of name components plus an {@code absolute} flag.
 * The separator character is {@code "/"}.
 *
 * <p>Three package-visible constructors exist for special cases (root path, subpath slices,
 * and pre-parsed component arrays). Public callers should use
 * {@link EphemeralFileSystem#getPath(String, String...)} to obtain instances.
 */
public class EphemeralPath implements Path {

  private static final String[] NO_NAMES = new String[0];
  private static final String SEPARATOR = "/";
  private final EphemeralFileSystem fileSystem;
  private final String[] names;
  private final boolean absolute;

  /**
   * Creates the root path (zero name components, absolute) for the given file system.
   *
   * @param fileSystem the owning {@link EphemeralFileSystem}; must not be {@code null}
   */
  protected EphemeralPath (EphemeralFileSystem fileSystem) {

    this.fileSystem = fileSystem;

    absolute = true;
    names = NO_NAMES;
  }

  /**
   * Builds a path by parsing and joining the provided string segments. The {@code first}
   * parameter determines whether the path is absolute (starts with {@code "/"}). Subsequent
   * elements in {@code more} are always treated as relative and appended in order.
   *
   * @param fileSystem the owning {@link EphemeralFileSystem}; must not be {@code null}
   * @param first      the first path string; must not be {@code null}
   * @param more       optional additional path strings to append
   * @throws NullPointerException if {@code first} is {@code null}
   * @throws InvalidPathException if any resulting path component is empty
   */
  public EphemeralPath (EphemeralFileSystem fileSystem, String first, String... more) {

    this.fileSystem = fileSystem;

    if (first == null) {
      throw new NullPointerException();
    } else {

      LinkedList<String> nameList = new LinkedList<>();

      absolute = first.startsWith(SEPARATOR);

      split(nameList, first, absolute);

      if (more != null) {
        for (String another : more) {
          split(nameList, another, false);
        }
      }

      names = nameList.toArray(new String[0]);
    }
  }

  /**
   * Creates a subpath view by copying a slice of the name array from another path.
   *
   * @param path  the source path
   * @param begin the start index (inclusive) of the slice
   * @param end   the end index (exclusive) of the slice
   */
  private EphemeralPath (EphemeralPath path, int begin, int end) {

    fileSystem = (EphemeralFileSystem)path.getFileSystem();
    absolute = path.isAbsolute();
    names = new String[end - begin];
    int to = 0;

    for (int from = begin; from < end; from++) {
      names[to++] = path.getNames()[from];
    }
  }

  /**
   * Constructs a new path from pre-parsed name components and an explicit absoluteness flag.
   *
   * @param fileSystem the owning {@link EphemeralFileSystem}; must not be {@code null}
   * @param names      the parsed name components; must not be {@code null}
   * @param absolute   {@code true} if the path is absolute
   */
  private EphemeralPath (EphemeralFileSystem fileSystem, String[] names, boolean absolute) {

    this.fileSystem = fileSystem;
    this.names = names;
    this.absolute = absolute;
  }

  /**
   * Returns the separator character used by ephemeral paths.
   *
   * @return the {@code '/'} character
   */
  public static char getSeparatorChar () {

    return SEPARATOR.charAt(0);
  }

  /**
   * Returns the separator string used by ephemeral paths.
   *
   * @return {@code "/"}
   */
  public static String getSeparator () {

    return SEPARATOR;
  }

  /**
   * Splits a raw path string on the separator and appends the resulting non-empty tokens to
   * {@code nameList}. Leading separator characters are permitted only when {@code absolute}
   * is {@code true} and {@code text} is the first segment.
   *
   * @param nameList the list to append tokens to
   * @param text     the raw text to split
   * @param absolute {@code true} when a leading separator is acceptable
   * @throws InvalidPathException if {@code text} is empty or yields an empty component in an
   *                              unexpected position
   */
  private void split (LinkedList<String> nameList, String text, boolean absolute) {

    if (text.isEmpty()) {
      throw new InvalidPathException(text, "Empty path component");
    } else {

      int index = 0;

      for (String segment : text.split(SEPARATOR, -1)) {
        if (segment.isEmpty()) {
          if ((!absolute) || (index > 0)) {
            throw new InvalidPathException(text, "Empty path component");
          }
        } else {
          nameList.add(segment);
          index++;
        }
      }
    }
  }

  /**
   * Returns the parsed name components of this path. The returned array is the internal
   * backing array and must not be modified by callers.
   *
   * @return the name component array; never {@code null}, may be empty for the root path
   */
  public String[] getNames () {

    return names;
  }

  /**
   * Returns the file system that created this path.
   *
   * @return the owning {@link EphemeralFileSystem}; never {@code null}
   */
  @Override
  public FileSystem getFileSystem () {

    return fileSystem;
  }

  /**
   * Indicates whether this path is absolute.
   *
   * @return {@code true} if the path was constructed from a string beginning with {@code "/"}
   */
  @Override
  public boolean isAbsolute () {

    return absolute;
  }

  /**
   * Returns the root component of this path, or {@code null} for relative paths.
   *
   * @return the root {@link EphemeralPath}, or {@code null} if this path is relative
   */
  @Override
  public EphemeralPath getRoot () {

    return absolute ? new EphemeralPath(fileSystem) : null;
  }

  /**
   * Returns the last name element of this path as a relative single-element path, or
   * {@code null} for the root path.
   *
   * @return the file name path element, or {@code null} when the path has no name components
   */
  @Override
  public EphemeralPath getFileName () {

    return (names.length == 0) ? null : new EphemeralPath(fileSystem, names[names.length - 1]);
  }

  /**
   * Returns the parent of this path, or {@code null} when no parent exists.
   *
   * @return the parent {@link EphemeralPath}, or {@code null}
   */
  @Override
  public EphemeralPath getParent () {

    return (names.length == 0) ? null : (names.length > 1) ? new EphemeralPath(this, 0, names.length - 1) : absolute ? new EphemeralPath(fileSystem) : null;
  }

  /**
   * Returns the number of name elements in this path.
   *
   * @return the element count; {@code 0} for the root path
   */
  @Override
  public int getNameCount () {

    return names.length;
  }

  /**
   * Returns the name element at the given position as a relative path.
   *
   * @param index the zero-based element index
   * @return the name element at {@code index}
   * @throws IllegalArgumentException if {@code index} is negative or {@code >= getNameCount()}
   */
  @Override
  public EphemeralPath getName (int index) {

    if ((index < 0) || (index >= names.length)) {
      throw new IllegalArgumentException("Illegal index value");
    }

    return new EphemeralPath(fileSystem, names[index]);
  }

  /**
   * Returns a relative sub-sequence of the name elements of this path.
   *
   * @param beginIndex the start index (inclusive)
   * @param endIndex   the end index (exclusive)
   * @return the sub-path
   * @throws IllegalArgumentException if the indices are out of range or inconsistent
   */
  @Override
  public EphemeralPath subpath (int beginIndex, int endIndex) {

    if ((beginIndex < 0) || (beginIndex >= names.length) || (endIndex <= beginIndex) || (endIndex > names.length)) {
      throw new IllegalArgumentException("Illegal index value");
    } else {

      return new EphemeralPath(this, beginIndex, endIndex);
    }
  }

  /**
   * Tests whether this path starts with the given path. Returns {@code true} only when both
   * paths belong to an {@link EphemeralFileSystem} and the other path's components form a
   * prefix of this path's components.
   *
   * @param other the candidate prefix path
   * @return {@code true} if this path starts with {@code other}
   */
  @Override
  public boolean startsWith (Path other) {

    if ((other.getFileSystem() instanceof EphemeralFileSystem) && (other.getNameCount() <= names.length)) {
      for (int index = 0; index < other.getNameCount(); index++) {
        if (!((EphemeralPath)other).getNames()[index].equals(names[index])) {

          return false;
        }
      }

      return true;
    }

    return false;
  }

  /**
   * Tests whether this path ends with the given path. Applies standard NIO rules: an absolute
   * {@code other} must match the full path; a relative {@code other} need only match the
   * trailing components.
   *
   * @param other the candidate suffix path
   * @return {@code true} if this path ends with {@code other}
   */
  @Override
  public boolean endsWith (Path other) {

    if (other.getFileSystem() instanceof EphemeralFileSystem) {
      if (absolute || (!other.isAbsolute())) {
        if ((other.isAbsolute() && (other.getNameCount() == names.length)) || ((!other.isAbsolute()) && (other.getNameCount() <= names.length))) {

          int offset = names.length - other.getNameCount();

          for (int index = 0; index < other.getNameCount(); index++) {
            if (!((EphemeralPath)other).getNames()[index].equals(names[offset + index])) {

              return false;
            }
          }

          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns a path with redundant {@code "."} and {@code ".."} components resolved. If no
   * such components are present {@code this} is returned unchanged.
   *
   * @return the normalized path
   */
  @Override
  public EphemeralPath normalize () {

    LinkedList<String> namesList = null;
    int position = 0;

    for (String name : names) {
      switch (name) {
        case ".":
          if (namesList == null) {
            namesList = new LinkedList<>(Arrays.asList(names).subList(0, position));
          }
          break;
        case "..":
          if (namesList == null) {
            namesList = (position > 1) ? new LinkedList<>(Arrays.asList(names).subList(0, position - 1)) : new LinkedList<>();
          } else if (!namesList.isEmpty()) {
            namesList.removeLast();
          }
          break;
        default:
          if (namesList == null) {
            position++;
          } else {
            namesList.add(name);
          }
      }
    }

    return (namesList == null) ? this : new EphemeralPath(fileSystem, namesList.toArray(new String[0]), absolute);
  }

  /**
   * Resolves the given path against this path. If {@code other} is absolute it is returned
   * unchanged. If {@code other} is empty this path is returned. Otherwise the name components
   * of {@code other} are appended to the components of this path.
   *
   * @param other the path to resolve
   * @return the resolved path
   */
  @Override
  public Path resolve (Path other) {

    if (other.isAbsolute()) {

      return other;
    } else if (other.getNameCount() == 0) {

      return this;
    } else {

      String[] resolvedNames = new String[names.length + other.getNameCount()];

      System.arraycopy(names, 0, resolvedNames, 0, names.length);
      for (int index = 0; index < other.getNameCount(); index++) {
        resolvedNames[names.length + index] = other.getName(index).toString();
      }

      return new EphemeralPath(fileSystem, resolvedNames, absolute);
    }
  }

  /**
   * Constructs a relative path between this path and the given path. The result is a sequence
   * of {@code ".."} steps to reach the common ancestor followed by the remaining components
   * of {@code other}.
   *
   * @param other the target path; must have the same absolute/relative status as this path
   * @return a relative path from this path to {@code other}
   * @throws IllegalArgumentException if this path and {@code other} differ in absoluteness
   */
  @Override
  public EphemeralPath relativize (Path other) {

    if (absolute != other.isAbsolute()) {
      throw new IllegalArgumentException("No relative path can be constructed");
    } else {

      LinkedList<String> namesList = new LinkedList<>();
      int index = 0;

      for (String name : names) {
        if (name.equals(other.getName(index).toString())) {
          index++;
        } else {
          break;
        }
      }

      if (index < names.length) {

        int redacted = names.length - index;

        for (int loop = 0; loop < redacted; loop++) {
          namesList.add("..");
        }
      }

      if (index < other.getNameCount()) {
        for (int loop = index; loop < other.getNameCount(); loop++) {
          namesList.add(other.getName(loop).toString());
        }
      }

      return new EphemeralPath(fileSystem, namesList.toArray(new String[0]), false);
    }
  }

  /**
   * Converts this path to a URI using the provider's scheme and the absolute path string.
   *
   * @return the URI representation of this path
   */
  @Override
  public URI toUri () {

    return URI.create(fileSystem.provider().getScheme() + "://" + toAbsolutePath());
  }

  /**
   * Returns an absolute form of this path. If already absolute, returns {@code this}.
   *
   * @return this path as an absolute path
   */
  @Override
  public EphemeralPath toAbsolutePath () {

    return absolute ? this : new EphemeralPath(fileSystem, names, true);
  }

  /**
   * Returns the real path by normalizing and making this path absolute. Symbolic link
   * resolution is not currently supported.
   *
   * @param options link options (currently unused)
   * @return the normalized absolute path
   */
  @Override
  public EphemeralPath toRealPath (LinkOption... options) {

    // TODO: will need to handle symlinks when/if the file system provide for them
    return normalize().toAbsolutePath();
  }

  /**
   * Registers this path with the given watch service for the specified event kinds.
   *
   * @param watcher   the watch service; must be an {@link EphemeralWatchService}
   * @param events    the kinds of events to watch for
   * @param modifiers optional modifiers (currently unused)
   * @return the {@link WatchKey} representing the registration
   * @throws NoSuchFileException      if this path does not exist in the heap
   * @throws NotDirectoryException    if this path is not a directory
   * @throws IllegalArgumentException if {@code watcher} is not an {@link EphemeralWatchService}
   */
  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws NoSuchFileException, NotDirectoryException {

    if (!(watcher instanceof EphemeralWatchService)) {
      throw new IllegalArgumentException("The watcher is not associated with this file system");
    } else {

      EphemeralWatchKey watchKey;

      // modifiers unused as yet
      ((EphemeralWatchService)watcher).register(watchKey = new EphemeralWatchKey((EphemeralWatchService)watcher, events, this));

      return watchKey;
    }
  }

  /**
   * Compares this path lexicographically with another {@link EphemeralPath}, first by name
   * component count and then component-by-component.
   *
   * @param other the other path to compare to
   * @return a negative integer, zero, or positive integer as this path is less than, equal
   * to, or greater than {@code other}
   */
  @Override
  public int compareTo (Path other) {

    EphemeralPath otherEphemeralPath = (EphemeralPath)other;

    if (names.length == other.getNameCount()) {
      for (int index = 0; index < names.length; index++) {

        int comparison;

        if ((comparison = names[index].compareTo(otherEphemeralPath.getNames()[index])) != 0) {

          return comparison;
        }
      }

      return 0;
    } else {

      return names.length - otherEphemeralPath.getNameCount();
    }
  }

  /**
   * Returns a hash code based on the name component array and the absoluteness flag.
   *
   * @return a hash code value for this path
   */
  @Override
  public int hashCode () {

    return (Arrays.hashCode(names) * 31) + (absolute ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
  }

  /**
   * Compares this path with another object for equality. Two {@link EphemeralPath} instances
   * are equal when both have the same absoluteness flag and the same sequence of name
   * components.
   *
   * @param obj the object to compare
   * @return {@code true} if {@code obj} is an {@link EphemeralPath} with identical components
   * and absoluteness
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof EphemeralPath) && (((EphemeralPath)obj).isAbsolute() == absolute) && Arrays.equals(((EphemeralPath)obj).getNames(), names);
  }

  /**
   * Returns the string form of this path. Absolute paths begin with the separator; name
   * components are joined by the separator.
   *
   * @return the path string; never {@code null}
   */
  @Override
  public String toString () {

    StringBuilder pathBuilder = new StringBuilder();

    for (String name : names) {
      if (absolute || (!pathBuilder.isEmpty())) {
        pathBuilder.append(SEPARATOR);
      }
      pathBuilder.append(name);
    }

    return pathBuilder.toString();
  }
}
