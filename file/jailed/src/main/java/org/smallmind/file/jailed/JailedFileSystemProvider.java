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
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A {@link FileSystemProvider} that exposes a jailed, chroot-like view of an underlying
 * native file system via a {@link JailedPathTranslator}.
 *
 * <p>This provider manages a single {@link JailedFileSystem} instance. All file operations
 * are forwarded to the native file system's provider after translating the supplied jailed
 * paths into their native equivalents. Paths returned from listing operations are wrapped
 * back into jailed paths before being delivered to callers.
 *
 * <p>Because only one file system instance exists per provider, calls to
 * {@link #newFileSystem(URI, Map)} always throw {@link FileSystemAlreadyExistsException}.
 *
 * @see JailedFileSystem
 * @see JailedPathTranslator
 */
public class JailedFileSystemProvider extends FileSystemProvider {

  /**
   * The single jailed file system instance managed by this provider.
   */
  private final JailedFileSystem jailedFileSystem;

  /**
   * The translator responsible for mapping between jailed and native paths.
   */
  private final JailedPathTranslator jailedPathTranslator;

  /**
   * The URI scheme registered for this provider (e.g., {@code "jailed"}).
   */
  private final String scheme;

  /**
   * Creates a provider with the default scheme {@code "jailed"} and a
   * {@link ContextSensitiveRootedPathTranslator} backed by the JVM default file system.
   *
   * <p>The jail root is resolved at call time from a thread-bound
   * {@link RootedFileSystemContext}.
   */
  public JailedFileSystemProvider () {

    this("jailed", new ContextSensitiveRootedPathTranslator(FileSystems.getDefault()));
  }

  /**
   * Creates a provider with the default scheme {@code "jailed"} and a
   * {@link ContextSensitiveRootedPathTranslator} backed by the file system owned by the
   * given provider.
   *
   * <p>The scheme of the resulting instance is always {@code "jailed"}, not the scheme of
   * {@code fileSystemProvider}. The other provider is used only to obtain the backing
   * native {@link FileSystem}.
   *
   * @param fileSystemProvider the provider whose root file system will back the jail
   */
  public JailedFileSystemProvider (FileSystemProvider fileSystemProvider) {

    this("jailed", new ContextSensitiveRootedPathTranslator(fileSystemProvider.getFileSystem(URI.create(fileSystemProvider.getScheme() + ":///"))));
  }

  /**
   * Creates a provider with an explicit URI scheme and path translator.
   *
   * @param scheme               the URI scheme to register for this provider (e.g.,
   *                             {@code "jailed"})
   * @param jailedPathTranslator the {@link JailedPathTranslator} that performs path
   *                             translation between the jail and the native file system
   */
  public JailedFileSystemProvider (String scheme, JailedPathTranslator jailedPathTranslator) {

    this.scheme = scheme;
    this.jailedPathTranslator = jailedPathTranslator;

    jailedFileSystem = new JailedFileSystem(this);
  }

  /**
   * Returns the URI scheme registered for this provider.
   *
   * @return the URI scheme string (e.g., {@code "jailed"})
   */
  @Override
  public String getScheme () {

    return scheme;
  }

  /**
   * Returns the {@link JailedPathTranslator} used by this provider to map between jailed
   * paths and native paths.
   *
   * @return the path translator; never {@code null}
   */
  public JailedPathTranslator getJailedPathTranslator () {

    return jailedPathTranslator;
  }

  /**
   * Always throws {@link FileSystemAlreadyExistsException} because this provider manages
   * exactly one pre-created {@link JailedFileSystem} instance.
   *
   * @param uri the URI identifying the file system (validated by
   *            {@link JailedURIUtility#checkUri(String, URI)})
   * @param env ignored
   * @return never returns normally
   * @throws FileSystemAlreadyExistsException always
   * @throws IllegalArgumentException         if the URI is invalid for this provider
   */
  @Override
  public FileSystem newFileSystem (URI uri, Map<String, ?> env) {

    JailedURIUtility.checkUri(scheme, uri);
    throw new FileSystemAlreadyExistsException();
  }

  /**
   * Returns the single {@link JailedFileSystem} instance managed by this provider after
   * validating the URI.
   *
   * @param uri the URI identifying the file system (validated by
   *            {@link JailedURIUtility#checkUri(String, URI)})
   * @return the managed {@link JailedFileSystem}
   * @throws IllegalArgumentException if the URI is invalid for this provider
   */
  @Override
  public FileSystem getFileSystem (URI uri) {

    JailedURIUtility.checkUri(scheme, uri);

    return jailedFileSystem;
  }

  /**
   * Converts a URI to a {@link JailedPath} using
   * {@link JailedURIUtility#fromUri(JailedFileSystem, URI)}.
   *
   * @param uri the URI to convert to a path
   * @return a {@link JailedPath} whose value is the path component of {@code uri}
   * @throws IllegalArgumentException if the URI is not compatible with this provider
   */
  @Override
  public Path getPath (URI uri) {

    return JailedURIUtility.fromUri(jailedFileSystem, uri);
  }

  /**
   * Translates {@code path} to its native equivalent and opens a byte channel via the
   * native provider.
   *
   * @param path    the jailed path for which to open a byte channel
   * @param options options specifying how the file is opened
   * @param attrs   optional attributes to set atomically on creation
   * @return a new {@link SeekableByteChannel} on the underlying file
   * @throws IOException if an I/O error occurs
   */
  @Override
  public SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().newByteChannel(jailedPathTranslator.unwrapPath(path), options, attrs);
  }

  /**
   * Opens a directory stream on the native path corresponding to {@code dir}. The filter
   * is applied to jailed-path wrappers of each native entry; the iterator also yields
   * jailed paths.
   *
   * @param dir    the jailed path of the directory to list
   * @param filter filter applied to each directory entry
   * @return a {@link DirectoryStream} whose iterator yields {@link JailedPath} instances
   * @throws IOException if an I/O error occurs
   */
  @Override
  public DirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter)
    throws IOException {

    DirectoryStream<Path> nativeDirectoryStream = jailedPathTranslator.getNativeFileSystem().provider().newDirectoryStream(jailedPathTranslator.unwrapPath(dir), entry -> {

      try {
        return filter.accept(jailedPathTranslator.wrapPath(jailedFileSystem, entry));
      } catch (IOException ioException) {
        throw new RuntimeException(ioException);
      }
    });

    return new DirectoryStream<>() {

      @Override
      public Iterator<Path> iterator () {

        Iterator<Path> nativeIterator = nativeDirectoryStream.iterator();

        return new Iterator<>() {

          @Override
          public boolean hasNext () {

            return nativeIterator.hasNext();
          }

          @Override
          public Path next () {

            try {
              return jailedPathTranslator.wrapPath(jailedFileSystem, nativeIterator.next());
            } catch (IOException ioException) {
              throw new RuntimeException(ioException);
            }
          }
        };
      }

      @Override
      public void close ()
        throws IOException {

        nativeDirectoryStream.close();
      }
    };
  }

  /**
   * Translates {@code dir} to its native equivalent and creates the directory via the
   * native provider.
   *
   * @param dir   the jailed path at which the directory should be created
   * @param attrs optional attributes to set atomically on the new directory
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void createDirectory (Path dir, FileAttribute<?>... attrs)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().createDirectory(jailedPathTranslator.unwrapPath(dir), attrs);
  }

  /**
   * Translates {@code path} to its native equivalent and deletes the file or directory via
   * the native provider.
   *
   * @param path the jailed path of the file or directory to delete
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void delete (Path path)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().delete(jailedPathTranslator.unwrapPath(path));
  }

  /**
   * Translates {@code source} and {@code target} to their native equivalents and copies the
   * file via the native provider.
   *
   * @param source  the jailed path of the file to copy
   * @param target  the jailed path of the copy destination
   * @param options options controlling how the copy is performed
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void copy (Path source, Path target, CopyOption... options)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().copy(jailedPathTranslator.unwrapPath(source), jailedPathTranslator.unwrapPath(target), options);
  }

  /**
   * Translates {@code source} and {@code target} to their native equivalents and moves the
   * file via the native provider.
   *
   * @param source  the jailed path of the file to move
   * @param target  the jailed path of the move destination
   * @param options options controlling how the move is performed
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void move (Path source, Path target, CopyOption... options)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().move(jailedPathTranslator.unwrapPath(source), jailedPathTranslator.unwrapPath(target), options);
  }

  /**
   * Translates both paths to their native equivalents and checks file identity via the
   * native provider.
   *
   * @param path  the first jailed path
   * @param path2 the second jailed path
   * @return {@code true} if both paths locate the same underlying file
   * @throws IOException if an I/O error occurs
   */
  @Override
  public boolean isSameFile (Path path, Path path2)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().isSameFile(jailedPathTranslator.unwrapPath(path), jailedPathTranslator.unwrapPath(path));
  }

  /**
   * Translates {@code path} to its native equivalent and checks whether the file is hidden
   * via the native provider.
   *
   * @param path the jailed path to check
   * @return {@code true} if the file is considered hidden on the native file system
   * @throws IOException if an I/O error occurs
   */
  @Override
  public boolean isHidden (Path path)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().isHidden(jailedPathTranslator.unwrapPath(path));
  }

  /**
   * Translates {@code path} to its native equivalent and retrieves the {@link FileStore}
   * from the native provider.
   *
   * @param path the jailed path whose file store is to be returned
   * @return the {@link FileStore} for the underlying file
   * @throws IOException if an I/O error occurs
   */
  @Override
  public FileStore getFileStore (Path path)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().getFileStore(jailedPathTranslator.unwrapPath(path));
  }

  /**
   * Translates {@code path} to its native equivalent and checks access via the native provider.
   *
   * @param path  the jailed path to check
   * @param modes the access modes to verify; may be empty to check only existence
   * @throws IOException if the file does not exist, access is denied, or another I/O error occurs
   */
  @Override
  public void checkAccess (Path path, AccessMode... modes)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().checkAccess(jailedPathTranslator.unwrapPath(path), modes);
  }

  /**
   * Translates {@code path} to its native equivalent and retrieves a file attribute view.
   * Any {@link IOException} from path translation is wrapped in a {@link RuntimeException}.
   *
   * @param <V>     the type of the file attribute view
   * @param path    the jailed path for which to obtain the attribute view
   * @param type    the {@link Class} of the desired attribute view
   * @param options options indicating how symbolic links are handled
   * @return the file attribute view, or {@code null} if the view type is not available
   */
  @Override
  public <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    try {
      return jailedPathTranslator.getNativeFileSystem().provider().getFileAttributeView(jailedPathTranslator.unwrapPath(path), type, options);
    } catch (IOException ioException) {
      throw new RuntimeException(ioException);
    }
  }

  /**
   * Translates {@code path} to its native equivalent and reads file attributes from the
   * native provider.
   *
   * @param <A>     the type of the file attributes object
   * @param path    the jailed path for which to read attributes
   * @param type    the {@link Class} of the desired attributes type
   * @param options options indicating how symbolic links are handled
   * @return the file attributes of the specified type
   * @throws IOException if an I/O error occurs
   */
  @Override
  public <A extends BasicFileAttributes> A readAttributes (Path path, Class<A> type, LinkOption... options)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().readAttributes(jailedPathTranslator.unwrapPath(path), type, options);
  }

  /**
   * Translates {@code path} to its native equivalent and reads named attributes from the
   * native provider.
   *
   * @param path       the jailed path for which to read attributes
   * @param attributes a string of the form {@code "<view>:<attrs>"}
   * @param options    options indicating how symbolic links are handled
   * @return a map from attribute name to attribute value
   * @throws IOException if an I/O error occurs
   */
  @Override
  public Map<String, Object> readAttributes (Path path, String attributes, LinkOption... options)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().readAttributes(jailedPathTranslator.unwrapPath(path), attributes, options);
  }

  /**
   * Translates {@code path} to its native equivalent and sets the file attribute via the
   * native provider.
   *
   * @param path      the jailed path of the file whose attribute is to be set
   * @param attribute the attribute to set, in the form {@code "<view>:<name>"}
   * @param value     the new value for the attribute
   * @param options   options indicating how symbolic links are handled
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void setAttribute (Path path, String attribute, Object value, LinkOption... options)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().setAttribute(jailedPathTranslator.unwrapPath(path), attribute, value, options);
  }
}
