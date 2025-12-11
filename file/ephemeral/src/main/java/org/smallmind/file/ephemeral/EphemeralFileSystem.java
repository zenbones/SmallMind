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

public class EphemeralFileSystem extends FileSystem {

  private static final EphemeralUserPrincipalLookupService USER_PRINCIPAL_LOOKUP_SERVICE = new EphemeralUserPrincipalLookupService();
  private final EphemeralFileSystemProvider provider;
  private final EphemeralFileSystemConfiguration configuration;
  private final EphemeralFileStore fileStore;
  private final EphemeralPath rootPath;
  private volatile boolean closed;

  public EphemeralFileSystem (EphemeralFileSystemProvider provider, EphemeralFileSystemConfiguration configuration) {

    this.provider = provider;
    this.configuration = configuration;

    fileStore = new EphemeralFileStore(this, configuration.getCapacity(), configuration.getBlockSize());
    rootPath = new EphemeralPath(this);
  }

  public void clear () {

    fileStore.clear();
  }

  @Override
  public EphemeralFileSystemProvider provider () {

    return provider;
  }

  @Override
  public synchronized void close () {

    if ((!closed) && (!provider.isDefault())) {
      //TODO: Closing a file system will close all open channels, directory-streams, watch-service, and other closeable objects associated with this file system
      closed = true;
    }
  }

  @Override
  public synchronized boolean isOpen () {

    return !closed;
  }

  @Override
  public boolean isReadOnly () {

    return false;
  }

  @Override
  public String getSeparator () {

    return EphemeralPath.getSeparator();
  }

  @Override
  public Iterable<Path> getRootDirectories () {

    return rootPath;
  }

  public EphemeralFileStore getFileStore () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return fileStore;
    }
  }

  @Override
  public Iterable<FileStore> getFileStores () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return new SingleItemIterable<>(fileStore);
    }
  }

  @Override
  public Set<String> supportedFileAttributeViews () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return fileStore.getSupportedFileAttributeViewNames();
    }
  }

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

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return USER_PRINCIPAL_LOOKUP_SERVICE;
    }
  }

  @Override
  public WatchService newWatchService () {

    if (closed) {
      throw new ClosedFileSystemException();
    } else {

      return new EphemeralWatchService(fileStore);
    }
  }
}
