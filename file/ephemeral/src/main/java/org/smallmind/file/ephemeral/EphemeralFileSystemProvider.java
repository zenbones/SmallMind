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
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
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

/**
 * {@link FileSystemProvider} for the ephemeral in-memory file system with optional delegation to a native provider.
 */
public class EphemeralFileSystemProvider extends FileSystemProvider {

  private static final CountDownLatch INITIALIZATION_LATCH = new CountDownLatch(1);
  private final EphemeralFileSystem ephemeralFileSystem;
  private final String scheme;
  private FileSystem nativeFileSystem;

  /**
   * Builds a provider using the default {@code "ephemeral"} scheme.
   */
  public EphemeralFileSystemProvider () {

    this("ephemeral");
  }

  /**
   * Builds a provider that mirrors the scheme of another provider and delegates native access to it.
   *
   * @param fileSystemProvider the provider to mirror
   */
  public EphemeralFileSystemProvider (FileSystemProvider fileSystemProvider) {

    this(fileSystemProvider.getScheme());

    nativeFileSystem = fileSystemProvider.getFileSystem(URI.create(fileSystemProvider.getScheme() + ":///"));
  }

  /**
   * Builds a provider using the supplied scheme.
   *
   * @param scheme the URI scheme to expose
   */
  public EphemeralFileSystemProvider (String scheme) {

    this.scheme = scheme;

    ephemeralFileSystem = new EphemeralFileSystem(this, new EphemeralFileSystemConfiguration());
    INITIALIZATION_LATCH.countDown();
  }

  /**
   * Waits for the provider to finish initializing.
   *
   * @param timeout duration to wait
   * @param unit    time unit of the timeout
   * @throws InterruptedException if interrupted while waiting
   * @throws TimeoutException     if initialization does not complete in time
   */
  public static void waitForInitialization (long timeout, TimeUnit unit)
    throws InterruptedException, TimeoutException {

    if (!INITIALIZATION_LATCH.await(timeout, unit)) {
      throw new TimeoutException();
    }
  }

