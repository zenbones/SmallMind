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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * A {@link FileSystemProvider} that exposes a jailed, chroot-like view of an underlying
 * native file system via a {@link JailedPathTranslator}.
 *
 * <p>This provider manages a single {@link JailedFileSystem} instance. Every operation first
 * verifies that the paths it was handed are {@link JailedPath} instances belonging to that file
 * system, then forwards the operation to the native file system's provider using the translated
 * native paths. Paths returned from listing operations are wrapped back into jailed paths before
 * being delivered to callers, and a native path that can not be expressed inside the jail is
 * refused rather than reported.
 *
 * <p>Link options are passed through to the translator, so an operation that does not follow
 * the final symbolic link of a path - deleting it, moving it, or reading its target - is
 * confined on the strength of its parent chain alone, while an operation that does follow it is
 * confined on the strength of its real location.
 *
 * <p>Because only one file system instance exists per provider, calls to
 * {@link #newFileSystem(URI, Map)} always throw {@link FileSystemAlreadyExistsException}.
 *
 * @see JailedFileSystem
 * @see JailedPathTranslator
 */
public class JailedFileSystemProvider extends FileSystemProvider {

  /**
   * The empty set of link options, denoting an operation that follows symbolic links.
   */
  private static final LinkOption[] NO_LINK_OPTIONS = new LinkOption[0];

  /**
   * The set of link options denoting an operation that does not follow symbolic links.
   */
  private static final LinkOption[] NO_FOLLOW_LINK_OPTIONS = new LinkOption[] {LinkOption.NOFOLLOW_LINKS};

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
   * Returns the provider of the backing native file system, to which every operation is
   * forwarded.
   *
   * @return the native {@link FileSystemProvider}
   */
  private FileSystemProvider getNativeFileSystemProvider () {

    return jailedPathTranslator.getNativeFileSystem().provider();
  }

  /**
   * Translates a jailed path into its native equivalent after verifying that it belongs to this
   * provider's file system.
   *
   * @param path    the path supplied by the caller
   * @param options options indicating how symbolic links are handled by the operation being
   *                performed
   * @return the corresponding native {@link Path}
   * @throws IOException               if an I/O error occurs during translation
   * @throws SecurityException         if the path can not be confined to the jail
   * @throws ProviderMismatchException if the path is not a {@link JailedPath} of this provider's
   *                                   file system
   */
  private Path unwrapPath (Path path, LinkOption... options)
    throws IOException {

    if (!((path instanceof JailedPath) && jailedFileSystem.equals(path.getFileSystem()))) {
      throw new ProviderMismatchException();
    } else {

      return jailedPathTranslator.unwrapPath(path, options);
    }
  }

  /**
   * Translates a native path into its jailed equivalent.
   *
   * @param nativePath the native path to wrap
   * @return the corresponding jailed {@link Path}
   * @throws IOException       if an I/O error occurs during translation
   * @throws SecurityException if the native path lies outside the jail
   */
  private Path wrapPath (Path nativePath)
    throws IOException {

    return jailedPathTranslator.wrapPath(jailedFileSystem, nativePath);
  }

  /**
   * Extracts the link options from an array of open or copy options.
   *
   * @param options the options supplied by the caller, which may be {@code null}
   * @return {@link #NO_FOLLOW_LINK_OPTIONS} if the caller asked that links not be followed,
   * otherwise {@link #NO_LINK_OPTIONS}
   */
  private LinkOption[] getLinkOptions (Object[] options) {

    if (options != null) {
      for (Object option : options) {
        if (LinkOption.NOFOLLOW_LINKS.equals(option)) {

          return NO_FOLLOW_LINK_OPTIONS;
        }
      }
    }

    return NO_LINK_OPTIONS;
  }

  /**
   * Extracts the link options from a set of open options.
   *
   * @param options the options supplied by the caller, which may be {@code null}
   * @return {@link #NO_FOLLOW_LINK_OPTIONS} if the caller asked that links not be followed,
   * otherwise {@link #NO_LINK_OPTIONS}
   */
  private LinkOption[] getLinkOptions (Set<? extends OpenOption> options) {

    return ((options != null) && options.contains(LinkOption.NOFOLLOW_LINKS)) ? NO_FOLLOW_LINK_OPTIONS : NO_LINK_OPTIONS;
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
   * Translates {@code path} to its native equivalent and opens an input stream via the native
   * provider.
   *
   * @param path    the jailed path of the file to open
   * @param options options specifying how the file is opened
   * @return a new {@link InputStream} on the underlying file
   * @throws IOException if an I/O error occurs
   */
  @Override
  public InputStream newInputStream (Path path, OpenOption... options)
    throws IOException {

    return getNativeFileSystemProvider().newInputStream(unwrapPath(path, getLinkOptions(options)), options);
  }

  /**
   * Translates {@code path} to its native equivalent and opens an output stream via the native
   * provider.
   *
   * @param path    the jailed path of the file to open
   * @param options options specifying how the file is opened
   * @return a new {@link OutputStream} on the underlying file
   * @throws IOException if an I/O error occurs
   */
  @Override
  public OutputStream newOutputStream (Path path, OpenOption... options)
    throws IOException {

    return getNativeFileSystemProvider().newOutputStream(unwrapPath(path, getLinkOptions(options)), options);
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

    return getNativeFileSystemProvider().newByteChannel(unwrapPath(path, getLinkOptions(options)), options, attrs);
  }

  /**
   * Translates {@code path} to its native equivalent and opens a file channel via the native
   * provider.
   *
   * @param path    the jailed path for which to open a file channel
   * @param options options specifying how the file is opened
   * @param attrs   optional attributes to set atomically on creation
   * @return a new {@link FileChannel} on the underlying file
   * @throws IOException                   if an I/O error occurs
   * @throws UnsupportedOperationException if the native provider does not support file channels
   */
  @Override
  public FileChannel newFileChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    return getNativeFileSystemProvider().newFileChannel(unwrapPath(path, getLinkOptions(options)), options, attrs);
  }

  /**
   * Translates {@code path} to its native equivalent and opens an asynchronous file channel via
   * the native provider.
   *
   * @param path     the jailed path for which to open a channel
   * @param options  options specifying how the file is opened
   * @param executor the thread pool to which tasks are submitted, or {@code null} for the
   *                 default
   * @param attrs    optional attributes to set atomically on creation
   * @return a new {@link AsynchronousFileChannel} on the underlying file
   * @throws IOException                   if an I/O error occurs
   * @throws UnsupportedOperationException if the native provider does not support asynchronous
   *                                       channels
   */
  @Override
  public AsynchronousFileChannel newAsynchronousFileChannel (Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs)
    throws IOException {

    return getNativeFileSystemProvider().newAsynchronousFileChannel(unwrapPath(path, getLinkOptions(options)), options, executor, attrs);
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

    DirectoryStream<Path> nativeDirectoryStream = getNativeFileSystemProvider().newDirectoryStream(unwrapPath(dir), entry -> filter.accept(wrapPath(entry)));

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
              return wrapPath(nativeIterator.next());
            } catch (IOException ioException) {
              throw new DirectoryIteratorException(ioException);
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

    getNativeFileSystemProvider().createDirectory(unwrapPath(dir, NO_FOLLOW_LINK_OPTIONS), attrs);
  }

  /**
   * Creates a symbolic link inside the jail.
   *
   * <p>A relative target is taken to be relative to the directory holding the link, as it is on
   * the native file systems, and is resolved there in order to confine it. The link is then
   * created with a native target expressed relative to that same directory, which keeps the
   * location of the jail out of the link and guarantees that the link resolves to the confined
   * target rather than to whatever the untranslated text would have addressed.
   *
   * @param link   the jailed path of the link to create
   * @param target the jailed path the link should resolve to
   * @param attrs  optional attributes to set atomically on the new link
   * @throws IOException                   if an I/O error occurs
   * @throws SecurityException             if either path can not be confined to the jail
   * @throws UnsupportedOperationException if the native provider does not support symbolic links
   */
  @Override
  public void createSymbolicLink (Path link, Path target, FileAttribute<?>... attrs)
    throws IOException {

    Path nativeLink = unwrapPath(link, NO_FOLLOW_LINK_OPTIONS);
    Path jailedLinkParent = link.toAbsolutePath().getParent();
    Path nativeTarget = unwrapPath((jailedLinkParent == null) ? target : jailedLinkParent.resolve(target), NO_FOLLOW_LINK_OPTIONS);
    Path nativeLinkParent = nativeLink.getParent();
    Path nativeRelativeTarget;

    if (nativeLinkParent == null) {
      nativeRelativeTarget = nativeTarget;
    } else if ((nativeRelativeTarget = nativeLinkParent.relativize(nativeTarget)).getNameCount() == 0) {
      nativeRelativeTarget = jailedPathTranslator.getNativeFileSystem().getPath(".");
    }

    getNativeFileSystemProvider().createSymbolicLink(nativeLink, nativeRelativeTarget, attrs);
  }

  /**
   * Creates a hard link inside the jail.
   *
   * @param link     the jailed path of the link to create
   * @param existing the jailed path of the existing file to link to
   * @throws IOException                   if an I/O error occurs
   * @throws SecurityException             if either path can not be confined to the jail
   * @throws UnsupportedOperationException if the native provider does not support hard links
   */
  @Override
  public void createLink (Path link, Path existing)
    throws IOException {

    getNativeFileSystemProvider().createLink(unwrapPath(link, NO_FOLLOW_LINK_OPTIONS), unwrapPath(existing));
  }

  /**
   * Reads the target of a symbolic link inside the jail.
   *
   * <p>The native target is translated back into jail space, which means that a link pointing
   * out of the jail is refused with a {@link SecurityException} rather than having its target
   * disclosed. As on the native file systems, a relative result is relative to the directory
   * holding the link.
   *
   * @param link the jailed path of the link to read
   * @return the target of the link, as a jailed {@link Path}
   * @throws IOException                   if the path is not a link or an I/O error occurs
   * @throws SecurityException             if the link points outside the jail
   * @throws UnsupportedOperationException if the native provider does not support symbolic links
   */
  @Override
  public Path readSymbolicLink (Path link)
    throws IOException {

    return wrapPath(getNativeFileSystemProvider().readSymbolicLink(unwrapPath(link, NO_FOLLOW_LINK_OPTIONS)));
  }

  /**
   * Translates {@code path} to its native equivalent and deletes the file or directory via
   * the native provider.
   *
   * <p>Deletion does not follow the final symbolic link of the path, so a link that points out
   * of the jail may still be removed; only reading through it is refused.
   *
   * @param path the jailed path of the file or directory to delete
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void delete (Path path)
    throws IOException {

    getNativeFileSystemProvider().delete(unwrapPath(path, NO_FOLLOW_LINK_OPTIONS));
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

    getNativeFileSystemProvider().copy(unwrapPath(source, getLinkOptions(options)), unwrapPath(target, NO_FOLLOW_LINK_OPTIONS), options);
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

    getNativeFileSystemProvider().move(unwrapPath(source, NO_FOLLOW_LINK_OPTIONS), unwrapPath(target, NO_FOLLOW_LINK_OPTIONS), options);
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

    return getNativeFileSystemProvider().isSameFile(unwrapPath(path), unwrapPath(path2));
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

    return getNativeFileSystemProvider().isHidden(unwrapPath(path));
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

    return getNativeFileSystemProvider().getFileStore(unwrapPath(path));
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

    getNativeFileSystemProvider().checkAccess(unwrapPath(path), modes);
  }

  /**
   * Translates {@code path} to its native equivalent and retrieves a file attribute view.
   *
   * <p>Because this method can not report a checked exception, an {@link IOException} raised
   * while translating the path is wrapped in an {@link UncheckedIOException}.
   *
   * @param <V>     the type of the file attribute view
   * @param path    the jailed path for which to obtain the attribute view
   * @param type    the {@link Class} of the desired attribute view
   * @param options options indicating how symbolic links are handled
   * @return the file attribute view, or {@code null} if the view type is not available
   * @throws UncheckedIOException if an I/O error occurs while translating the path
   * @throws SecurityException    if the path can not be confined to the jail
   */
  @Override
  public <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    try {
      return getNativeFileSystemProvider().getFileAttributeView(unwrapPath(path, options), type, options);
    } catch (IOException ioException) {
      throw new UncheckedIOException(ioException);
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

    return getNativeFileSystemProvider().readAttributes(unwrapPath(path, options), type, options);
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

    return getNativeFileSystemProvider().readAttributes(unwrapPath(path, options), attributes, options);
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

    getNativeFileSystemProvider().setAttribute(unwrapPath(path, options), attribute, value, options);
  }
}
