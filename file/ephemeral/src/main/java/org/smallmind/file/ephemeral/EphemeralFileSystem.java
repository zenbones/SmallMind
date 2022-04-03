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

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import java.util.regex.Pattern;
import org.smallmind.nutsnbolts.util.SingleItemIterable;

public class EphemeralFileSystem extends FileSystem {

  private static final EphemeralUserPrincipalLookupService USER_PRINCIPAL_LOOKUP_SERVICE = new EphemeralUserPrincipalLookupService();
  private final EphemeralPath rootPath;
  private final EphemeralFileStore fileStore;
  private final EphemeralFileSystemProvider provider;
  private volatile boolean closed;

  public EphemeralFileSystem (EphemeralFileSystemProvider provider) {

    this.provider = provider;

    fileStore = new EphemeralFileStore(0, 0);
    rootPath = new EphemeralPath(this);
  }

  @Override
  public FileSystemProvider provider () {

    return provider;
  }

  @Override
  public void close () {

    closed = true;
  }

  @Override
  public boolean isOpen () {

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

    return fileStore;
  }

  @Override
  public Iterable<FileStore> getFileStores () {

    return new SingleItemIterable<>(fileStore);
  }

  @Override
  public Set<String> supportedFileAttributeViews () {

    return fileStore.getSupportedFileAttributeViewNames();
  }

  @Override
  public Path getPath (String first, String... more) {

    return new EphemeralPath(this, first, more);
  }

  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    int colonPos;

    if ((colonPos = syntaxAndPattern.indexOf(':')) < 0) {
      throw new IllegalArgumentException(syntaxAndPattern);
    } else {

      String syntax;

      switch (syntax = syntaxAndPattern.substring(0, colonPos)) {
        case "glob":

          return new RegexPathMatcher(Glob.toRegexPattern(EphemeralPath.getSeparatorChar(), syntaxAndPattern.substring(colonPos + 1)));
        case "regex":

          return new RegexPathMatcher(Pattern.compile(syntaxAndPattern.substring(colonPos + 1)));
        default:
          throw new UnsupportedOperationException(syntax);
      }
    }
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    return USER_PRINCIPAL_LOOKUP_SERVICE;
  }

  @Override
  public WatchService newWatchService () {

    return new EphemeralWatchService();
  }
}
