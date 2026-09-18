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

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.LinkedList;
import org.smallmind.file.ephemeral.watch.EphemeralWatchService;

/**
 * {@link Path} implementation for the in-memory {@link EphemeralFileSystem}.
 *
 * <p>A path is held as an array of simple name elements plus a flag recording whether it is
 * absolute. Redundant and trailing separators are discarded during parsing, so {@code /a//b/} and
 * {@code /a/b} produce identical paths, matching the behaviour of the platform's own providers.
 *
 * <p>Two name arrays carry special meaning:
 * <ul>
 *   <li>an <em>absolute</em> path with zero names is the root, and renders as {@code "/"}</li>
 *   <li>a <em>relative</em> path with a single empty name is the empty path, and renders as
 *       {@code ""} with a {@linkplain #getNameCount() name count} of one</li>
 * </ul>
 * The empty path exists because {@code java.nio.file} requires it: {@code Files.createTempFile}
 * and other JDK utilities build a single-element relative path and inspect its parent.
 *
 * <p>Relative paths are resolved against the file system's
 * {@linkplain EphemeralFileSystem#getWorkingDirectory() working directory}.
 */
public class EphemeralPath implements Path {

  private static final String[] NO_NAMES = new String[0];
  private static final String[] EMPTY_NAMES = new String[] {""};
  private static final String SEPARATOR = "/";

  /**
   * The one character a path element may never contain.
   */
  private static final char NUL_CHARACTER = 0;

  private final EphemeralFileSystem fileSystem;
  private final String[] names;
  private final boolean absolute;

  /**
   * Creates the root path of the given file system.
   *
   * @param fileSystem the owning file system
   */
  protected EphemeralPath (EphemeralFileSystem fileSystem) {

    this.fileSystem = fileSystem;

    absolute = true;
    names = NO_NAMES;
  }

  /**
   * Parses a path from a first component and any number of additional components.
   *
   * <p>Empty and repeated separators are ignored. If nothing but separators is supplied and the
   * first component is not absolute, the result is the empty path.
   *
   * @param fileSystem the owning file system
   * @param first      the first path component; must not be {@code null}
   * @param more       additional components to be joined
   * @throws NullPointerException if {@code first} is {@code null}
   * @throws InvalidPathException if any component contains the NUL character
   */
  public EphemeralPath (EphemeralFileSystem fileSystem, String first, String... more) {

    this.fileSystem = fileSystem;

    if (first == null) {
      throw new NullPointerException();
    } else {

      LinkedList<String> nameList = new LinkedList<>();

      absolute = first.startsWith(SEPARATOR);

      split(nameList, first);
      if (more != null) {
        for (String another : more) {
          split(nameList, another);
        }
      }

      names = nameList.isEmpty() ? (absolute ? NO_NAMES : EMPTY_NAMES) : nameList.toArray(new String[0]);
    }
  }

  private EphemeralPath (EphemeralPath path, int begin, int end, boolean absolute) {

    fileSystem = (EphemeralFileSystem)path.getFileSystem();

    this.absolute = absolute;

    names = new String[end - begin];

    int to = 0;

    for (int from = begin; from < end; from++) {
      names[to++] = path.getNames()[from];
    }
  }

  /**
   * Creates a path directly from its name elements, without parsing.
   *
   * <p>Visible within the package so that {@link EphemeralFileStore} can build the prefix of a path
   * while resolving symbolic links. The array is retained rather than copied, so callers must not
   * modify it afterwards.
   *
   * @param fileSystem the owning file system
   * @param names      the name elements of the path
   * @param absolute   whether the path is absolute
   */
  EphemeralPath (EphemeralFileSystem fileSystem, String[] names, boolean absolute) {

    this.fileSystem = fileSystem;
    this.names = names;
    this.absolute = absolute;
  }

  /**
   * Returns the separator character used by this file system.
   *
   * @return the separator character, {@code '/'}
   */
  public static char getSeparatorChar () {

    return SEPARATOR.charAt(0);
  }

  /**
   * Returns the separator used by this file system.
   *
   * @return the separator, {@code "/"}
   */
  public static String getSeparator () {

    return SEPARATOR;
  }

  /**
   * Returns the name elements of an arbitrary path, accepting both {@link EphemeralPath} and
   * {@link NativePath} instances.
   *
   * @param path the path to read
   * @return the effective name array of {@code path}
   */
  private static String[] namesOf (Path path) {

    if (path instanceof EphemeralPath) {

      return ((EphemeralPath)path).getEffectiveNames();
    } else {

      String[] extracted = new String[path.getNameCount()];

      for (int index = 0; index < extracted.length; index++) {
        extracted[index] = path.getName(index).toString();
      }

      return ((extracted.length == 1) && extracted[0].isEmpty()) ? NO_NAMES : extracted;
    }
  }

