/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class NativePath implements Path {

  private final EphemeralFileSystem ephemeralFileSystem;
  private final Path nativePath;

  public NativePath (EphemeralFileSystem ephemeralFileSystem, Path nativePath) {

    this.ephemeralFileSystem = ephemeralFileSystem;
    this.nativePath = nativePath;
  }

  public FileSystem getNativeFileSystem () {

    return nativePath.getFileSystem();
  }

  public Path getNativePath () {

    return nativePath;
  }

  @Override
  public FileSystem getFileSystem () {

    return ephemeralFileSystem;
  }

  @Override
  public boolean isAbsolute () {

    return nativePath.isAbsolute();
  }

  @Override
  public Path getRoot () {

    return new NativePath(ephemeralFileSystem, nativePath.getRoot());
  }

  @Override
  public Path getFileName () {

    return new NativePath(ephemeralFileSystem, nativePath.getFileName());
  }

  @Override
  public Path getParent () {

    return new NativePath(ephemeralFileSystem, nativePath.getParent());
  }

  @Override
  public int getNameCount () {

    return nativePath.getNameCount();
  }

  @Override
  public Path getName (int index) {

    return new NativePath(ephemeralFileSystem, nativePath.getName(index));
  }

  @Override
  public Path subpath (int beginIndex, int endIndex) {

    return new NativePath(ephemeralFileSystem, nativePath.subpath(beginIndex, endIndex));
  }

  @Override
  public boolean startsWith (Path other) {

    return nativePath.startsWith((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  @Override
  public boolean endsWith (Path other) {

    return nativePath.endsWith((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  @Override
  public Path normalize () {

    return new NativePath(ephemeralFileSystem, nativePath.normalize());
  }

  @Override
  public Path resolve (Path other) {

    return new NativePath(ephemeralFileSystem, nativePath.resolve((other instanceof NativePath) ? ((NativePath)other).nativePath : other));
  }

  @Override
  public Path relativize (Path other) {

    return new NativePath(ephemeralFileSystem, nativePath.relativize((other instanceof NativePath) ? ((NativePath)other).nativePath : other));
  }

  @Override
  public URI toUri () {

    return nativePath.toUri();
  }

  @Override
  public Path toAbsolutePath () {

    return new NativePath(ephemeralFileSystem, nativePath.toAbsolutePath());
  }

  @Override
  public Path toRealPath (LinkOption... options)
    throws IOException {

    return new NativePath(ephemeralFileSystem, nativePath.toRealPath(options));
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws IOException {

    return nativePath.register(watcher, events, modifiers);
  }

  @Override
  public int compareTo (Path other) {

    return nativePath.compareTo((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  @Override
  public String toString () {

    return nativePath.toString();
  }
}
