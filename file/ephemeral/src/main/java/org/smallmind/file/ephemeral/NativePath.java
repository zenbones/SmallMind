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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Path adapter that delegates to a native platform path while reporting the ephemeral file system.
 */
public class NativePath implements Path {

  private final EphemeralFileSystem ephemeralFileSystem;
  private final Path nativePath;

  /**
   * @param ephemeralFileSystem the ephemeral file system to expose
   * @param nativePath          the underlying platform path to delegate to
   */
  public NativePath (EphemeralFileSystem ephemeralFileSystem, Path nativePath) {

    this.ephemeralFileSystem = ephemeralFileSystem;
    this.nativePath = nativePath;
  }

  /**
   * @return the native file system backing this path
   */
  public FileSystem getNativeFileSystem () {

    return nativePath.getFileSystem();
  }

  /**
   * @return the underlying platform path
   */
  public Path getNativePath () {

    return nativePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileSystem getFileSystem () {

    return ephemeralFileSystem;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAbsolute () {

    return nativePath.isAbsolute();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getRoot () {

    return new NativePath(ephemeralFileSystem, nativePath.getRoot());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getFileName () {

    return new NativePath(ephemeralFileSystem, nativePath.getFileName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getParent () {

    return new NativePath(ephemeralFileSystem, nativePath.getParent());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNameCount () {

    return nativePath.getNameCount();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getName (int index) {

    return new NativePath(ephemeralFileSystem, nativePath.getName(index));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path subpath (int beginIndex, int endIndex) {

    return new NativePath(ephemeralFileSystem, nativePath.subpath(beginIndex, endIndex));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean startsWith (Path other) {

    return nativePath.startsWith((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean endsWith (Path other) {

    return nativePath.endsWith((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path normalize () {

    return new NativePath(ephemeralFileSystem, nativePath.normalize());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path resolve (Path other) {

    return new NativePath(ephemeralFileSystem, nativePath.resolve((other instanceof NativePath) ? ((NativePath)other).nativePath : other));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path relativize (Path other) {

    return new NativePath(ephemeralFileSystem, nativePath.relativize((other instanceof NativePath) ? ((NativePath)other).nativePath : other));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URI toUri () {

    return nativePath.toUri();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path toAbsolutePath () {

    return new NativePath(ephemeralFileSystem, nativePath.toAbsolutePath());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path toRealPath (LinkOption... options)
    throws IOException {

    return new NativePath(ephemeralFileSystem, nativePath.toRealPath(options));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws IOException {

    return nativePath.register(watcher, events, modifiers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo (Path other) {

    return nativePath.compareTo((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString () {

    return nativePath.toString();
  }
}
