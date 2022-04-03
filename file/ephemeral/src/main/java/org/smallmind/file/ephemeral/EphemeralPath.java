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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;

public class EphemeralPath implements Path {

  private static final String SEPARATOR = "/";
  private final EphemeralFileSystem fileSystem;
  private final LinkedList<String> nameList = new LinkedList<>();
  private final boolean absolute;

  protected EphemeralPath (EphemeralFileSystem fileSystem) {

    this.fileSystem = fileSystem;

    absolute = true;
  }

  public EphemeralPath (EphemeralFileSystem fileSystem, String first, String... more) {

    this.fileSystem = fileSystem;

    if (first == null) {
      throw new NullPointerException();
    } else {
      absolute = first.startsWith(SEPARATOR);

      append(first, absolute);

      if ((more != null) && (more.length > 0)) {
        for (String another : more) {
          append(another, false);
        }
      }
    }
  }

  private EphemeralPath (EphemeralPath path, int begin, int end) {

    fileSystem = path.fileSystem;
    absolute = path.absolute;

    for (int index = begin; index < end; index++) {
      nameList.add(path.nameList.get(index));
    }
  }

  public static char getSeparatorChar () {

    return SEPARATOR.charAt(0);
  }

  public static String getSeparator () {

    return SEPARATOR;
  }

  private void append (String text, boolean absolute) {

    if (text.length() == 0) {
      throw new InvalidPathException(text, "Empty path component");
    } else {

      int index = 0;

      for (String segment : text.split(SEPARATOR, -1)) {
        if (segment.length() == 0) {
          if (absolute && (index > 0))
            throw new InvalidPathException(text, "Empty path component");
        } else {
          nameList.add(segment);
          index++;
        }
      }
    }
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

    return nameList.isEmpty() ? null : new EphemeralPath(fileSystem, nameList.getLast());
  }

  @Override
  public Path getParent () {

    return nameList.isEmpty() ? null : (nameList.size() > 1) ? new EphemeralPath(this, 0, nameList.size() - 1) : absolute ? new EphemeralPath(fileSystem) : null;
  }

  @Override
  public int getNameCount () {

    return nameList.size();
  }

  @Override
  public Path getName (int index) {

    if ((index < 0) || (index >= nameList.size())) {
      throw new IllegalArgumentException("Illegal index value");
    }

    return new EphemeralPath(fileSystem, nameList.get(index));
  }

  @Override
  public Path subpath (int beginIndex, int endIndex) {

    if ((beginIndex < 0) || (beginIndex >= nameList.size()) || (endIndex <= beginIndex) || (endIndex > nameList.size())) {
      throw new IllegalArgumentException("Illegal index value");
    } else {

      return new EphemeralPath(this, beginIndex, endIndex);
    }
  }

  @Override
  public boolean startsWith (Path other) {

    return false;
  }

  @Override
  public boolean endsWith (Path other) {

    return false;
  }

  @Override
  public Path normalize () {

    return null;
  }

  @Override
  public Path resolve (Path other) {

    return null;
  }

  @Override
  public Path relativize (Path other) {

    return null;
  }

  @Override
  public URI toUri () {

    return null;
  }

  @Override
  public Path toAbsolutePath () {

    return null;
  }

  @Override
  public Path toRealPath (LinkOption... options) throws IOException {

    return null;
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {

    return null;
  }

  @Override
  public int compareTo (Path other) {

    return 0;
  }
}