  /**
   * Appends the non-empty segments of {@code text} to the accumulating name list.
   *
   * @param nameList the list accumulating name elements
   * @param text     the component to split
   * @throws InvalidPathException if the component contains the NUL character
   */
  private void split (LinkedList<String> nameList, String text) {

    if (text.indexOf(NUL_CHARACTER) >= 0) {
      throw new InvalidPathException(text, "NUL character not allowed");
    }

    for (String segment : text.split(SEPARATOR)) {
      if (!segment.isEmpty()) {
        nameList.add(segment);
      }
    }
  }

  /**
   * Returns the name elements backing this path.
   *
   * @return the internal name array, which callers must not modify
   */
  public String[] getNames () {

    return names;
  }

  /**
   * Returns whether this path is the empty path, a relative path consisting of one empty name.
   *
   * @return {@code true} if this is the empty path
   */
  private boolean isEmptyPath () {

    return (!absolute) && (names.length == 1) && names[0].isEmpty();
  }

  /**
   * Returns the name elements of this path as they participate in {@link #relativize(Path)},
   * where the empty path behaves as though it had no name elements at all.
   *
   * @return the effective name array
   */
  private String[] getEffectiveNames () {

    return isEmptyPath() ? NO_NAMES : names;
  }

  @Override
  public FileSystem getFileSystem () {

    return fileSystem;
  }

  @Override
  public boolean isAbsolute () {

    return absolute;
  }

  @Override
  public EphemeralPath getRoot () {

    return absolute ? new EphemeralPath(fileSystem) : null;
  }

  @Override
  public EphemeralPath getFileName () {

    return (names.length == 0) ? null : new EphemeralPath(fileSystem, new String[] {names[names.length - 1]}, false);
  }

  @Override
  public EphemeralPath getParent () {

    return (names.length == 0) ? null : (names.length > 1) ? new EphemeralPath(this, 0, names.length - 1, absolute) : absolute ? new EphemeralPath(fileSystem) : null;
  }

  @Override
  public int getNameCount () {

    return names.length;
  }

  @Override
  public EphemeralPath getName (int index) {

    if ((index < 0) || (index >= names.length)) {
      throw new IllegalArgumentException("Illegal index value");
    }

    return new EphemeralPath(fileSystem, new String[] {names[index]}, false);
  }

  @Override
  public EphemeralPath subpath (int beginIndex, int endIndex) {

    if ((beginIndex < 0) || (beginIndex >= names.length) || (endIndex <= beginIndex) || (endIndex > names.length)) {
      throw new IllegalArgumentException("Illegal index value");
    } else {

      return new EphemeralPath(this, beginIndex, endIndex, false);
    }
  }

  @Override
  public boolean startsWith (Path other) {

    if ((!(other instanceof EphemeralPath)) || (!fileSystem.equals(other.getFileSystem())) || (absolute != other.isAbsolute())) {

      return false;
    } else {

      String[] otherNames = ((EphemeralPath)other).getNames();

      if (otherNames.length > names.length) {

        return false;
      } else {
        for (int index = 0; index < otherNames.length; index++) {
          if (!otherNames[index].equals(names[index])) {

            return false;
          }
        }

        return true;
      }
    }
  }

  @Override
  public boolean endsWith (Path other) {

    if ((!(other instanceof EphemeralPath)) || (!fileSystem.equals(other.getFileSystem()))) {

      return false;
    } else {

      String[] otherNames = ((EphemeralPath)other).getNames();
      int offset;

      if (other.isAbsolute()) {
        if ((!absolute) || (otherNames.length != names.length)) {

          return false;
        }
        offset = 0;
      } else if (otherNames.length > names.length) {

        return false;
      } else {
        offset = names.length - otherNames.length;
      }

      for (int index = 0; index < otherNames.length; index++) {
        if (!otherNames[index].equals(names[offset + index])) {

          return false;
        }
      }

      return true;
    }
  }

  @Override
  public EphemeralPath normalize () {

    if (isEmptyPath()) {

      return this;
    } else {

      LinkedList<String> normalizedList = new LinkedList<>();
      boolean modified = false;

      for (String name : names) {
        switch (name) {
          case ".":
            modified = true;
            break;
          case "..":
            modified = true;
            if ((!normalizedList.isEmpty()) && (!"..".equals(normalizedList.getLast()))) {
              normalizedList.removeLast();
            } else if (!absolute) {
              // a relative path retains leading parent references, an absolute path cannot ascend past its root
              normalizedList.add(name);
            }
            break;
          default:
            normalizedList.add(name);
        }
      }

      if (!modified) {

        return this;
      } else if (normalizedList.isEmpty()) {

        return absolute ? new EphemeralPath(fileSystem) : new EphemeralPath(fileSystem, EMPTY_NAMES, false);
      } else {

        return new EphemeralPath(fileSystem, normalizedList.toArray(new String[0]), absolute);
      }
    }
  }

