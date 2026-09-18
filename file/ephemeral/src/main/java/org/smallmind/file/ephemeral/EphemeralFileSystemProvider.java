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
import java.nio.ByteBuffer;
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
import java.nio.file.StandardCopyOption;
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
import java.util.concurrent.atomic.AtomicReference;

/*
 -Djava.nio.file.spi.DefaultFileSystemProvider=org.smallmind.file.ephemeral.EphemeralFileSystemProvider

 -Dorg.smallmind.file.ephemeral.configuration.capacity=<Long.MAX_VALUE>
 -Dorg.smallmind.file.ephemeral.configuration.blockSize=<1024>
 -Dorg.smallmind.file.ephemeral.configuration.roots=/overlay/file/system,/any/other/path
*/

/**
 * {@link FileSystemProvider} that serves paths from an in-memory ephemeral store, with
 * transparent delegation to a native provider for paths that do not match the configured
 * roots. The provider can be installed as the JVM default by setting the system property
 * {@code java.nio.file.spi.DefaultFileSystemProvider} to its fully qualified class name.
 *
 * <p>Three constructors are available:
 * <ul>
 *   <li>No-arg: uses the {@code "ephemeral"} scheme and no native delegation.</li>
 *   <li>{@link #EphemeralFileSystemProvider(FileSystemProvider)}: mirrors the scheme of an
 *       existing provider and retains a reference to it; the underlying native
 *       {@link FileSystem} is resolved lazily on first use rather than during construction,
 *       because this constructor is invoked from within
 *       {@code FileSystems$DefaultFileSystemHolder.<clinit>} when installed as the JVM
 *       default, and any call back into {@link java.nio.file.FileSystems#getDefault()} on
 *       that thread would observe a partially-initialised holder.</li>
 *   <li>{@link #EphemeralFileSystemProvider(String)}: uses a custom scheme with no native
 *       delegation.</li>
 * </ul>
 *
 * <p>All operations that accept a {@link Path} verify that the path's file system is an
 * {@link EphemeralFileSystem}; a {@link ProviderMismatchException} is thrown otherwise.
 * {@link NativePath} instances are redirected to the native provider without touching the
 * heap.
 */
public class EphemeralFileSystemProvider extends FileSystemProvider {

  private static final CountDownLatch INITIALIZATION_LATCH = new CountDownLatch(1);
  private final EphemeralFileSystem ephemeralFileSystem;
  private final AtomicReference<FileSystem> nativeFileSystemRef = new AtomicReference<>();
  private final String scheme;
  private FileSystemProvider fileSystemProvider;

  /**
   * Creates a provider using the default {@code "ephemeral"} URI scheme.
   */
  public EphemeralFileSystemProvider () {

    this("ephemeral");
  }

  /**
   * Creates a provider that mirrors the URI scheme of the given native provider and retains
   * a reference to it for delegation of non-ephemeral paths. The native {@link FileSystem}
   * itself is <em>not</em> resolved here; it is fetched on demand by
   * {@link #getNativeFileSystem()}. Deferring the {@code getFileSystem} call keeps this
   * constructor free of any callback into {@link java.nio.file.FileSystems#getDefault()},
   * which is essential when the provider is being instantiated by
   * {@code FileSystems$DefaultFileSystemHolder.<clinit>} on the same thread.
   *
   * @param fileSystemProvider the provider whose scheme to mirror and whose file system to
   *                           use for native delegation; must not be {@code null}
   */
  public EphemeralFileSystemProvider (FileSystemProvider fileSystemProvider) {

    this(fileSystemProvider.getScheme());

    this.fileSystemProvider = fileSystemProvider;
  }

  /**
   * Creates a provider using the given URI scheme. Initialises the singleton
   * {@link EphemeralFileSystem} and counts down the initialization latch.
   *
   * @param scheme the URI scheme to expose; must not be {@code null}
   */
  public EphemeralFileSystemProvider (String scheme) {

    this.scheme = scheme;

    EphemeralFileSystemConfiguration configuration = new EphemeralFileSystemConfiguration();

    if (isInstalledAsDefaultFileSystem()) {
      // refused here, while the reason can still be stated plainly, rather than surfacing much
      // later as an InternalError raised from the platform's own start-up
      configuration.checkFitToBeDefaultFileSystem();
    }

    ephemeralFileSystem = new EphemeralFileSystem(this, configuration);
    INITIALIZATION_LATCH.countDown();
  }

