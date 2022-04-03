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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class EphemeralPath implements Path {

  private static final char SEPARATOR = '/';
  private final EphemeralFileSystem fileSystem;

  public EphemeralPath (EphemeralFileSystem fileSystem, String first, String... more) {

    this.fileSystem = fileSystem;
  }

  public static char getSeparator () {

    return SEPARATOR;
  }

  @Override
  public FileSystem getFileSystem () {

    return fileSystem;
  }

  @Override
  public boolean isAbsolute () {

    return false;
  }

  @Override
  public Path getRoot () {

    return null;
  }

  @Override
  public Path getFileName () {

    return null;
  }

  @Override
  public Path getParent () {

    return null;
  }

  @Override
  public int getNameCount () {

    return 0;
  }

  @Override
  public Path getName (int index) {

    return null;
  }

  @Override
  public Path subpath (int beginIndex, int endIndex) {

    return null;
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
