/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.LinkedList;
import org.smallmind.file.ephemeral.watch.EphemeralWatchKey;
import org.smallmind.file.ephemeral.watch.EphemeralWatchService;

public class EphemeralPath implements Path {

  private static final String[] NO_NAMES = new String[0];
  private static final String SEPARATOR = "/";
  private final EphemeralFileSystem fileSystem;
  private final String[] names;
  private final boolean absolute;

  protected EphemeralPath (EphemeralFileSystem fileSystem) {

    this.fileSystem = fileSystem;

    absolute = true;
    names = NO_NAMES;
  }

  public EphemeralPath (EphemeralFileSystem fileSystem, String first, String... more) {

    this.fileSystem = fileSystem;

    if (first == null) {
      throw new NullPointerException();
    } else {

      LinkedList<String> nameList = new LinkedList<>();

      absolute = first.startsWith(SEPARATOR);

      split(nameList, first, absolute);

      if ((more != null) && (more.length > 0)) {
        for (String another : more) {
          split(nameList, another, false);
        }
      }

      names = nameList.toArray(new String[0]);
    }
  }

  private EphemeralPath (EphemeralPath path, int begin, int end) {

    fileSystem = (EphemeralFileSystem)path.getFileSystem();
    absolute = path.isAbsolute();
    names = new String[end - begin];
    int to = 0;

    for (int from = begin; from < end; from++) {
      names[to++] = path.getNames()[from];
    }
  }

  private EphemeralPath (EphemeralFileSystem fileSystem, String[] names, boolean absolute) {

    this.fileSystem = fileSystem;
    this.names = names;
    this.absolute = absolute;
  }

  public static char getSeparatorChar () {

    return SEPARATOR.charAt(0);
  }

  public static String getSeparator () {

    return SEPARATOR;
  }

  private void split (LinkedList<String> nameList, String text, boolean absolute) {

    if (text.length() == 0) {
      throw new InvalidPathException(text, "Empty path component");
    } else {

      int index = 0;

      for (String segment : text.split(SEPARATOR, -1)) {
        if (segment.length() == 0) {
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

  public String[] getNames () {

    return names;
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
  public Path getRoot () {

    return absolute ? new EphemeralPath(fileSystem) : null;
  }

  @Override
  public Path getFileName () {

    return (names.length == 0) ? null : new EphemeralPath(fileSystem, names[names.length - 1]);
  }

  @Override
  public Path getParent () {

    return (names.length == 0) ? null : (names.length > 1) ? new EphemeralPath(this, 0, names.length - 1) : absolute ? new EphemeralPath(fileSystem) : null;
  }

  @Override
  public int getNameCount () {

    return names.length;
  }

  @Override
  public Path getName (int index) {

    if ((index < 0) || (index >= names.length)) {
      throw new IllegalArgumentException("Illegal index value");
    }

    return new EphemeralPath(fileSystem, names[index]);
  }

  @Override
  public Path subpath (int beginIndex, int endIndex) {

    if ((beginIndex < 0) || (beginIndex >= names.length) || (endIndex <= beginIndex) || (endIndex > names.length)) {
      throw new IllegalArgumentException("Illegal index value");
    } else {

      return new EphemeralPath(this, beginIndex, endIndex);
    }
  }

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

  @Override
  public Path normalize () {

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

  @Override
  public Path relativize (Path other) {

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

  @Override
  public URI toUri () {

    return URI.create(fileSystem.provider().getScheme() + "://" + toAbsolutePath());
  }

  @Override
  public Path toAbsolutePath () {

    return absolute ? this : new EphemeralPath(fileSystem, names, true);
  }

  @Override
  public Path toRealPath (LinkOption... options) {

    // TODO: will need to handle symlinks when/if the file system provide for them
    return normalize().toAbsolutePath();
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws NotDirectoryException {

    if (!(watcher instanceof EphemeralWatchService)) {
      throw new IllegalArgumentException("The watcher is not associated with this file system");
    } else {

      EphemeralWatchKey watchKey;

      ((EphemeralWatchService)watcher).register(watchKey = new EphemeralWatchKey((EphemeralWatchService)watcher, events, modifiers, this));

      return watchKey;
    }
  }

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

  @Override
  public int hashCode () {

    return (Arrays.hashCode(names) * 31) + (absolute ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof EphemeralPath) && (((EphemeralPath)obj).isAbsolute() == absolute) && Arrays.equals(((EphemeralPath)obj).getNames(), names);
  }

  @Override
  public String toString () {

    StringBuilder pathBuilder = new StringBuilder();

    for (String name : names) {
      if (absolute || (pathBuilder.length() > 0)) {
        pathBuilder.append(SEPARATOR);
      }
      pathBuilder.append(name);
    }

    return pathBuilder.toString();
  }
}