  /**
   * Returns whether this class has been named as the JVM default file system provider.
   *
   * <p>Distinct from {@link #isDefault()}, which only reports that this provider mirrors the
   * {@code "file"} scheme. Mirroring the scheme is what makes delegation possible and is perfectly
   * ordinary in a test; being named in
   * {@code java.nio.file.spi.DefaultFileSystemProvider} is what makes this provider answer for
   * every path in the JVM, and only then must the configuration leave the platform able to read its
   * own files.
   *
   * @return {@code true} if this class is named as the JVM default provider
   */
  private static boolean isInstalledAsDefaultFileSystem () {

    String property;

    if ((property = System.getProperty("java.nio.file.spi.DefaultFileSystemProvider")) != null) {
      // the property holds a comma separated chain of provider class names
      for (String className : property.split(",")) {
        if (EphemeralFileSystemProvider.class.getName().equals(className.strip())) {

          return true;
        }
      }
    }

    return false;
  }

  /**
   * Waits until the provider has finished initializing. This is useful in environments where
   * multiple threads may attempt to use the provider before its constructor has completed.
   *
   * @param timeout the maximum time to wait
   * @param unit    the time unit of the {@code timeout} argument
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException     if the provider does not finish initializing within the timeout
   */
  public static void waitForInitialization (long timeout, TimeUnit unit)
    throws InterruptedException, TimeoutException {

    if (!INITIALIZATION_LATCH.await(timeout, unit)) {
      throw new TimeoutException();
    }
  }

  /**
   * Indicates whether this provider is masquerading as the JVM default {@code "file"} scheme.
   *
   * @return {@code true} when the configured scheme is {@code "file"}
   */
  public boolean isDefault () {

    return "file".equals(scheme);
  }

  /**
   * Returns the URI scheme associated with this provider.
   *
   * @return the scheme string; never {@code null}
   */
  @Override
  public String getScheme () {

    return scheme;
  }

  /**
   * Returns the native {@link FileSystem} used for delegating non-ephemeral paths. The
   * underlying call to {@code fileSystemProvider.getFileSystem(URI)} is made at most once;
   * the resolved instance is published into an {@link AtomicReference} and reused on every
   * subsequent invocation. Once cached the fast path is a single volatile read with no
   * further provider interaction; on the initial miss, threads that race install their
   * resolution via {@link AtomicReference#compareAndSet compare-and-set} and then re-read,
   * so every caller observes the same {@link FileSystem}. Performing this resolution lazily
   * (rather than during construction) is what keeps the constructor safe to invoke from
   * within {@code FileSystems$DefaultFileSystemHolder.<clinit>}.
   *
   * @return the native {@link FileSystem}; never {@code null}
   * @throws NullPointerException if this provider was constructed without a native provider
   *                              (i.e., via the no-arg or {@link #EphemeralFileSystemProvider(String)}
   *                              constructors)
   */
  public FileSystem getNativeFileSystem () {

    FileSystem nativeFileSystem;

    if (fileSystemProvider == null) {
      throw new UnsupportedOperationException("This provider(" + scheme + ") was constructed without a native provider, so it cannot delegate");
    }

    if ((nativeFileSystem = nativeFileSystemRef.get()) == null) {
      nativeFileSystemRef.compareAndSet(null, fileSystemProvider.getFileSystem(URI.create(fileSystemProvider.getScheme() + ":///")));
      nativeFileSystem = nativeFileSystemRef.get();
    }

    return nativeFileSystem;
  }

  /**
   * Not supported; this provider maintains a single pre-constructed file system. Always
   * throws {@link FileSystemAlreadyExistsException} after validating the URI.
   *
   * @param uri the URI (validated but otherwise unused)
   * @param env the environment map (unused)
   * @return never returns normally
   * @throws FileSystemAlreadyExistsException always
   */
  @Override
  public FileSystem newFileSystem (URI uri, Map<String, ?> env) {

    EphemeralURIUtility.checkUri(scheme, uri);
    throw new FileSystemAlreadyExistsException();
  }

