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
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 -Djava.nio.file.spi.DefaultFileSystemProvider=org.smallmind.file.ephemeral.EphemeralFileSystemProvider

  -Dorg.smallmind.file.ephemeral.configuration.capacity=<Long.MAX_VALUE>
  -Dorg.smallmind.file.ephemeral.configuration.blockSize=<1024>
  -Dorg.smallmind.file.ephemeral.configuration.roots=/overlay/file/system,/any/other/path
*/

public class EphemeralFileSystemProvider extends FileSystemProvider {

  private static final CountDownLatch INITIALIZATION_LATCH = new CountDownLatch(1);
  private final EphemeralFileSystem ephemeralFileSystem;
  private final String scheme;
  private FileSystem nativeFileSystem;

  public EphemeralFileSystemProvider () {

    this("ephemeral");
  }

  public EphemeralFileSystemProvider (FileSystemProvider fileSystemProvider) {

    this(fileSystemProvider.getScheme());

    nativeFileSystem = fileSystemProvider.getFileSystem(URI.create(fileSystemProvider.getScheme() + ":///"));
  }

  public EphemeralFileSystemProvider (String scheme) {

    this.scheme = scheme;

    ephemeralFileSystem = new EphemeralFileSystem(this, new EphemeralFileSystemConfiguration());
    INITIALIZATION_LATCH.countDown();
  }

  public static void waitForInitialization (long timeout, TimeUnit unit)
    throws InterruptedException, TimeoutException {

    if (!INITIALIZATION_LATCH.await(timeout, unit)) {
      throw new TimeoutException();
    }
  }

  public boolean isDefault () {

    return "file".equals(scheme);
  }

  @Override
  public String getScheme () {

    return scheme;
  }

  public FileSystem getNativeFileSystem () {

    return nativeFileSystem;
  }

  @Override
  public FileSystem newFileSystem (URI uri, Map<String, ?> env) {

    EphemeralURIUtility.checkUri(scheme, uri);
    throw new FileSystemAlreadyExistsException();
  }

  @Override
  public FileSystem getFileSystem (URI uri) {

    EphemeralURIUtility.checkUri(scheme, uri);

    return ephemeralFileSystem;
  }

  @Override
  public Path getPath (URI uri) {

    return EphemeralURIUtility.fromUri(ephemeralFileSystem, uri);
  }

  @Override
  public synchronized SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
        try {
          internalCheckAccess(normalizedPath, AccessMode.WRITE);
        } catch (NoSuchFileException noSuchFileException) {
          if (normalizedPath.getNames().length > 0) {
            internalCheckAccess(normalizedPath.getParent(), AccessMode.WRITE);
          }
        }
      } else {
        internalCheckAccess(normalizedPath, AccessMode.READ);
      }

      return ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().newByteChannel(normalizedPath, options, attrs);
    }
  }

  @Override
  public synchronized DirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter)
    throws NoSuchFileException, NotDirectoryException {

    return newDirectoryStream(dir, filter, new LinkOption[0]);
  }

  public synchronized SecureDirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter, LinkOption... options)
    throws NoSuchFileException, NotDirectoryException {

    if (!EphemeralFileSystem.class.isAssignableFrom(dir.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + dir + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedDir = (EphemeralPath)dir.normalize();

      internalCheckAccess(normalizedDir, AccessMode.READ);

      return ((EphemeralFileSystem)normalizedDir.getFileSystem()).getFileStore().newDirectoryStream(normalizedDir, filter);
    }
  }

  @Override
  public synchronized void createDirectory (Path dir, FileAttribute<?>... attrs)
    throws NoSuchFileException, FileAlreadyExistsException {

    if (!EphemeralFileSystem.class.isAssignableFrom(dir.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + dir + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedDir = (EphemeralPath)dir.normalize();

      if (normalizedDir.getNames().length > 0) {
        internalCheckAccess(normalizedDir.getParent(), AccessMode.WRITE);
      }

      ((EphemeralFileSystem)normalizedDir.getFileSystem()).getFileStore().createDirectory(normalizedDir, attrs);
    }
  }

  @Override
  public synchronized void delete (Path path)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.WRITE);

      ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().delete(normalizedPath);
    }
  }

  @Override
  public synchronized void copy (Path source, Path target, CopyOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedSource = (EphemeralPath)source.normalize();
      EphemeralPath normalizedTarget = (EphemeralPath)target.normalize();

      internalCheckAccess(normalizedSource, AccessMode.READ);
      internalCheckAccess(normalizedTarget, AccessMode.WRITE);

      ((EphemeralFileSystem)normalizedSource.getFileSystem()).getFileStore().copy(normalizedSource, normalizedTarget, options);
    }
  }

  @Override
  public synchronized void move (Path source, Path target, CopyOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedSource = (EphemeralPath)source.normalize();
      EphemeralPath normalizedTarget = (EphemeralPath)target.normalize();

      internalCheckAccess(normalizedSource, AccessMode.READ);
      internalCheckAccess(normalizedTarget, AccessMode.WRITE);

      ((EphemeralFileSystem)source.getFileSystem()).getFileStore().move(normalizedSource, normalizedTarget, options);
    }
  }

  @Override
  public synchronized boolean isSameFile (Path source, Path target)
    throws NoSuchFileException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedSource = (EphemeralPath)source.normalize();
      EphemeralPath normalizedTarget = (EphemeralPath)target.normalize();

      internalCheckAccess(normalizedSource, AccessMode.READ);
      internalCheckAccess(normalizedTarget, AccessMode.READ);

      return normalizedSource.equals(normalizedTarget);
    }
  }

  @Override
  public boolean isHidden (Path path) {

    return false;
  }

  @Override
  public FileStore getFileStore (Path path) {

    return path.getFileSystem().getFileStores().iterator().next();
  }

  @Override
  public synchronized void checkAccess (Path path, AccessMode... modes)
    throws NoSuchFileException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, modes);
    }
  }

  private void internalCheckAccess (EphemeralPath path, AccessMode... modes)
    throws NoSuchFileException {

    ((EphemeralFileSystem)path.getFileSystem()).getFileStore().checkAccess(path);
  }

  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      try {
        internalCheckAccess(normalizedPath, AccessMode.READ);

        return ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().getFileAttributeView(normalizedPath, type, options);
      } catch (NoSuchFileException noSuchFileException) {

        return null;
      }
    }
  }

  @Override
  public synchronized <A extends BasicFileAttributes> A readAttributes (Path path, Class<A> type, LinkOption... options)
    throws NoSuchFileException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.READ);

      return ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().readAttributes(normalizedPath, type, options);
    }
  }

  @Override
  public synchronized Map<String, Object> readAttributes (Path path, String attributes, LinkOption... options)
    throws NoSuchFileException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.READ);

      return ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().readAttributes(normalizedPath, attributes, options);
    }
  }

  @Override
  public synchronized void setAttribute (Path path, String attribute, Object value, LinkOption... options)
    throws NoSuchFileException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.WRITE);

      ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().setAttribute(normalizedPath, attribute, value, options);
    }
  }

  public FileChannel newFileChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().newFileChannel(((NativePath)path).getNativePath(), options, attrs);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public AsynchronousFileChannel newAsynchronousFileChannel (Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs)
    throws IOException {

    if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().newAsynchronousFileChannel(((NativePath)path).getNativePath(), options, executor, attrs);
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
