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
 * {@link Path} adapter that wraps a native platform path while reporting an
 * {@link EphemeralFileSystem} as its owning file system. All structural operations
 * (navigation, comparison, string conversion) are delegated to the underlying native path.
 * When a {@link NativePath} is encountered by {@link EphemeralFileSystemProvider} the
 * operation is forwarded directly to the native provider, bypassing the ephemeral heap.
 */
public class NativePath implements Path {

  private final EphemeralFileSystem ephemeralFileSystem;
  private final Path nativePath;

  /**
   * Wraps a native path so that it appears to belong to the given ephemeral file system.
   *
   * @param ephemeralFileSystem the ephemeral file system to expose via
   *                            {@link #getFileSystem()}; must not be {@code null}
   * @param nativePath          the underlying platform path to delegate all operations to;
   *                            must not be {@code null}
   */
  public NativePath (EphemeralFileSystem ephemeralFileSystem, Path nativePath) {

    this.ephemeralFileSystem = ephemeralFileSystem;
    this.nativePath = nativePath;
  }

  /**
   * Returns the native file system that backs the underlying path.
   *
   * @return the native {@link FileSystem}; never {@code null}
   */
  public FileSystem getNativeFileSystem () {

    return nativePath.getFileSystem();
  }

  /**
   * Returns the underlying platform {@link Path} that this adapter wraps.
   *
   * @return the native path; never {@code null}
   */
  public Path getNativePath () {

    return nativePath;
  }

  /**
   * Returns the ephemeral file system associated with this path.
   *
   * @return the {@link EphemeralFileSystem} supplied at construction time; never {@code null}
   */
  @Override
  public FileSystem getFileSystem () {

    return ephemeralFileSystem;
  }

  /**
   * Delegates to the native path.
   *
   * @return {@code true} if the native path is absolute
   */
  @Override
  public boolean isAbsolute () {

    return nativePath.isAbsolute();
  }

  /**
   * Returns the root component of the native path wrapped in a {@link NativePath}.
   *
   * @return the root path adapter
   */
  @Override
  public Path getRoot () {

    return new NativePath(ephemeralFileSystem, nativePath.getRoot());
  }

  /**
   * Returns the file name component of the native path wrapped in a {@link NativePath}.
   *
   * @return the file name path adapter
   */
  @Override
  public Path getFileName () {

    return new NativePath(ephemeralFileSystem, nativePath.getFileName());
  }

  /**
   * Returns the parent of the native path wrapped in a {@link NativePath}.
   *
   * @return the parent path adapter
   */
  @Override
  public Path getParent () {

    return new NativePath(ephemeralFileSystem, nativePath.getParent());
  }

  /**
   * Delegates to the native path.
   *
   * @return the number of name elements in the native path
   */
  @Override
  public int getNameCount () {

    return nativePath.getNameCount();
  }

  /**
   * Returns the name element at the given index from the native path, wrapped in a
   * {@link NativePath}.
   *
   * @param index the zero-based element index
   * @return the name element adapter
   */
  @Override
  public Path getName (int index) {

    return new NativePath(ephemeralFileSystem, nativePath.getName(index));
  }

  /**
   * Returns a sub-sequence of name elements from the native path, wrapped in a
   * {@link NativePath}.
   *
   * @param beginIndex the start index (inclusive)
   * @param endIndex   the end index (exclusive)
   * @return the sub-path adapter
   */
  @Override
  public Path subpath (int beginIndex, int endIndex) {

    return new NativePath(ephemeralFileSystem, nativePath.subpath(beginIndex, endIndex));
  }

  /**
   * Tests whether this path starts with {@code other}. If {@code other} is a
   * {@link NativePath} its underlying native path is used for the comparison.
   *
   * @param other the candidate prefix path
   * @return {@code true} if this path starts with {@code other}
   */
  @Override
  public boolean startsWith (Path other) {

    return nativePath.startsWith((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  /**
   * Tests whether this path ends with {@code other}. If {@code other} is a
   * {@link NativePath} its underlying native path is used for the comparison.
   *
   * @param other the candidate suffix path
   * @return {@code true} if this path ends with {@code other}
   */
  @Override
  public boolean endsWith (Path other) {

    return nativePath.endsWith((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  /**
   * Returns the normalized form of the native path, wrapped in a {@link NativePath}.
   *
   * @return the normalized path adapter
   */
  @Override
  public Path normalize () {

    return new NativePath(ephemeralFileSystem, nativePath.normalize());
  }

  /**
   * Resolves {@code other} against the native path. If {@code other} is a {@link NativePath}
   * its underlying native path is used; the result is wrapped in a new {@link NativePath}.
   *
   * @param other the path to resolve
   * @return the resolved path adapter
   */
  @Override
  public Path resolve (Path other) {

    return new NativePath(ephemeralFileSystem, nativePath.resolve((other instanceof NativePath) ? ((NativePath)other).nativePath : other));
  }

  /**
   * Constructs a relative path between this path and {@code other} using native path
   * semantics. The result is wrapped in a new {@link NativePath}.
   *
   * @param other the target path
   * @return the relative path adapter
   */
  @Override
  public Path relativize (Path other) {

    return new NativePath(ephemeralFileSystem, nativePath.relativize((other instanceof NativePath) ? ((NativePath)other).nativePath : other));
  }

  /**
   * Delegates to the native path's {@link Path#toUri()} implementation.
   *
   * @return the URI of the native path
   */
  @Override
  public URI toUri () {

    return nativePath.toUri();
  }

  /**
   * Returns the absolute form of the native path, wrapped in a {@link NativePath}.
   *
   * @return the absolute path adapter
   */
  @Override
  public Path toAbsolutePath () {

    return new NativePath(ephemeralFileSystem, nativePath.toAbsolutePath());
  }

  /**
   * Resolves the real (canonical) form of the native path, wrapped in a {@link NativePath}.
   *
   * @param options link options passed through to the native path
   * @return the real path adapter
   * @throws IOException if the native path cannot be resolved
   */
  @Override
  public Path toRealPath (LinkOption... options)
    throws IOException {

    return new NativePath(ephemeralFileSystem, nativePath.toRealPath(options));
  }

  /**
   * Registers the native path with the given watch service for the specified events.
   * Delegates entirely to the native path.
   *
   * @param watcher   the watch service to register with
   * @param events    the event kinds to watch for
   * @param modifiers optional modifiers
   * @return the resulting {@link WatchKey}
   * @throws IOException if registration fails
   */
  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws IOException {

    return nativePath.register(watcher, events, modifiers);
  }

  /**
   * Compares this path to {@code other} using native path ordering. If {@code other} is a
   * {@link NativePath} its underlying native path is used for the comparison.
   *
   * @param other the path to compare to
   * @return a negative integer, zero, or positive integer per the native path contract
   */
  @Override
  public int compareTo (Path other) {

    return nativePath.compareTo((other instanceof NativePath) ? ((NativePath)other).nativePath : other);
  }

  /**
   * Returns the string representation of the native path.
   *
   * @return the native path string; never {@code null}
   */
  @Override
  public String toString () {

    return nativePath.toString();
  }
}