  /**
   * Returns the singleton {@link EphemeralFileSystem} for this provider.
   *
   * @param uri the URI, whose scheme must match this provider's scheme
   * @return the singleton ephemeral file system; never {@code null}
   */
  @Override
  public FileSystem getFileSystem (URI uri) {

    EphemeralURIUtility.checkUri(scheme, uri);

    return ephemeralFileSystem;
  }

  /**
   * Converts a URI into a {@link Path} within the ephemeral file system.
   *
   * @param uri the URI to convert
   * @return the resulting {@link Path}
   */
  @Override
  public Path getPath (URI uri) {

    return EphemeralURIUtility.fromUri(ephemeralFileSystem, uri);
  }

  /**
   * Opens or creates a file, returning a {@link SeekableByteChannel}. Paths that are
   * {@link NativePath} instances are delegated to the native provider. For ephemeral paths
   * the access mode is verified before opening the channel in the heap store.
   *
   * @param path    the file path; must belong to an {@link EphemeralFileSystem}
   * @param options the set of open options
   * @param attrs   optional file attributes to set on creation
   * @return the opened channel
   * @throws IOException               if the channel cannot be opened
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().newByteChannel(((NativePath)path).getNativePath(), options, attrs);
    } else {

      EphemeralPath ephemeralPath = (EphemeralPath)path;

      return ((EphemeralFileSystem)ephemeralPath.getFileSystem()).getFileStore().newByteChannel(ephemeralPath, options, attrs);
    }
  }

  /**
   * Opens a directory, returning a {@link DirectoryStream} over its entries. {@link NativePath}
   * instances are delegated to the native provider.
   *
   * @param dir    the directory path; must belong to an {@link EphemeralFileSystem}
   * @param filter an optional filter applied to directory entries; may be {@code null}
   * @return the directory stream
   * @throws IOException               if the directory cannot be opened
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public DirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter)
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
   * Opens a directory as a {@link SecureDirectoryStream}, bypassing the native delegation
   * path. Only ephemeral paths are accepted.
   *
   * @param dir     the directory path; must belong to an {@link EphemeralFileSystem}
   * @param filter  an optional filter; may be {@code null}
   * @param options link options (currently unused)
   * @return the secure directory stream
   * @throws NoSuchFileException       if the directory does not exist
   * @throws NotDirectoryException     if the path is not a directory
   * @throws IOException               if the directory cannot be opened
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  public SecureDirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter, LinkOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(dir.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + dir + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else {

      EphemeralPath normalizedDir = (EphemeralPath)dir.normalize();

      internalCheckAccess(normalizedDir, AccessMode.READ);

      return ((EphemeralFileSystem)normalizedDir.getFileSystem()).getFileStore().newDirectoryStream(normalizedDir, filter);
    }
  }

  /**
   * Creates a new directory at the given path. {@link NativePath} instances are delegated
   * to the native provider.
   *
   * @param dir   the directory path to create; must belong to an {@link EphemeralFileSystem}
   * @param attrs optional file attributes to apply on creation
   * @throws IOException               if the directory cannot be created
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public void createDirectory (Path dir, FileAttribute<?>... attrs)
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
   * Deletes the file or directory at the given path. {@link NativePath} instances are
   * delegated to the native provider.
   *
   * @param path the path to delete; must belong to an {@link EphemeralFileSystem}
   * @throws IOException               if the path cannot be deleted
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public void delete (Path path)
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
   * Copies the source path to the target path. Both paths must be associated with the same
   * {@link EphemeralFileSystem}. {@link NativePath} instances are delegated to the native
   * provider.
   *
   * @param source  the source path; must belong to an {@link EphemeralFileSystem}
   * @param target  the target path; must belong to the same {@link EphemeralFileSystem}
   * @param options the copy options
   * @throws IOException               if the copy cannot be performed
   * @throws ProviderMismatchException if either path is not associated with an
   *                                   {@link EphemeralFileSystem}, or if the two paths belong
   *                                   to different file systems
   */
  @Override
  public void copy (Path source, Path target, CopyOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!source.getFileSystem().equals(target.getFileSystem())) {
      throw new ProviderMismatchException("The source and target are associated with different file systems");
    } else if ((source instanceof NativePath) && (target instanceof NativePath)) {

      ((NativePath)source).getNativeFileSystem().provider().copy(((NativePath)source).getNativePath(), ((NativePath)target).getNativePath(), options);
    } else if ((source instanceof NativePath) || (target instanceof NativePath)) {
      // one side is overlaid and the other is not, so the bytes must be carried across
      transfer(source, target, options);
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
   * Moves the source path to the target path. Both paths must be associated with the same
   * {@link EphemeralFileSystem}. {@link NativePath} instances are delegated to the native
   * provider.
   *
   * @param source  the source path; must belong to an {@link EphemeralFileSystem}
   * @param target  the target path; must belong to the same {@link EphemeralFileSystem}
   * @param options the move options
   * @throws IOException               if the move cannot be performed
   * @throws ProviderMismatchException if either path is not associated with an
   *                                   {@link EphemeralFileSystem}, or if the two paths belong
   *                                   to different file systems
   */
  @Override
  public void move (Path source, Path target, CopyOption... options)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!source.getFileSystem().equals(target.getFileSystem())) {
      throw new ProviderMismatchException("The source and target are associated with different file systems");
    } else if ((source instanceof NativePath) && (target instanceof NativePath)) {

      ((NativePath)source).getNativeFileSystem().provider().move(((NativePath)source).getNativePath(), ((NativePath)target).getNativePath(), options);
    } else if ((source instanceof NativePath) || (target instanceof NativePath)) {
      // a move across the overlay boundary has nothing to relink, so it copies and then removes
      transfer(source, target, options);
      delete(source);
    } else {

      EphemeralPath normalizedSource = (EphemeralPath)source.normalize();
      EphemeralPath normalizedTarget = (EphemeralPath)target.normalize();

      internalCheckAccess(normalizedSource, AccessMode.READ);
      if (exists(normalizedTarget)) {
        internalCheckAccess(normalizedTarget, AccessMode.WRITE);
      } else {
        internalCheckAccess(normalizedTarget.getParent(), AccessMode.WRITE);
      }

      ((EphemeralFileSystem)source.getFileSystem()).getFileStore().move(normalizedSource, normalizedTarget, options);
    }
  }

  /**
   * Tests whether two paths refer to the same file. Returns {@code false} when the paths
   * belong to different file systems. {@link NativePath} instances are delegated to the
   * native provider.
   *
   * @param source the first path; must belong to an {@link EphemeralFileSystem}
   * @param target the second path; must belong to an {@link EphemeralFileSystem}
   * @return {@code true} if both paths resolve to the same file, which two hard links do
   * @throws IOException               if an I/O error occurs
   * @throws ProviderMismatchException if either path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public boolean isSameFile (Path source, Path target)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(source.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + source + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!EphemeralFileSystem.class.isAssignableFrom(target.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + target + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (!source.getFileSystem().equals(target.getFileSystem())) {

      return false;
    } else if ((source instanceof NativePath) && (target instanceof NativePath)) {

      return ((NativePath)source).getNativeFileSystem().provider().isSameFile(((NativePath)source).getNativePath(), ((NativePath)target).getNativePath());
    } else if ((source instanceof NativePath) || (target instanceof NativePath)) {
      // a heap file and a real file are never the same file
      return false;
    } else {

      return ((EphemeralFileSystem)source.getFileSystem()).getFileStore().isSameFile((EphemeralPath)source, (EphemeralPath)target);
    }
  }

  /**
   * Indicates whether the given path is considered hidden. An ephemeral path is hidden when its
   * file name begins with a period, which is the convention the platform's own POSIX provider
   * applies; a {@link NativePath} is delegated so that it keeps the native meaning.
   *
   * @param path the path to test
   * @return {@code true} if the path is hidden
   * @throws IOException               if the native provider reports an error
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public boolean isHidden (Path path)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().isHidden(((NativePath)path).getNativePath());
    } else {

      Path fileName;

      return ((fileName = path.getFileName()) != null) && fileName.toString().startsWith(".");
    }
  }

  /**
   * Returns the file store that contains the given path. Delegates to the first file store
   * reported by the path's file system.
   *
   * @param path the path to query
   * @return the associated {@link FileStore}
   * @throws IOException if the native provider reports an error
   */
  @Override
  public FileStore getFileStore (Path path)
    throws IOException {

    if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().getFileStore(((NativePath)path).getNativePath());
    } else {

      return path.getFileSystem().getFileStores().iterator().next();
    }
  }

  /**
   * Checks whether the given path is accessible in the requested mode(s). {@link NativePath}
   * instances are delegated to the native provider; ephemeral paths simply verify existence.
   *
   * @param path  the path to check; must belong to an {@link EphemeralFileSystem}
   * @param modes the access modes to verify (existence is sufficient for ephemeral paths)
   * @throws IOException               if the path does not exist or cannot be accessed
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public void checkAccess (Path path, AccessMode... modes)
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
   * Verifies that the given ephemeral path exists in the heap store. The {@code modes}
   * argument is accepted for API compatibility but existence alone is considered sufficient.
   *
   * @param path  the normalized ephemeral path to check
   * @param modes the access modes to check (not enforced beyond existence)
   * @throws NoSuchFileException if the path does not exist
   */
  private void internalCheckAccess (EphemeralPath path, AccessMode... modes)
    throws IOException {

    ((EphemeralFileSystem)path.getFileSystem()).getFileStore().checkAccess(path, modes);
  }

  /**
   * Returns a file-attribute view of the requested type for the given path.
   * {@link NativePath} instances are delegated to the native provider. For ephemeral paths
   * {@code null} is returned when the path does not exist or the view type is unsupported.
   *
   * @param <V>     the view type
   * @param path    the path; must belong to an {@link EphemeralFileSystem}
   * @param type    the class of the desired view
   * @param options link options passed through to the store
   * @return the view, or {@code null} when unavailable
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().getFileAttributeView(((NativePath)path).getNativePath(), type, options);
    } else {

      try {

        return ((EphemeralFileSystem)path.getFileSystem()).getFileStore().getFileAttributeView((EphemeralPath)path, type, options);
      } catch (IOException ioException) {
        // the view of a file that cannot be resolved is simply absent
        return null;
      }
    }
  }

  /**
   * Reads strongly typed file attributes for the given path. {@link NativePath} instances are
   * delegated to the native provider.
   *
   * @param <A>     the attribute type
   * @param path    the path; must belong to an {@link EphemeralFileSystem}
   * @param type    the class of the desired attributes
   * @param options link options passed through to the store
   * @return the read attributes
   * @throws IOException               if the attributes cannot be read
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public <A extends BasicFileAttributes> A readAttributes (Path path, Class<A> type, LinkOption... options)
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
   * Reads a selected subset of file attributes by name for the given path.
   * {@link NativePath} instances are delegated to the native provider.
   *
   * @param path       the path; must belong to an {@link EphemeralFileSystem}
   * @param attributes the attribute name selection string
   * @param options    link options passed through to the store
   * @return a map of attribute names to values
   * @throws IOException               if the attributes cannot be read
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public Map<String, Object> readAttributes (Path path, String attributes, LinkOption... options)
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
   * Sets a file attribute for the given path. {@link NativePath} instances are delegated to
   * the native provider.
   *
   * @param path      the path; must belong to an {@link EphemeralFileSystem}
   * @param attribute the qualified attribute name
   * @param value     the new attribute value
   * @param options   link options passed through to the store
   * @throws IOException               if the attribute cannot be set
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public void setAttribute (Path path, String attribute, Object value, LinkOption... options)
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
   * Opens a {@link FileChannel} for the given path.
   *
   * <p>This is a distinct entry point from {@link #newByteChannel}: {@link FileChannel#open} routes
   * here, and a great deal of ordinary code reaches for it in preference to
   * {@code Files.newByteChannel}. Supporting only native paths here would leave every such caller
   * failing however complete the rest of the file system is.
   *
   * @param path    the file to open
   * @param options the open options
   * @param attrs   optional file attributes
   * @return the opened file channel
   * @throws IOException               if the channel cannot be opened
   * @throws ProviderMismatchException if the path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public FileChannel newFileChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (path instanceof NativePath) {

      return ((NativePath)path).getNativeFileSystem().provider().newFileChannel(((NativePath)path).getNativePath(), options, attrs);
    } else {

      return ((EphemeralFileSystem)path.getFileSystem()).getFileStore().newFileChannel((EphemeralPath)path, options, attrs);
    }
  }

  /**
   * Creates a symbolic link.
   *
   * @param link   the link to create
   * @param target the target of the link, which need not exist
   * @param attrs  optional file attributes
   * @throws IOException               if the link cannot be created
   * @throws ProviderMismatchException if the link is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public void createSymbolicLink (Path link, Path target, FileAttribute<?>... attrs)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(link.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + link + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (link instanceof NativePath) {
      ((NativePath)link).getNativeFileSystem().provider().createSymbolicLink(((NativePath)link).getNativePath(), (target instanceof NativePath) ? ((NativePath)target).getNativePath() : target, attrs);
    } else {
      ((EphemeralFileSystem)link.getFileSystem()).getFileStore().createSymbolicLink((EphemeralPath)link, target, attrs);
    }
  }

  /**
   * Reads the target of a symbolic link.
   *
   * @param link the link to read
   * @return the raw target of the link
   * @throws IOException               if the link cannot be read
   * @throws ProviderMismatchException if the link is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public Path readSymbolicLink (Path link)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(link.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + link + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if (link instanceof NativePath) {

      return new NativePath((EphemeralFileSystem)link.getFileSystem(), ((NativePath)link).getNativeFileSystem().provider().readSymbolicLink(((NativePath)link).getNativePath()));
    } else {

      return ((EphemeralFileSystem)link.getFileSystem()).getFileStore().readSymbolicLink((EphemeralPath)link);
    }
  }

  /**
   * Creates an additional name for an existing file, sharing its content.
   *
   * @param link     the new name to create
   * @param existing the existing file to link to
   * @throws IOException               if the link cannot be created
   * @throws ProviderMismatchException if either path is not associated with an
   *                                   {@link EphemeralFileSystem}
   */
  @Override
  public void createLink (Path link, Path existing)
    throws IOException {

    if (!EphemeralFileSystem.class.isAssignableFrom(link.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + link + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    } else if ((link instanceof NativePath) && (existing instanceof NativePath)) {
      ((NativePath)link).getNativeFileSystem().provider().createLink(((NativePath)link).getNativePath(), ((NativePath)existing).getNativePath());
    } else if ((link instanceof NativePath) || (existing instanceof NativePath)) {
      throw new IOException("A hard link cannot span the overlay boundary");
    } else {
      ((EphemeralFileSystem)link.getFileSystem()).getFileStore().createLink((EphemeralPath)link, (EphemeralPath)existing);
    }
  }

  /**
   * Copies one entry across the overlay boundary by carrying its bytes over, since no node can be
   * shared or relinked between the heap and the native file system.
   *
   * @param source  the entry to copy
   * @param target  the destination path
   * @param options the copy options
   * @throws IOException if the copy cannot be performed
   */
  private void transfer (Path source, Path target, CopyOption... options)
    throws IOException {

    boolean replaceExisting = false;

    if (options != null) {
      for (CopyOption option : options) {
        if (StandardCopyOption.REPLACE_EXISTING.equals(option)) {
          replaceExisting = true;
          break;
        }
      }
    }

    if (exists(target)) {
      if (!replaceExisting) {
        throw new FileAlreadyExistsException(target.toString());
      } else {
        delete(target);
      }
    }

    if (readAttributes(source, BasicFileAttributes.class).isDirectory()) {
      createDirectory(target);
    } else {
      try (SeekableByteChannel sourceChannel = newByteChannel(source, Set.of(StandardOpenOption.READ));
           SeekableByteChannel targetChannel = newByteChannel(target, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW))) {

        ByteBuffer buffer = ByteBuffer.allocate(8192);

        while (sourceChannel.read(buffer) > 0) {
          buffer.flip();
          while (buffer.hasRemaining()) {
            targetChannel.write(buffer);
          }
          buffer.clear();
        }
      }
    }
  }

  /**
   * Opens an {@link AsynchronousFileChannel} for the given path. Only {@link NativePath}
   * instances are supported; ephemeral paths always throw {@link UnsupportedOperationException}.
   *
   * @param path     the native file path
   * @param options  the open options
   * @param executor the thread pool for asynchronous I/O
   * @param attrs    optional file attributes
   * @return the opened asynchronous file channel
   * @throws IOException                   if the channel cannot be opened
   * @throws UnsupportedOperationException if the path is an ephemeral (non-native) path
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
