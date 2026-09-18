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
package org.smallmind.file.jailed;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A {@link FileSystem} implementation that presents a jailed, chroot-like view of an
 * underlying native file system.
 *
 * <p>All paths created by this file system are {@link JailedPath} instances whose segments
 * are strictly confined to the subtree defined by the {@link JailedPathTranslator} held by
 * the owning {@link JailedFileSystemProvider}. The path separator is always {@code '/'}.
 *
 * <p>Everything a caller can observe through this file system is expressed in terms of the jail
 * rather than the host: {@link #getRootDirectories()} reports the single jail root,
 * {@link #getFileStores()} reports only the store that holds the jail, and
 * {@link #getPathMatcher(String)} matches jailed paths with jailed separator and case semantics,
 * so a pattern means the same thing whether the jail is backed by Windows or by Linux.
 *
 * <p>Lifecycle and metadata queries that have no jailed equivalent ({@link #isOpen()},
 * {@link #isReadOnly()}, {@link #supportedFileAttributeViews()},
 * {@link #getUserPrincipalLookupService()}) are delegated to the backing native file system
 * obtained via the translator.
 *
 * @see JailedFileSystemProvider
 * @see JailedPathTranslator
 */
public class JailedFileSystem extends FileSystem {

  /**
   * The string representation of the jailed path separator character.
   */
  private static final String SEPARATOR = Character.toString(JailedPath.SEPARATOR);

  /**
   * The name of the glob syntax accepted by {@link #getPathMatcher(String)}.
   */
  private static final String GLOB_SYNTAX = "glob";

  /**
   * The name of the regular expression syntax accepted by {@link #getPathMatcher(String)}.
   */
  private static final String REGEX_SYNTAX = "regex";

  /**
   * The provider that created and manages this file system instance.
   */
  private final JailedFileSystemProvider jailedFileSystemProvider;

  /**
   * The root path of this jail, always {@code "/"}.
   */
  private final JailedPath rootPath;

  /**
   * Constructs a new jailed file system owned by the given provider.
   *
   * <p>A root {@link JailedPath} ({@code "/"}) is created eagerly and returned by
   * {@link #getRootDirectories()}.
   *
   * @param jailedFileSystemProvider the {@link JailedFileSystemProvider} that owns this
   *                                 file system; must not be {@code null}
   */
  public JailedFileSystem (JailedFileSystemProvider jailedFileSystemProvider) {

    this.jailedFileSystemProvider = jailedFileSystemProvider;

    rootPath = new JailedPath(this, getSeparator().toCharArray(), true);
  }

  /**
   * Returns the provider that created this file system.
   *
   * @return the {@link JailedFileSystemProvider} that owns this instance
   */
  public FileSystemProvider provider () {

    return jailedFileSystemProvider;
  }

  /**
   * Returns the translator that maps between this jail and the backing native file system.
   *
   * @return the {@link JailedPathTranslator} held by the owning provider; never {@code null}
   */
  public JailedPathTranslator getJailedPathTranslator () {

    return jailedFileSystemProvider.getJailedPathTranslator();
  }

  /**
   * Returns the native file system that backs this jail.
   *
   * @return the native {@link FileSystem} obtained from the translator
   */
  private FileSystem getNativeFileSystem () {

    return getJailedPathTranslator().getNativeFileSystem();
  }

  /**
   * Closes this file system.
   *
   * <p>This implementation is a no-op because the jailed file system does not own the
   * underlying native file system; its lifecycle is managed externally, and closing it here
   * would close it for every other holder as well.
   */
  @Override
  public void close () {

  }

  /**
   * Delegates to the backing native file system to determine whether it is still open.
   *
   * @return {@code true} if the backing native file system is open
   */
  @Override
  public boolean isOpen () {

    return getNativeFileSystem().isOpen();
  }

  /**
   * Delegates to the backing native file system to determine read-only status.
   *
   * @return {@code true} if the backing native file system is read-only
   */
  @Override
  public boolean isReadOnly () {

    return getNativeFileSystem().isReadOnly();
  }

  /**
   * Returns the name separator for this file system, which is always {@code "/"}.
   *
   * @return the string {@code "/"}
   */
  @Override
  public String getSeparator () {

    return SEPARATOR;
  }

  /**
   * Returns an iterable containing only the single jail root path ({@code "/"}).
   *
   * @return an {@link Iterable} containing the root {@link JailedPath}
   */
  @Override
  public Iterable<Path> getRootDirectories () {

    return List.of(rootPath);
  }

  /**
   * Returns the file store that holds the jail.
   *
   * <p>Only the store containing the jail root is reported, because the remaining stores of the
   * host are not reachable from inside the jail and their names would disclose its layout. As
   * {@link FileSystem#getFileStores()} permits, a store that can not be accessed is omitted, so
   * the result is empty when the jail root can not be resolved.
   *
   * @return an {@link Iterable} containing at most the one {@link FileStore} that holds the jail
   */
  @Override
  public Iterable<FileStore> getFileStores () {

    try {

      return List.of(jailedFileSystemProvider.getFileStore(rootPath));
    } catch (IOException | SecurityException exception) {

      return List.of();
    }
  }

  /**
   * Returns the set of supported file attribute view names from the backing native file system.
   *
   * @return the set of supported file attribute view name strings
   */
  @Override
  public Set<String> supportedFileAttributeViews () {

    return getNativeFileSystem().supportedFileAttributeViews();
  }

  /**
   * Constructs a {@link JailedPath} by joining {@code first} and the optional {@code more}
   * components with the jail separator.
   *
   * <p>Empty components contribute nothing, so that joining never introduces an empty name
   * element.
   *
   * @param first the first path component
   * @param more  optional additional path components to append
   * @return a new {@link JailedPath} for the combined path string
   */
  @Override
  public Path getPath (String first, String... more) {

    if ((more == null) || (more.length == 0)) {

      return new JailedPath(this, first);
    } else {

      StringBuilder pathBuilder = new StringBuilder(first);

      for (String component : more) {
        if ((component != null) && (!component.isEmpty())) {
          if (!pathBuilder.isEmpty()) {
            pathBuilder.append(JailedPath.SEPARATOR);
          }

          pathBuilder.append(component);
        }
      }

      return new JailedPath(this, pathBuilder.toString());
    }
  }

  /**
   * Returns a {@link PathMatcher} for the given syntax-and-pattern string.
   *
   * <p>The {@code "glob"} and {@code "regex"} syntaxes are supported, and both are interpreted
   * in jail space - the separator is the forward slash and matching is case-sensitive -
   * regardless of the separator and case semantics of the backing native file system. Matching
   * is performed against the string form of the path.
   *
   * @param syntaxAndPattern a string of the form {@code "<syntax>:<pattern>"}
   * @return a {@link PathMatcher} that matches jailed paths against the given pattern
   * @throws IllegalArgumentException               if the parameter does not take the required form
   * @throws UnsupportedOperationException          if the syntax is not {@code "glob"} or {@code "regex"}
   * @throws java.util.regex.PatternSyntaxException if the pattern is invalid
   */
  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    int colonPos;

    if ((colonPos = syntaxAndPattern.indexOf(':')) < 1) {
      throw new IllegalArgumentException("The parameter must be of the form '<syntax>:<pattern>'");
    } else {

      String syntax = syntaxAndPattern.substring(0, colonPos);
      String pattern = syntaxAndPattern.substring(colonPos + 1);

      if (GLOB_SYNTAX.equalsIgnoreCase(syntax)) {

        return new JailedPathMatcher(JailedGlob.toRegexPattern(pattern));
      } else if (REGEX_SYNTAX.equalsIgnoreCase(syntax)) {

        return new JailedPathMatcher(Pattern.compile(pattern));
      } else {
        throw new UnsupportedOperationException("Unsupported syntax(=" + syntax + ")");
      }
    }
  }

  /**
   * Returns the {@link UserPrincipalLookupService} of the backing native file system.
   *
   * @return the user/group lookup service
   * @throws UnsupportedOperationException if the backing file system does not support
   *                                       user/group lookups
   */
  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    return getNativeFileSystem().getUserPrincipalLookupService();
  }

  /**
   * Creates a new {@link WatchService} that watches jailed paths.
   *
   * <p>The returned service wraps the watch service of the backing native file system, so that
   * registering a jailed path translates and confines it exactly as any other jailed operation
   * would, and the keys and events handed back are expressed in jail space.
   *
   * @return a new {@link JailedWatchService}
   * @throws IOException                   if an I/O error occurs creating the watch service
   * @throws UnsupportedOperationException if the backing file system does not support
   *                                       watching file-tree changes
   */
  @Override
  public WatchService newWatchService ()
    throws IOException {

    return new JailedWatchService(this, getNativeFileSystem().newWatchService());
  }
}