  /**
   * @return {@code true} when this provider is masquerading as the default {@code file} scheme
   */
  public boolean isDefault () {

    return "file".equals(scheme);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getScheme () {

    return scheme;
  }

  /**
   * @return the delegated native file system, when configured
   */
  public FileSystem getNativeFileSystem () {

    return nativeFileSystem;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileSystem newFileSystem (URI uri, Map<String, ?> env) {

    EphemeralURIUtility.checkUri(scheme, uri);
    throw new FileSystemAlreadyExistsException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileSystem getFileSystem (URI uri) {

    EphemeralURIUtility.checkUri(scheme, uri);

    return ephemeralFileSystem;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getPath (URI uri) {

    return EphemeralURIUtility.fromUri(ephemeralFileSystem, uri);
  }

  /**
   * {@inheritDoc}
   * Delegates to native paths when the supplied path is a {@link NativePath}; otherwise routes to the ephemeral store.
   */
  @Override
  public synchronized SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().newByteChannel(((NativePath)path).getNativePath(), options, attrs);
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

  /**
   * {@inheritDoc}
   * Delegates to native paths when appropriate.
   */
  @Override
  public synchronized DirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(dir.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + dir + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (dir instanceof NativePath) {

      return ((NativePath)dir).getNativeFileSystem().provider().newDirectoryStream(((NativePath)dir).getNativePath(), filter);
    } else {

      EphemeralPath normalizedDir = (EphemeralPath)dir.normalize();

      internalCheckAccess(normalizedDir, AccessMode.READ);

      return ((EphemeralFileSystem)normalizedDir.getFileSystem()).getFileStore().newDirectoryStream(normalizedDir, filter);
    }
  }

  /**
   * Opens a secure directory stream within the ephemeral store.
   *
   * @param dir     the directory to open
   * @param filter  optional filter
   * @param options link options (unused)
   * @return the secure directory stream
   * @throws NoSuchFileException   if the directory does not exist
   * @throws NotDirectoryException if the path is not a directory
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void createDirectory (Path dir, FileAttribute<?>... attrs)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(dir.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + dir + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (dir instanceof NativePath) {

      ((NativePath)dir).getNativeFileSystem().provider().createDirectory(((NativePath)dir).getNativePath(), attrs);
    } else {

      EphemeralPath normalizedDir = (EphemeralPath)dir.normalize();

      if (normalizedDir.getNames().length > 0) {
        internalCheckAccess(normalizedDir.getParent(), AccessMode.WRITE);
      }

      ((EphemeralFileSystem)normalizedDir.getFileSystem()).getFileStore().createDirectory(normalizedDir, attrs);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void delete (Path path)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      ((NativePath)path).getNativeFileSystem().provider().delete(((NativePath)path).getNativePath());
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.WRITE);

      ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().delete(normalizedPath);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void copy (Path source, Path target, CopyOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!source.getFileSystem().equals(target.getFileSystem())) {
      throw new ProviderMismatchException("The source and target are associated with different file systems");
    } else if (source instanceof NativePath) {

      ((NativePath)source).getNativeFileSystem().provider().copy(((NativePath)source).getNativePath(), ((NativePath)target).getNativePath(), options);
    } else {

      EphemeralPath normalizedSource = (EphemeralPath)source.normalize();
      EphemeralPath normalizedTarget = (EphemeralPath)target.normalize();

      internalCheckAccess(normalizedSource, AccessMode.READ);
      if (exists(normalizedTarget)) {
        internalCheckAccess(normalizedTarget, AccessMode.WRITE);
      } else {
        internalCheckAccess(normalizedTarget.getParent(), AccessMode.WRITE);
      }

      ((EphemeralFileSystem)normalizedSource.getFileSystem()).getFileStore().copy(normalizedSource, normalizedTarget, options);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void move (Path source, Path target, CopyOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!source.getFileSystem().equals(target.getFileSystem())) {
      throw new ProviderMismatchException("The source and target are associated with different file systems");
    } else if (source instanceof NativePath) {

      ((NativePath)source).getNativeFileSystem().provider().move(((NativePath)source).getNativePath(), ((NativePath)target).getNativePath(), options);
    } else {

      EphemeralPath normalizedSource = (EphemeralPath)source.normalize();
      EphemeralPath normalizedTarget = (EphemeralPath)target.normalize();

      internalCheckAccess(normalizedSource, AccessMode.READ);
      internalCheckAccess(normalizedTarget, AccessMode.WRITE);

      ((EphemeralFileSystem)source.getFileSystem()).getFileStore().move(normalizedSource, normalizedTarget, options);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized boolean isSameFile (Path source, Path target)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!source.getFileSystem().equals(target.getFileSystem())) {

      return false;
    } else if (source instanceof NativePath) {

      return ((NativePath)source).getNativeFileSystem().provider().isSameFile(((NativePath)source).getNativePath(), ((NativePath)target).getNativePath());
    } else {

      EphemeralPath normalizedSource = (EphemeralPath)source.normalize();
      EphemeralPath normalizedTarget = (EphemeralPath)target.normalize();

      internalCheckAccess(normalizedSource, AccessMode.READ);
      internalCheckAccess(normalizedTarget, AccessMode.READ);

      return normalizedSource.equals(normalizedTarget);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isHidden (Path path) {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileStore getFileStore (Path path) {

    return path.getFileSystem().getFileStores().iterator().next();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void checkAccess (Path path, AccessMode... modes)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      ((NativePath)path).getNativeFileSystem().provider().checkAccess(((NativePath)path).getNativePath(), modes);
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, modes);
    }
  }

  /**
   * Ensures the supplied path exists within the ephemeral store.
   *
   * @param path  path to check
   * @param modes access modes requested (unused, existence is sufficient)
   * @throws NoSuchFileException if the path does not exist
   */
  private void internalCheckAccess (EphemeralPath path, AccessMode... modes)
    throws NoSuchFileException {

    ((EphemeralFileSystem)path.getFileSystem()).getFileStore().checkAccess(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().getFileAttributeView(((NativePath)path).getNativePath(), type, options);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized <A extends BasicFileAttributes> A readAttributes (Path path, Class<A> type, LinkOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().readAttributes(((NativePath)path).getNativePath(), type, options);
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.READ);

      return ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().readAttributes(normalizedPath, type, options);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Map<String, Object> readAttributes (Path path, String attributes, LinkOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().readAttributes(((NativePath)path).getNativePath(), attributes, options);
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.READ);

      return ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().readAttributes(normalizedPath, attributes, options);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void setAttribute (Path path, String attribute, Object value, LinkOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      ((NativePath)path).getNativeFileSystem().provider().setAttribute(((NativePath)path).getNativePath(), attribute, value, options);
    } else {

      EphemeralPath normalizedPath = (EphemeralPath)path.normalize();

      internalCheckAccess(normalizedPath, AccessMode.WRITE);

      ((EphemeralFileSystem)normalizedPath.getFileSystem()).getFileStore().setAttribute(normalizedPath, attribute, value, options);
    }
  }

  /**
   * Opens a {@link FileChannel} on a native path; ephemeral paths are unsupported.
   */
  public FileChannel newFileChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().newFileChannel(((NativePath)path).getNativePath(), options, attrs);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Opens an {@link AsynchronousFileChannel} on a native path; ephemeral paths are unsupported.
   */
  public AsynchronousFileChannel newAsynchronousFileChannel (Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs)
    throws IOException {

    if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().newAsynchronousFileChannel(((NativePath)path).getNativePath(), options, executor, attrs);
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