  @Override
  public Path resolve (Path other) {

    String[] otherNames;

    if (other.isAbsolute()) {

      return other;
    } else if ((otherNames = namesOf(other)).length == 0) {

      return this;
    } else if (isEmptyPath()) {

      return new EphemeralPath(fileSystem, otherNames, false);
    } else {

      String[] resolvedNames = new String[names.length + otherNames.length];

      System.arraycopy(names, 0, resolvedNames, 0, names.length);
      System.arraycopy(otherNames, 0, resolvedNames, names.length, otherNames.length);

      return new EphemeralPath(fileSystem, resolvedNames, absolute);
    }
  }

  @Override
  public EphemeralPath relativize (Path other) {

    if (absolute != other.isAbsolute()) {
      throw new IllegalArgumentException("No relative path can be constructed");
    } else {

      String[] sourceNames = getEffectiveNames();
      String[] targetNames = namesOf(other);
      LinkedList<String> namesList = new LinkedList<>();
      int common = 0;
      int limit = Math.min(sourceNames.length, targetNames.length);

      while ((common < limit) && sourceNames[common].equals(targetNames[common])) {
        common++;
      }

      for (int index = common; index < sourceNames.length; index++) {
        namesList.add("..");
      }
      namesList.addAll(Arrays.asList(targetNames).subList(common, targetNames.length));

      return namesList.isEmpty() ? new EphemeralPath(fileSystem, EMPTY_NAMES, false) : new EphemeralPath(fileSystem, namesList.toArray(new String[0]), false);
    }
  }

  @Override
  public URI toUri () {

    try {

      return new URI(fileSystem.provider().getScheme(), "", toAbsolutePath().toString(), null);
    } catch (URISyntaxException uriSyntaxException) {
      throw new IOError(uriSyntaxException);
    }
  }

  @Override
  public EphemeralPath toAbsolutePath () {

    if (absolute) {

      return this;
    } else {

      EphemeralPath workingDirectory = fileSystem.getWorkingDirectory();

      return isEmptyPath() ? workingDirectory : (EphemeralPath)workingDirectory.resolve(this);
    }
  }

  /**
   * Returns the absolute, normalized, link-free form of this path.
   *
   * @param options {@link LinkOption#NOFOLLOW_LINKS} to leave a final symbolic link unresolved
   * @return the real path of the existing file
   * @throws IOException if the file does not exist, or if a symbolic link cycle is encountered
   */
  @Override
  public EphemeralPath toRealPath (LinkOption... options)
    throws IOException {

    return fileSystem.getFileStore().toRealPath(toAbsolutePath().normalize(), options);
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws IOException {

    if (!(watcher instanceof EphemeralWatchService)) {
      throw new IllegalArgumentException("The watcher is not associated with this file system");
    } else {
      for (WatchEvent.Kind<?> event : events) {
        if (!(StandardWatchEventKinds.ENTRY_CREATE.equals(event) || StandardWatchEventKinds.ENTRY_DELETE.equals(event) || StandardWatchEventKinds.ENTRY_MODIFY.equals(event) || StandardWatchEventKinds.OVERFLOW.equals(event))) {
          throw new UnsupportedOperationException(event.name());
        }
      }

      return ((EphemeralWatchService)watcher).register(toAbsolutePath().normalize(), events);
    }
  }

  @Override
  public int compareTo (Path other) {

    if (!fileSystem.equals(other.getFileSystem())) {
      throw new ClassCastException("The path(" + other + ") is not associated with this file system");
    }

    return toString().compareTo(other.toString());
  }

  @Override
  public int hashCode () {

    return (Arrays.hashCode(names) * 31) + (absolute ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
  }

  @Override
  public boolean equals (Object obj) {

    return (this == obj) || ((obj instanceof EphemeralPath) && fileSystem.equals(((EphemeralPath)obj).getFileSystem()) && (((EphemeralPath)obj).isAbsolute() == absolute) && Arrays.equals(((EphemeralPath)obj).getNames(), names));
  }

  @Override
  public String toString () {

    if (absolute && (names.length == 0)) {

      return SEPARATOR;
    } else {

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
}
