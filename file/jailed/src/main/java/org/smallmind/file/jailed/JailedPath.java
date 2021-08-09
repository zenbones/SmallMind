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
package org.smallmind.file.jailed;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class JailedPath implements Path {

  private final JailedFileSystem jailedFileSystem;
  private final Path nativePath;

  public JailedPath (JailedFileSystem jailedFileSystem, Path nativePath) {

    this.jailedFileSystem = jailedFileSystem;
    this.nativePath = nativePath;
  }

  public Path getNativePath () {

    return nativePath;
  }

  @Override
  public FileSystem getFileSystem () {

    return jailedFileSystem;
  }

  @Override
  public boolean isAbsolute () {

    return nativePath.isAbsolute();
  }

  @Override
  public Path getRoot () {

    return new JailedPath(jailedFileSystem, nativePath.getRoot());
  }

  @Override
  public Path getFileName () {

    return new JailedPath(jailedFileSystem, nativePath.getFileName());
  }

  @Override
  public Path getParent () {

    return new JailedPath(jailedFileSystem, nativePath.getParent());
  }

  @Override
  public int getNameCount () {

    return nativePath.getNameCount();
  }

  @Override
  public Path getName (int index) {

    return new JailedPath(jailedFileSystem, nativePath.getName(index));
  }

  @Override
  public Path subpath (int beginIndex, int endIndex) {

    return new JailedPath(jailedFileSystem, nativePath.subpath(beginIndex, endIndex));
  }

  @Override
  public boolean startsWith (Path other) {

    return nativePath.startsWith(JailedPathUtility.unwrap(other));
  }

  @Override
  public boolean endsWith (Path other) {

    return nativePath.equals(JailedPathUtility.unwrap(other));
  }

  @Override
  public Path normalize () {

    return new JailedPath(jailedFileSystem, nativePath.normalize());
  }

  @Override
  public Path resolve (Path other) {

    return new JailedPath(jailedFileSystem, nativePath.resolve(JailedPathUtility.unwrap(other)));
  }

  @Override
  public Path relativize (Path other) {

    return new JailedPath(jailedFileSystem, nativePath.relativize(JailedPathUtility.unwrap(other)));
  }

  @Override
  public URI toUri () {

    URI nativeURI = nativePath.toUri();

    try {
      return new URI(jailedFileSystem.provider().getScheme(), nativeURI.getUserInfo(), nativeURI.getHost(), nativeURI.getPort(), new JailedPath(jailedFileSystem, nativePath.toAbsolutePath()).toString(), null, null);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public Path toAbsolutePath () {

    return new JailedPath(jailedFileSystem, nativePath.toAbsolutePath());
  }

  @Override
  public Path toRealPath (LinkOption... options)
    throws IOException {

    return new JailedPath(jailedFileSystem, nativePath.toRealPath(options));
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws IOException {

    return nativePath.register(watcher, events, modifiers);
  }

  @Override
  public int compareTo (Path other) {

    return nativePath.compareTo(JailedPathUtility.unwrap(other));
  }
}
