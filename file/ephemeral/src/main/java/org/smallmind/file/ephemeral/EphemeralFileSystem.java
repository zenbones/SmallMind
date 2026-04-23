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
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Set;
import java.util.regex.Pattern;
import org.smallmind.file.ephemeral.watch.EphemeralWatchService;
import org.smallmind.nutsnbolts.util.SingleItemIterable;

/**
 * {@link FileSystem} implementation backed entirely by heap memory. Paths that belong to one
 * of the configured root prefixes are served from the in-memory {@link EphemeralFileStore};
 * all other paths are transparently delegated to the native file system wrapped by the provider.
 * The file system may be closed only when it was not installed as the JVM default provider
 * (i.e., when {@link EphemeralFileSystemProvider#isDefault()} returns {@code false}).
 */
public class EphemeralFileSystem extends FileSystem {

  private static final EphemeralUserPrincipalLookupService USER_PRINCIPAL_LOOKUP_SERVICE = new EphemeralUserPrincipalLookupService();
  private final EphemeralFileSystemProvider provider;
  private final EphemeralFileSystemConfiguration configuration;
  private final EphemeralFileStore fileStore;
  private final EphemeralPath rootPath;
  private volatile boolean closed;

  /**
   * Constructs an ephemeral file system using the given provider and configuration.
   *
   * @param provider      the {@link EphemeralFileSystemProvider} that owns this file system
   * @param configuration the configuration describing capacity, block size, and root prefixes
   */
  public EphemeralFileSystem (EphemeralFileSystemProvider provider, EphemeralFileSystemConfiguration configuration) {

    this.provider = provider;
    this.configuration = configuration;

    fileStore = new EphemeralFileStore(this, configuration.getCapacity(), configuration.getBlockSize());
    rootPath = new EphemeralPath(this);
  }

  /**
   * Removes all entries from the underlying file store, returning it to an empty state.
   */
  public void clear () {

    fileStore.clear();
  }

  /**
   * Returns the provider that created this file system.
   *
   * @return the owning {@link EphemeralFileSystemProvider}; never {@code null}
   */
  @Override
  public EphemeralFileSystemProvider provider () {

    return provider;
  }

  /**
   * Closes this file system. This method has no effect if the file system is already closed
   * or if it is installed as the JVM default provider. Closing the file system marks it as
   * closed but does not yet close open channels or streams.
   */
  @Override
  public synchronized void close () {

    if ((!closed) && (!provider.isDefault())) {
      //TODO: Closing a file system will close all open channels, directory-streams, watch-service, and other closeable objects associated with this file system
      closed = true;
    }
  }

  /**
   * Indicates whether this file system is open.
   *
   * @return {@code true} if the file system has not been closed
   */
  @Override
  public synchronized boolean isOpen () {

    return !closed;
  }

  /**
   * Indicates whether this file system is read-only.
   *
   * @return always {@code false}
   */
  @Override
  public boolean isReadOnly () {

    return false;
  }

  /**
   * Returns the name separator string used by paths in this file system.
   *
   * @return the separator string ({@code "/"})
   */
  @Override
  public String getSeparator () {

    return EphemeralPath.getSeparator();
  }

  /**
   * Returns an iterable containing the single root path of this file system.
   *
   * @return an {@link Iterable} yielding the root {@link EphemeralPath}
   */
  @Override
  public Iterable<Path> getRootDirectories () {

    return rootPath;
  }

  /**
   * Returns the underlying ephemeral file store.
   *
   * @return the {@link EphemeralFileStore}; never {@code null}
   * @throws ClosedFileSystemException if this file system has been closed
   */
  public EphemeralFileStore getFileStore () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return fileStore;
    }
  }

  /**
   * Returns an iterable over the file stores associated with this file system. There is
   * exactly one store.
   *
   * @return an {@link Iterable} containing the single {@link EphemeralFileStore}
   * @throws ClosedFileSystemException if this file system has been closed
   */
  @Override
  public Iterable<FileStore> getFileStores () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return new SingleItemIterable<>(fileStore);
    }
  }

  /**
   * Returns the set of attribute view names supported by this file system.
   *
   * @return an unmodifiable set of supported view name strings
   * @throws ClosedFileSystemException if this file system has been closed
   */
  @Override
  public Set<String> supportedFileAttributeViews () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return fileStore.getSupportedFileAttributeViewNames();
    }
  }

  /**
   * Converts a path string, and optional additional strings, into a {@link Path}. Paths that
   * match one of the configured roots are returned as {@link EphemeralPath} instances; all
   * other paths are wrapped in a {@link NativePath} that delegates to the native file system.
   *
   * @param first the initial path string
   * @param more  additional strings to be joined to the path
   * @return the resulting {@link Path}
   * @throws ClosedFileSystemException if this file system has been closed
   */
  @Override
  public Path getPath (String first, String... more) {

    if (closed) {
      throw new ClosedFileSystemException();
    } else if (configuration.isOurs(first, more)) {

      return new EphemeralPath(this, first, more);
    } else {

      return new NativePath(this, provider.getNativeFileSystem().getPath(first, more));
    }
  }

  /**
   * Converts a URI into a {@link Path}. URIs whose string representation matches a configured
   * root are returned as {@link EphemeralPath} instances; others are wrapped as
   * {@link NativePath}.
   *
   * @param uri the URI to convert
   * @return the resulting {@link Path}
   * @throws ClosedFileSystemException if this file system has been closed
   */
  public Path getPath (URI uri) {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      String uriAsString = uri.toString();

      if (configuration.isOurs(uriAsString)) {

        return new EphemeralPath(this, uriAsString);
      } else {

        return new NativePath(this, provider.getNativeFileSystem().provider().getPath(uri));
      }
    }
  }

  /**
   * Returns a {@link PathMatcher} for the given syntax-and-pattern string. The supported
   * syntaxes are {@code "glob"} (converted via {@link Glob#toRegexPattern}) and
   * {@code "regex"}.
   *
   * @param syntaxAndPattern a string of the form {@code "syntax:pattern"}
   * @return a {@link PathMatcher} backed by the compiled pattern
   * @throws IllegalArgumentException      if the string does not contain a colon separator
   * @throws UnsupportedOperationException if the syntax is not {@code "glob"} or {@code "regex"}
   * @throws ClosedFileSystemException     if this file system has been closed
   */
  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      int colonPos;

      if ((colonPos = syntaxAndPattern.indexOf(':')) < 0) {
        throw new IllegalArgumentException(syntaxAndPattern);
      } else {

        String syntax;

        return switch (syntax = syntaxAndPattern.substring(0, colonPos)) {
          case "glob" -> new RegexPathMatcher(Glob.toRegexPattern(EphemeralPath.getSeparatorChar(), syntaxAndPattern.substring(colonPos + 1)));
          case "regex" -> new RegexPathMatcher(Pattern.compile(syntaxAndPattern.substring(colonPos + 1)));
          default -> throw new UnsupportedOperationException(syntax);
        };
      }
    }
  }

  /**
   * Returns the {@link UserPrincipalLookupService} associated with this file system.
   *
   * @return the shared {@link EphemeralUserPrincipalLookupService}; never {@code null}
   * @throws ClosedFileSystemException if this file system has been closed
   */
  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return USER_PRINCIPAL_LOOKUP_SERVICE;
    }
  }

  /**
   * Creates a new {@link WatchService} for this file system.
   *
   * @return a new {@link EphemeralWatchService} bound to the underlying file store
   * @throws ClosedFileSystemException if this file system has been closed
   */
  @Override
  public WatchService newWatchService () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return new EphemeralWatchService(fileStore);
    }
  }
}
