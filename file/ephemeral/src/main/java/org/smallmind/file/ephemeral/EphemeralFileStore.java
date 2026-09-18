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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.NotLinkException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.file.ephemeral.heap.DirectoryNode;
import org.smallmind.file.ephemeral.heap.FileNode;
import org.smallmind.file.ephemeral.heap.HeapEvent;
import org.smallmind.file.ephemeral.heap.HeapEventListener;
import org.smallmind.file.ephemeral.heap.HeapEventType;
import org.smallmind.file.ephemeral.heap.HeapFileContent;
import org.smallmind.file.ephemeral.heap.HeapNode;
import org.smallmind.file.ephemeral.heap.HeapNodeType;
import org.smallmind.file.ephemeral.heap.HeapSpaceGovernor;
import org.smallmind.file.ephemeral.heap.LinkNode;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * The {@link FileStore} that owns the in-memory tree behind an {@link EphemeralFileSystem}, and the
 * single place where that tree is mutated.
 *
 * <h2>Locking</h2>
 * The shape of the tree — which nodes exist, and where — is guarded by one
 * {@link ReentrantReadWriteLock}. Lookups take the read lock; anything that creates, removes,
 * renames, or re-parents a node takes the write lock. The <em>content</em> of a file is guarded
 * separately, by the monitor of its own {@link HeapFileContent}, so that a long write does not
 * block unrelated directory traversal. The lock order is always tree lock first, then content;
 * space accounting deliberately uses a lock-free counter so that {@link #reserve(long)}, which is
 * called while a content monitor is held, can never reach back for the tree lock.
 *
 * <h2>Capacity</h2>
 * The configured capacity is enforced. Every growth of file content is reserved against a running
 * total first, and a write that would exceed the capacity fails with an {@link IOException} rather
 * than succeeding and driving the reported free space negative.
 *
 * <h2>Links</h2>
 * Path resolution follows symbolic links, bounded by {@link #MAXIMUM_LINK_DEPTH} so that a cycle
 * reports an error instead of exhausting the stack. Hard links are represented by two
 * {@link FileNode}s sharing one {@link HeapFileContent}, which is discarded when the last of them
 * is deleted.
 */
public class EphemeralFileStore extends FileStore implements HeapSpaceGovernor {

  private static final Map<String, Class<? extends FileAttributeView>> SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP = Map.of("basic", BasicFileAttributeView.class);
  private static final String[] BASIC_FILE_ATTRIBUTE_NAMES = new String[] {"creationTime", "lastModifiedTime", "lastAccessTime", "isDirectory", "isRegularFile", "isSymbolicLink", "isOther", "size", "fileKey"};

  /**
   * The number of symbolic links that may be traversed while resolving a single path before the
   * resolution is treated as cyclic.
   */
  private static final int MAXIMUM_LINK_DEPTH = 40;

  private final EphemeralFileSystem fileSystem;
  private final EphemeralFileStoreAttributeView fileStoreAttributeView = new EphemeralFileStoreAttributeView();
  private final ReentrantReadWriteLock treeLock = new ReentrantReadWriteLock();
  private final AtomicLong usedRef = new AtomicLong(0);
  private final DirectoryNode rootNode;
  private final long capacity;
  private final int blockSize;

  /**
   * Creates a store with the given bounds.
   *
   * @param fileSystem the owning file system
   * @param capacity   the total number of bytes of file content the store may hold; must be &gt; 0
   * @param blockSize  the segment size used to allocate file content; must be &gt; 0
   * @throws IllegalArgumentException if either bound is not positive
   */
  public EphemeralFileStore (EphemeralFileSystem fileSystem, long capacity, int blockSize) {

    if ((capacity <= 0) || (blockSize <= 0)) {
      throw new IllegalArgumentException("Both capacity and block size must be > 0");
    }

    this.fileSystem = fileSystem;
    this.capacity = capacity;
    this.blockSize = blockSize;

    rootNode = new DirectoryNode(null, null, blockSize);
  }

  /**
   * Returns whether {@link LinkOption#NOFOLLOW_LINKS} appears among the supplied options.
   *
   * @param options the options to inspect, which may be {@code null}
   * @return {@code true} if links in the final position should not be followed
   */
  private static boolean isNoFollow (LinkOption... options) {

    if (options != null) {
      for (LinkOption option : options) {
        if (LinkOption.NOFOLLOW_LINKS.equals(option)) {

          return true;
        }
      }
    }

    return false;
  }

  /**
   * Validates and interprets the options of a channel open request.
   *
   * <p>Only combinations that {@code java.nio.file} itself forbids are rejected. In particular
   * {@link StandardOpenOption#READ} together with {@link StandardOpenOption#WRITE} is legal and
   * produces a channel that can do both.
   *
   * @param options the requested options
   * @return the interpreted options
   * @throws UnsupportedOperationException if an option this store does not understand is supplied
   * @throws IllegalArgumentException      if the combination of options is invalid
   */
  private static OpenOptions parseOpenOptions (Set<? extends OpenOption> options) {

    OpenOptions parsed = new OpenOptions();

    for (OpenOption option : options) {
      if (option instanceof LinkOption) {
        parsed.noFollowLinks = parsed.noFollowLinks || LinkOption.NOFOLLOW_LINKS.equals(option);
      } else if (!(option instanceof StandardOpenOption)) {
        throw new UnsupportedOperationException(String.valueOf(option));
      } else {
        switch ((StandardOpenOption)option) {
          case READ:
            parsed.read = true;
            break;
          case WRITE:
            parsed.write = true;
            break;
          case APPEND:
            parsed.write = true;
            parsed.append = true;
            break;
          case TRUNCATE_EXISTING:
            parsed.truncateExisting = true;
            break;
          case CREATE:
            parsed.create = true;
            break;
          case CREATE_NEW:
            parsed.createNew = true;
            break;
          case DELETE_ON_CLOSE:
            parsed.deleteOnClose = true;
            break;
          case SPARSE:
          case SYNC:
          case DSYNC:
            // all content is held in memory, so these carry no meaning here
            break;
          default:
            throw new UnknownSwitchCaseException(option.toString());
        }
      }
    }

    if (parsed.append && parsed.read) {
      throw new IllegalArgumentException("READ + APPEND not allowed");
    } else if (parsed.append && parsed.truncateExisting) {
      throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
    }

    if (!(parsed.read || parsed.write)) {
      parsed.read = true;
    }

    return parsed;
  }

  /**
   * Empties the store, discarding every node and returning all accounted space.
   */
  public void clear () {

    treeLock.writeLock().lock();
    try {
      rootNode.clear();
      usedRef.set(0);
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  @Override
  public String name () {

    return EphemeralFileStore.class.getSimpleName();
  }

  @Override
  public String type () {

    return name();
  }

  @Override
  public boolean isReadOnly () {

    return false;
  }

  /**
   * Returns the total number of bytes of file content this store may hold.
   *
   * @return the configured capacity
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public long getTotalSpace () {

    ensureOpen();

    return capacity;
  }

  /**
   * Returns the number of bytes still available for file content.
   *
   * @return the capacity less the bytes currently allocated; never negative
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public long getUsableSpace () {

    ensureOpen();

    return capacity - usedRef.get();
  }

  /**
   * Returns the number of bytes still available for file content.
   *
   * @return the capacity less the bytes currently allocated; never negative
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public long getUnallocatedSpace () {

    ensureOpen();

    return capacity - usedRef.get();
  }

  /**
   * Accounts for file content about to be allocated.
   *
   * <p>Implemented with a compare-and-set loop rather than a lock because it is invoked while a
   * {@link HeapFileContent} monitor is held; taking the tree lock here would invert this store's
   * lock order and could deadlock against an in-flight tree mutation.
   *
   * @param bytes the number of bytes about to be allocated
   * @throws IOException if the allocation would exceed the capacity of this store
   */
  @Override
  public void reserve (long bytes)
    throws IOException {

    while (true) {

      long current = usedRef.get();
      long updated = current + bytes;

      if (updated > capacity) {
        throw new IOException("Insufficient space in file store(" + name() + "), " + (capacity - current) + " of " + capacity + " bytes remain");
      } else if (usedRef.compareAndSet(current, updated)) {

        return;
      }
    }
  }

  /**
   * Returns file content bytes to this store.
   *
   * @param bytes the number of bytes no longer allocated
   */
  @Override
  public void release (long bytes) {

    usedRef.addAndGet(-bytes);
  }

  @Override
  public boolean supportsFileAttributeView (Class<? extends FileAttributeView> type) {

    ensureOpen();

    return SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsValue(type);
  }

  @Override
  public boolean supportsFileAttributeView (String name) {

    ensureOpen();

    return SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsKey(name);
  }

  /**
   * Returns the names of the attribute views this store supports.
   *
   * @return the supported view names
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  public Set<String> getSupportedFileAttributeViewNames () {

    ensureOpen();

    return SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.keySet();
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView (Class<V> type) {

    ensureOpen();

    return EphemeralFileStoreAttributeView.class.equals(type) ? type.cast(fileStoreAttributeView) : null;
  }

  /**
   * Throws if the owning file system has been closed.
   *
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  private void ensureOpen () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    }
  }

  /**
   * Records a resource that must be closed when the owning file system is.
   *
   * @param closeable the channel, directory stream, or watch service to track
   */
  public void registerOpenResource (Closeable closeable) {

    fileSystem.registerOpenResource(closeable);
  }

  /**
   * Stops tracking a resource, because it has closed itself.
   *
   * @param closeable the channel, directory stream, or watch service to forget
   */
  public void unregisterOpenResource (Closeable closeable) {

    fileSystem.unregisterOpenResource(closeable);
  }

  /**
   * Returns the absolute, normalized form of a path, so that every operation addresses the tree the
   * same way regardless of how the caller spelled it.
   *
   * @param path the path to canonicalize
   * @return the absolute, normalized path
   */
  private EphemeralPath canonical (EphemeralPath path) {

    return path.toAbsolutePath().normalize();
  }

  /**
   * Builds the absolute path formed by the first {@code count} name elements of {@code path}.
   *
   * @param path  the path to take a prefix of
   * @param count the number of leading name elements to retain
   * @return the prefix as an absolute path
   */
  private EphemeralPath prefix (EphemeralPath path, int count) {

    return (count == path.getNames().length) ? path : new EphemeralPath(fileSystem, Arrays.copyOfRange(path.getNames(), 0, count), true);
  }

  /**
   * Resolves an absolute, normalized path to the node it names, following symbolic links.
   *
   * <p>A link encountered anywhere but the final element is always followed, because the elements
   * after it have to be looked up somewhere. A link in the final position is followed only when
   * {@code followFinalLink} is set, which is how {@link LinkOption#NOFOLLOW_LINKS} is honoured.
   *
   * @param path            the absolute, normalized path to resolve
   * @param followFinalLink whether a symbolic link in the final position should be resolved
   * @param depth           the number of links already traversed
   * @return the resolution, whose node is {@code null} when nothing exists at the path
   * @throws IOException if more than {@link #MAXIMUM_LINK_DEPTH} links are traversed
   */
  private Resolution resolve (EphemeralPath path, boolean followFinalLink, int depth)
    throws IOException {

    if (depth > MAXIMUM_LINK_DEPTH) {
      throw new FileSystemException(path.toString(), null, "Too many levels of symbolic links");
    } else {

      String[] names = path.getNames();
      DirectoryNode currentNode = rootNode;

      for (int index = 0; index < names.length; index++) {

        HeapNode childNode;
        boolean last = (index == (names.length - 1));

        if ((childNode = currentNode.get(names[index])) == null) {

          return new Resolution(path, null);
        } else if (HeapNodeType.SYMBOLIC_LINK.equals(childNode.getType()) && (followFinalLink || (!last))) {

          EphemeralPath targetPath = linkTarget((LinkNode)childNode, prefix(path, index));

          // whatever followed the link must be re-resolved beneath the link's target
          for (int remaining = index + 1; remaining < names.length; remaining++) {
            targetPath = (EphemeralPath)targetPath.resolve(names[remaining]);
          }

          return resolve(targetPath.normalize(), followFinalLink, depth + 1);
        } else if (last) {

          return new Resolution(prefix(path, index + 1), childNode);
        } else if (!HeapNodeType.DIRECTORY.equals(childNode.getType())) {
          // a file or dangling link used as a directory names nothing
          return new Resolution(path, null);
        } else {
          currentNode = (DirectoryNode)childNode;
        }
      }

      return new Resolution(path, rootNode);
    }
  }

  /**
   * Interprets the raw target of a symbolic link as a path, resolving a relative target against the
   * directory that holds the link.
   *
   * @param linkNode      the link whose target is being interpreted
   * @param linkDirectory the absolute path of the directory containing the link
   * @return the target as an absolute path
   */
  private EphemeralPath linkTarget (LinkNode linkNode, EphemeralPath linkDirectory) {

    EphemeralPath target = new EphemeralPath(fileSystem, linkNode.getTarget());

    return target.isAbsolute() ? target : (EphemeralPath)linkDirectory.resolve(target);
  }

  /**
   * Resolves a path under the read lock, following links unless instructed otherwise.
   *
   * @param path    the path to resolve
   * @param options the link options supplied by the caller
   * @return the node named by the path, or {@code null} if nothing exists there
   * @throws IOException if a symbolic link cycle is encountered
   */
  private HeapNode findNode (EphemeralPath path, LinkOption... options)
    throws IOException {

    treeLock.readLock().lock();
    try {

      return resolve(canonical(path), !isNoFollow(options), 0).node();
    } finally {
      treeLock.readLock().unlock();
    }
  }

  /**
   * Notifies the listeners watching {@code directory} that one of its entries changed.
   *
   * <p>Only the containing directory is notified. A {@link java.nio.file.WatchService}
   * registration observes the entries of one directory and not those of its descendants, so an
   * event must not climb the tree.
   *
   * @param directory the directory holding the changed entry, which may be {@code null}
   * @param path      the absolute path of the changed entry
   * @param type      the kind of change
   */
  private void publish (DirectoryNode directory, EphemeralPath path, HeapEventType type) {

    if (directory != null) {
      directory.fire(new HeapEvent(this, path, type));
    }
  }

  /**
   * Returns the attribute view for a file.
   *
   * @param path    the file to inspect
   * @param type    the view type requested
   * @param options the link options
   * @param <V>     the view type
   * @return the view, or {@code null} if the type is unsupported or the file does not exist
   * @throws IOException if a symbolic link cycle is encountered
   */
  public <V extends FileAttributeView> V getFileAttributeView (EphemeralPath path, Class<V> type, LinkOption... options)
    throws IOException {

    ensureOpen();

    if (!SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsValue(type)) {

      return null;
    } else {

      HeapNode heapNode;

      return ((heapNode = findNode(path, options)) != null) ? type.cast(new EphemeralBasicFileAttributeView(heapNode.getAttributes())) : null;
    }
  }

  /**
   * Reads the attributes of a file.
   *
   * @param path    the file to inspect
   * @param type    the attribute type requested
   * @param options the link options
   * @param <A>     the attribute type
   * @return the attributes of the file
   * @throws NoSuchFileException           if the file does not exist
   * @throws UnsupportedOperationException if the attribute type is not supported
   * @throws IOException                   if a symbolic link cycle is encountered
   */
  public <A extends BasicFileAttributes> A readAttributes (EphemeralPath path, Class<A> type, LinkOption... options)
    throws IOException {

    ensureOpen();

    if (!type.isAssignableFrom(EphemeralBasicFileAttributes.class)) {
      throw new UnsupportedOperationException(type.getName());
    } else {

      HeapNode heapNode;

      if ((heapNode = findNode(path, options)) == null) {
        throw new NoSuchFileException(path.toString());
      }

      return type.cast(heapNode.getAttributes());
    }
  }

  /**
   * Reads named attributes of a file into a map.
   *
   * @param path       the file to inspect
   * @param attributes a comma-separated attribute list, optionally prefixed by a view name, where
   *                   {@code "*"} selects every attribute of the view
   * @param options    the link options
   * @return a map of attribute name to value
   * @throws NoSuchFileException           if the file does not exist
   * @throws IllegalArgumentException      if an attribute is not recognised
   * @throws UnsupportedOperationException if the named view is not supported
   * @throws IOException                   if a symbolic link cycle is encountered
   */
  public Map<String, Object> readAttributes (EphemeralPath path, String attributes, LinkOption... options)
    throws IOException {

    ensureOpen();

    HeapNode heapNode;
    String[] attributeNames;
    String viewName;
    boolean asterisk = false;
    int colonPos;

    if ((colonPos = attributes.indexOf(':')) >= 0) {
      if (!SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsKey(viewName = attributes.substring(0, colonPos))) {
        throw new UnsupportedOperationException(viewName);
      }
      attributeNames = attributes.substring(colonPos + 1).split(",");
    } else {
      attributeNames = attributes.split(",");
    }

    for (int index = 0; index < attributeNames.length; index++) {
      attributeNames[index] = attributeNames[index].strip();

      if (!asterisk) {
        if (attributeNames[index].indexOf('*') >= 0) {
          asterisk = true;
        }
      }
    }

    if (asterisk) {
      attributeNames = BASIC_FILE_ATTRIBUTE_NAMES;
    }

    if ((heapNode = findNode(path, options)) == null) {
      throw new NoSuchFileException(path.toString());
    } else {

      EphemeralBasicFileAttributes fileAttributes = heapNode.getAttributes();
      HashMap<String, Object> attributeMap = new HashMap<>();

      for (String attributeName : attributeNames) {
        switch (attributeName) {
          case "creationTime":
            attributeMap.put("creationTime", fileAttributes.creationTime());
            break;
          case "lastModifiedTime":
            attributeMap.put("lastModifiedTime", fileAttributes.lastModifiedTime());
            break;
          case "lastAccessTime":
            attributeMap.put("lastAccessTime", fileAttributes.lastAccessTime());
            break;
          case "isDirectory":
            attributeMap.put("isDirectory", fileAttributes.isDirectory());
            break;
          case "isRegularFile":
            attributeMap.put("isRegularFile", fileAttributes.isRegularFile());
            break;
          case "isSymbolicLink":
            attributeMap.put("isSymbolicLink", fileAttributes.isSymbolicLink());
            break;
          case "isOther":
            attributeMap.put("isOther", fileAttributes.isOther());
            break;
          case "size":
            attributeMap.put("size", fileAttributes.size());
            break;
          case "fileKey":
            attributeMap.put("fileKey", fileAttributes.fileKey());
            break;
          default:
            throw new IllegalArgumentException("Unrecognized attribute(" + attributeName + ")");
        }
      }

      return attributeMap;
    }
  }

  /**
   * Sets a single attribute of a file.
   *
   * @param path      the file to modify
   * @param attribute the attribute name, optionally prefixed by a view name
   * @param value     the value to set
   * @param options   the link options
   * @throws NoSuchFileException           if the file does not exist
   * @throws IllegalArgumentException      if the attribute is not recognised or not writable
   * @throws UnsupportedOperationException if the named view is not supported
   * @throws IOException                   if a symbolic link cycle is encountered
   */
  public void setAttribute (EphemeralPath path, String attribute, Object value, LinkOption... options)
    throws IOException {

    ensureOpen();

    HeapNode heapNode;
    String attributeName;
    String viewName;
    int colonPos;

    if ((colonPos = attribute.indexOf(':')) >= 0) {
      if (!SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsKey(viewName = attribute.substring(0, colonPos))) {
        throw new UnsupportedOperationException(viewName);
      }
      attributeName = attribute.substring(colonPos + 1);
    } else {
      attributeName = attribute;
    }

    if ((heapNode = findNode(path, options)) == null) {
      throw new NoSuchFileException(path.toString());
    } else {

      EphemeralBasicFileAttributes fileAttributes = heapNode.getAttributes();

      switch (attributeName) {
        case "creationTime":
          fileAttributes.setCreationTime((FileTime)value);
          break;
        case "lastModifiedTime":
          fileAttributes.setLastModifiedTime((FileTime)value);
          break;
        case "lastAccessTime":
          fileAttributes.setLastAccessTime((FileTime)value);
          break;
        default:
          throw new IllegalArgumentException("Unrecognized or read-only attribute(" + attributeName + ")");
      }
    }
  }

  @Override
  public Object getAttribute (String attribute)
    throws IOException {

    ensureOpen();

    int colonPos;

    if ((colonPos = attribute.indexOf(':')) < 0) {
      throw new IllegalArgumentException(attribute);
    } else {
      if (fileStoreAttributeView.name().equals(attribute.substring(0, colonPos))) {
        try {

          return fileStoreAttributeView.getClass().getDeclaredField(attribute.substring(colonPos + 1)).get(fileStoreAttributeView);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
          throw new IOException(exception);
        }
      } else {

        return null;
      }
    }
  }

  /**
   * Registers a listener for changes to the entries of a directory.
   *
   * @param path     the directory to watch
   * @param listener the listener to notify
   * @throws NoSuchFileException   if the directory does not exist
   * @throws NotDirectoryException if the path does not name a directory
   * @throws IOException           if a symbolic link cycle is encountered
   */
  public void registerHeapListener (EphemeralPath path, HeapEventListener listener)
    throws IOException {

    ensureOpen();

    HeapNode heapNode;

    if ((heapNode = findNode(path)) == null) {
      throw new NoSuchFileException(path.toString());
    } else if (!HeapNodeType.DIRECTORY.equals(heapNode.getType())) {
      throw new NotDirectoryException(path.toString());
    } else {
      heapNode.registerListener(listener);
    }
  }

  /**
   * Removes a previously registered directory listener.
   *
   * @param path     the directory being watched
   * @param listener the listener to remove
   * @throws IOException if a symbolic link cycle is encountered
   */
  public void unregisterHeapListener (EphemeralPath path, HeapEventListener listener)
    throws IOException {

    ensureOpen();

    HeapNode heapNode;

    if ((heapNode = findNode(path)) != null) {
      heapNode.unregisterListener(listener);
    }
  }

  /**
   * Verifies that a file exists and that the requested kinds of access are permitted.
   *
   * <p>The heap carries no permission bits, so read and write access is always granted. Execute
   * access is granted only for directories, which is what a POSIX file system reports for a freshly
   * created regular file.
   *
   * @param path  the file to check
   * @param modes the kinds of access required
   * @throws NoSuchFileException   if the file does not exist
   * @throws AccessDeniedException if execute access is requested for something other than a
   *                               directory
   * @throws IOException           if a symbolic link cycle is encountered
   */
  public void checkAccess (EphemeralPath path, AccessMode... modes)
    throws IOException {

    ensureOpen();

    HeapNode heapNode;

    if ((heapNode = findNode(path)) == null) {
      throw new NoSuchFileException(path.toString());
    } else if (modes != null) {
      for (AccessMode mode : modes) {
        switch (mode) {
          case READ:
          case WRITE:
            break;
          case EXECUTE:
            if (!HeapNodeType.DIRECTORY.equals(heapNode.getType())) {
              throw new AccessDeniedException(path.toString());
            }
            break;
          default:
            throw new UnknownSwitchCaseException(mode.name());
        }
      }
    }
  }

  /**
   * Returns whether two paths name the same file, which two hard links to one content do.
   *
   * @param first  the first path
   * @param second the second path
   * @return {@code true} if both paths resolve to the same file
   * @throws NoSuchFileException if either file does not exist
   * @throws IOException         if a symbolic link cycle is encountered
   */
  public boolean isSameFile (EphemeralPath first, EphemeralPath second)
    throws IOException {

    ensureOpen();

    treeLock.readLock().lock();
    try {

      HeapNode firstNode;
      HeapNode secondNode;

      if ((firstNode = resolve(canonical(first), true, 0).node()) == null) {
        throw new NoSuchFileException(first.toString());
      } else if ((secondNode = resolve(canonical(second), true, 0).node()) == null) {
        throw new NoSuchFileException(second.toString());
      } else {

        return (firstNode == secondNode) || firstNode.getAttributes().fileKey().equals(secondNode.getAttributes().fileKey());
      }
    } finally {
      treeLock.readLock().unlock();
    }
  }

  /**
   * Returns the absolute, link-free path of an existing file.
   *
   * @param path    the path to resolve
   * @param options {@link LinkOption#NOFOLLOW_LINKS} to leave a final link unresolved
   * @return the real path
   * @throws NoSuchFileException if the file does not exist
   * @throws IOException         if a symbolic link cycle is encountered
   */
  public EphemeralPath toRealPath (EphemeralPath path, LinkOption... options)
    throws IOException {

    ensureOpen();

    treeLock.readLock().lock();
    try {

      Resolution resolution = resolve(canonical(path), !isNoFollow(options), 0);

      if (resolution.node() == null) {
        throw new NoSuchFileException(path.toString());
      }

      return resolution.path();
    } finally {
      treeLock.readLock().unlock();
    }
  }

  /**
   * Opens a stream over the entries of a directory.
   *
   * @param dir     the directory to list
   * @param filter  the filter deciding which entries to include, or {@code null} for all
   * @param options the link options
   * @return a {@link SecureDirectoryStream} over the directory
   * @throws NoSuchFileException   if the directory does not exist
   * @throws NotDirectoryException if the path does not name a directory
   * @throws IOException           if a symbolic link cycle is encountered
   */
  public SecureDirectoryStream<Path> newDirectoryStream (EphemeralPath dir, DirectoryStream.Filter<? super Path> filter, LinkOption... options)
    throws IOException {

    ensureOpen();

    HeapNode heapNode;

    if ((heapNode = findNode(dir, options)) == null) {
      throw new NoSuchFileException(dir.toString());
    } else if (!HeapNodeType.DIRECTORY.equals(heapNode.getType())) {
      throw new NotDirectoryException(dir.toString());
    } else {

      EphemeralDirectoryStream directoryStream = new EphemeralDirectoryStream(this, fileSystem.provider(), canonical(dir), (DirectoryNode)heapNode, filter);

      registerOpenResource(directoryStream);

      return directoryStream;
    }
  }

  /**
   * Rejects any file attribute this store cannot honour at creation time.
   *
   * @param attrs the attributes supplied by the caller
   * @throws UnsupportedOperationException if an attribute other than POSIX permissions is supplied
   */
  private void checkCreationAttributes (FileAttribute<?>... attrs) {

    if (attrs != null) {
      for (FileAttribute<?> attribute : attrs) {
        if (!"posix:permissions".equals(attribute.name())) {
          throw new UnsupportedOperationException("Only posix permission file attributes are supported");
        }
      }
    }
  }

  /**
   * Locates the directory that will hold a new entry, and the name it will take.
   *
   * @param path the path of the entry to be created
   * @return the parent directory
   * @throws NoSuchFileException   if the parent does not exist
   * @throws NotDirectoryException if the parent is not a directory
   * @throws IOException           if a symbolic link cycle is encountered
   */
  private DirectoryNode findParent (EphemeralPath path)
    throws IOException {

    EphemeralPath parentPath;
    HeapNode parentNode;

    if ((parentPath = path.getParent()) == null) {
      throw new FileAlreadyExistsException(path.toString());
    } else if ((parentNode = resolve(parentPath, true, 0).node()) == null) {
      throw new NoSuchFileException(parentPath.toString());
    } else if (!HeapNodeType.DIRECTORY.equals(parentNode.getType())) {
      throw new NotDirectoryException(parentPath.toString());
    } else {

      return (DirectoryNode)parentNode;
    }
  }

  /**
   * Creates a directory.
   *
   * @param path  the directory to create
   * @param attrs the attributes to apply
   * @throws FileAlreadyExistsException if something already exists at the path
   * @throws NoSuchFileException        if the parent does not exist
   * @throws IOException                if a symbolic link cycle is encountered
   */
  public void createDirectory (EphemeralPath path, FileAttribute<?>... attrs)
    throws IOException {

    ensureOpen();
    checkCreationAttributes(attrs);

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalPath = canonical(path);

      if (canonicalPath.getNameCount() == 0) {
        throw new FileAlreadyExistsException(canonicalPath.toString());
      } else {

        DirectoryNode parentNode = findParent(canonicalPath);
        String name = canonicalPath.getNames()[canonicalPath.getNameCount() - 1];

        if (parentNode.exists(name)) {
          throw new FileAlreadyExistsException(canonicalPath.toString());
        } else {
          parentNode.put(new DirectoryNode(parentNode, name, blockSize));
          publish(parentNode, canonicalPath, HeapEventType.CREATE);
        }
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Creates a directory and every missing directory above it, without reporting an error if it is
   * already there.
   *
   * <p>Used to seed the heap with the directories a file system is expected to have from the
   * outset. An empty heap has no working directory and no temporary directory, so relative I/O and
   * {@code Files.createTempFile} would fail until something created them by hand.
   *
   * @param path the absolute directory to create
   * @throws FileAlreadyExistsException if a non-directory already occupies part of the path
   * @throws IOException                if the directories cannot be created
   */
  public void createDirectories (EphemeralPath path)
    throws IOException {

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalPath = canonical(path);
      DirectoryNode currentNode = rootNode;

      for (String name : canonicalPath.getNames()) {

        HeapNode childNode;

        if ((childNode = currentNode.get(name)) == null) {

          DirectoryNode createdNode = new DirectoryNode(currentNode, name, blockSize);

          currentNode.put(createdNode);
          currentNode = createdNode;
        } else if (HeapNodeType.DIRECTORY.equals(childNode.getType())) {
          currentNode = (DirectoryNode)childNode;
        } else {
          throw new FileAlreadyExistsException(canonicalPath.toString());
        }
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Creates a symbolic link.
   *
   * @param link   the link to create
   * @param target the raw target of the link, which need not exist
   * @param attrs  the attributes to apply
   * @throws FileAlreadyExistsException if something already exists at the link path
   * @throws NoSuchFileException        if the parent of the link does not exist
   * @throws IOException                if a symbolic link cycle is encountered
   */
  public void createSymbolicLink (EphemeralPath link, Path target, FileAttribute<?>... attrs)
    throws IOException {

    ensureOpen();
    checkCreationAttributes(attrs);

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalLink = canonical(link);
      DirectoryNode parentNode = findParent(canonicalLink);
      String name = canonicalLink.getNames()[canonicalLink.getNameCount() - 1];

      if (parentNode.exists(name)) {
        throw new FileAlreadyExistsException(canonicalLink.toString());
      } else {
        parentNode.put(new LinkNode(parentNode, name, target.toString()));
        publish(parentNode, canonicalLink, HeapEventType.CREATE);
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Reads the target of a symbolic link.
   *
   * @param link the link to read
   * @return the raw target of the link
   * @throws NoSuchFileException if the link does not exist
   * @throws NotLinkException    if the path does not name a symbolic link
   * @throws IOException         if a symbolic link cycle is encountered
   */
  public Path readSymbolicLink (EphemeralPath link)
    throws IOException {

    ensureOpen();

    HeapNode heapNode;

    if ((heapNode = findNode(link, LinkOption.NOFOLLOW_LINKS)) == null) {
      throw new NoSuchFileException(link.toString());
    } else if (!HeapNodeType.SYMBOLIC_LINK.equals(heapNode.getType())) {
      throw new NotLinkException(link.toString());
    } else {

      return new EphemeralPath(fileSystem, ((LinkNode)heapNode).getTarget());
    }
  }

  /**
   * Creates an additional name for an existing file, sharing its content.
   *
   * @param link     the new name to create
   * @param existing the existing file to link to
   * @throws FileAlreadyExistsException if something already exists at the link path
   * @throws NoSuchFileException        if the existing file does not exist
   * @throws FileSystemException        if the existing path names a directory
   * @throws IOException                if a symbolic link cycle is encountered
   */
  public void createLink (EphemeralPath link, EphemeralPath existing)
    throws IOException {

    ensureOpen();

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalLink = canonical(link);
      HeapNode existingNode;

      if ((existingNode = resolve(canonical(existing), true, 0).node()) == null) {
        throw new NoSuchFileException(existing.toString());
      } else if (!HeapNodeType.FILE.equals(existingNode.getType())) {
        throw new FileSystemException(existing.toString(), null, "Only a regular file can be hard linked");
      } else {

        DirectoryNode parentNode = findParent(canonicalLink);
        String name = canonicalLink.getNames()[canonicalLink.getNameCount() - 1];

        if (parentNode.exists(name)) {
          throw new FileAlreadyExistsException(canonicalLink.toString());
        } else {

          HeapFileContent content = ((FileNode)existingNode).getContent();

          content.link();
          parentNode.put(new FileNode(parentNode, name, content));
          publish(parentNode, canonicalLink, HeapEventType.CREATE);
        }
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Deletes a file, directory, or symbolic link. A symbolic link is removed rather than followed.
   *
   * @param path the entry to delete
   * @throws NoSuchFileException        if the entry does not exist
   * @throws DirectoryNotEmptyException if the entry is a directory with children
   * @throws IOException                if the entry is the root, or a link cycle is encountered
   */
  public void delete (EphemeralPath path)
    throws IOException {

    ensureOpen();

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalPath = canonical(path);
      HeapNode heapNode;

      if (canonicalPath.getNameCount() == 0) {
        throw new IOException("The root directory may not be deleted");
      } else if ((heapNode = resolve(canonicalPath, false, 0).node()) == null) {
        throw new NoSuchFileException(canonicalPath.toString());
      } else {
        removeEntry(heapNode, canonicalPath);
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Unlinks a node from its parent, releasing its content when no other name refers to it.
   *
   * <p>Must be called while holding the write lock.
   *
   * @param heapNode the node to unlink
   * @param path     the absolute path the node occupies
   * @throws DirectoryNotEmptyException if the node is a directory with children
   * @throws IOException                if the node has no parent
   */
  private void removeEntry (HeapNode heapNode, EphemeralPath path)
    throws IOException {

    DirectoryNode parentNode;

    if ((parentNode = heapNode.getParent()) == null) {
      throw new IOException("The root directory may not be deleted");
    } else if (HeapNodeType.DIRECTORY.equals(heapNode.getType()) && (!((DirectoryNode)heapNode).isEmpty())) {
      throw new DirectoryNotEmptyException(path.toString());
    } else {
      parentNode.remove(heapNode.getName());

      if (HeapNodeType.FILE.equals(heapNode.getType())) {
        // the content outlives this name only if another name is hard linked to it
        ((FileNode)heapNode).getContent().unlink();
      }

      publish(parentNode, path, HeapEventType.DELETE);
      // a watcher registered on the entry itself learns that its registration is now moot
      heapNode.fire(new HeapEvent(this, path, HeapEventType.DELETE));
    }
  }

  /**
   * Clears the slot a copy or move is about to occupy, and returns the directory that will hold it.
   *
   * <p>Must be called while holding the write lock. This is the shared implementation of the
   * replacement rules {@code java.nio.file} defines: a target that already exists is an error
   * unless {@link StandardCopyOption#REPLACE_EXISTING} was given, and a target directory can only
   * be replaced while it is empty.
   *
   * @param target          the destination path
   * @param replaceExisting whether an existing destination may be replaced
   * @return the directory that will hold the new entry
   * @throws FileAlreadyExistsException if the destination exists and may not be replaced
   * @throws DirectoryNotEmptyException if the destination is a non-empty directory
   * @throws NoSuchFileException        if the destination's parent does not exist
   * @throws IOException                if a symbolic link cycle is encountered
   */
  private DirectoryNode clearTarget (EphemeralPath target, boolean replaceExisting)
    throws IOException {

    HeapNode targetNode;

    if ((targetNode = resolve(target, false, 0).node()) != null) {
      if (!replaceExisting) {
        throw new FileAlreadyExistsException(target.toString());
      } else {
        removeEntry(targetNode, target);
      }
    }

    return findParent(target);
  }

  /**
   * Copies a file, an empty directory, or a symbolic link to a new path.
   *
   * <p>A directory is copied as an empty directory, which is what {@code java.nio.file} specifies:
   * {@link java.nio.file.Files#copy} copies one entry, and walking a tree is the caller's job.
   *
   * @param source  the entry to copy
   * @param target  the destination path
   * @param options the copy options
   * @throws NoSuchFileException           if the source does not exist
   * @throws FileAlreadyExistsException    if the destination exists and may not be replaced
   * @throws UnsupportedOperationException if an unsupported option is supplied
   * @throws IOException                   if the copy cannot be performed
   */
  public void copy (EphemeralPath source, EphemeralPath target, CopyOption... options)
    throws IOException {

    ensureOpen();

    boolean replaceExisting = false;
    boolean copyAttributes = false;
    boolean noFollowLinks = false;

    if (options != null) {
      for (CopyOption option : options) {
        if (StandardCopyOption.REPLACE_EXISTING.equals(option)) {
          replaceExisting = true;
        } else if (StandardCopyOption.COPY_ATTRIBUTES.equals(option)) {
          copyAttributes = true;
        } else if (LinkOption.NOFOLLOW_LINKS.equals(option)) {
          noFollowLinks = true;
        } else if (StandardCopyOption.ATOMIC_MOVE.equals(option)) {
          throw new UnsupportedOperationException(StandardCopyOption.ATOMIC_MOVE.name());
        } else {
          throw new UnsupportedOperationException(String.valueOf(option));
        }
      }
    }

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalSource = canonical(source);
      EphemeralPath canonicalTarget = canonical(target);
      HeapNode sourceNode;

      if ((sourceNode = resolve(canonicalSource, !noFollowLinks, 0).node()) == null) {
        throw new NoSuchFileException(canonicalSource.toString());
      } else if (canonicalSource.equals(canonicalTarget)) {
        // copying a file onto itself is specified to do nothing
      } else {

        DirectoryNode parentNode = clearTarget(canonicalTarget, replaceExisting);
        String name = canonicalTarget.getNames()[canonicalTarget.getNameCount() - 1];
        HeapNode copiedNode;

        switch (sourceNode.getType()) {
          case FILE:
            copiedNode = new FileNode(parentNode, name, new HeapFileContent(((FileNode)sourceNode).getContent()));
            break;
          case DIRECTORY:
            copiedNode = new DirectoryNode(parentNode, name, blockSize);
            break;
          case SYMBOLIC_LINK:
            copiedNode = new LinkNode(parentNode, name, ((LinkNode)sourceNode).getTarget());
            break;
          default:
            throw new UnknownSwitchCaseException(sourceNode.getType().name());
        }

        if (copyAttributes) {
          copiedNode.getAttributes().setCreationTime(sourceNode.getAttributes().creationTime());
          copiedNode.getAttributes().setLastModifiedTime(sourceNode.getAttributes().lastModifiedTime());
          copiedNode.getAttributes().setLastAccessTime(sourceNode.getAttributes().lastAccessTime());
        }

        parentNode.put(copiedNode);
        publish(parentNode, canonicalTarget, HeapEventType.CREATE);
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Moves a file, directory, or symbolic link to a new path.
   *
   * <p>The node is re-parented rather than rebuilt, so a directory arrives with its entire subtree
   * intact and the cost of the move does not depend on how much lives beneath it. A symbolic link
   * is moved as a link, not as its target.
   *
   * @param source  the entry to move
   * @param target  the destination path
   * @param options the copy options
   * @throws NoSuchFileException           if the source does not exist
   * @throws FileAlreadyExistsException    if the destination exists and may not be replaced
   * @throws UnsupportedOperationException if an unsupported option is supplied
   * @throws IOException                   if the move cannot be performed
   */
  public void move (EphemeralPath source, EphemeralPath target, CopyOption... options)
    throws IOException {

    ensureOpen();

    boolean replaceExisting = false;

    if (options != null) {
      for (CopyOption option : options) {
        if (StandardCopyOption.REPLACE_EXISTING.equals(option)) {
          replaceExisting = true;
        } else if (!(StandardCopyOption.ATOMIC_MOVE.equals(option) || StandardCopyOption.COPY_ATTRIBUTES.equals(option) || LinkOption.NOFOLLOW_LINKS.equals(option))) {
          throw new UnsupportedOperationException(String.valueOf(option));
        }
        // a move within one heap re-parents a node under the write lock, so it is already atomic,
        // and it carries its own attributes and its own link target along with it
      }
    }

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalSource = canonical(source);
      EphemeralPath canonicalTarget = canonical(target);
      HeapNode sourceNode;

      if ((sourceNode = resolve(canonicalSource, false, 0).node()) == null) {
        throw new NoSuchFileException(canonicalSource.toString());
      } else if (canonicalSource.equals(canonicalTarget)) {
        // moving a file onto itself is specified to do nothing
      } else if (canonicalSource.getNameCount() == 0) {
        throw new IOException("The root directory may not be moved");
      } else if (HeapNodeType.DIRECTORY.equals(sourceNode.getType()) && canonicalTarget.startsWith(canonicalSource)) {
        throw new IOException("A directory may not be moved beneath itself");
      } else {

        DirectoryNode formerParentNode = sourceNode.getParent();
        DirectoryNode parentNode = clearTarget(canonicalTarget, replaceExisting);
        String name = canonicalTarget.getNames()[canonicalTarget.getNameCount() - 1];

        formerParentNode.remove(sourceNode.getName());
        publish(formerParentNode, canonicalSource, HeapEventType.DELETE);

        sourceNode.relink(parentNode, name);
        parentNode.put(sourceNode);
        publish(parentNode, canonicalTarget, HeapEventType.CREATE);
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Resolves, and if required creates, the file a channel is about to be opened on.
   *
   * @param path   the file to open
   * @param parsed the validated open options
   * @param attrs  the attributes to apply to a newly created file
   * @return the file node the channel will address
   * @throws NoSuchFileException        if the file does not exist and is not to be created
   * @throws FileAlreadyExistsException if the file exists and {@code CREATE_NEW} was requested
   * @throws IOException                if the path names a directory, or a link cycle is found
   */
  private FileNode openFileNode (EphemeralPath path, OpenOptions parsed, FileAttribute<?>... attrs)
    throws IOException {

    checkCreationAttributes(attrs);

    treeLock.writeLock().lock();
    try {

      EphemeralPath canonicalPath = canonical(path);
      HeapNode heapNode;

      if (canonicalPath.getNameCount() == 0) {
        throw new IOException("A directory may not be opened as a file");
      } else if ((heapNode = resolve(canonicalPath, !parsed.noFollowLinks, 0).node()) == null) {
        if (!(parsed.write && (parsed.create || parsed.createNew))) {
          throw new NoSuchFileException(canonicalPath.toString());
        } else {

          DirectoryNode parentNode = findParent(canonicalPath);
          String name = canonicalPath.getNames()[canonicalPath.getNameCount() - 1];
          FileNode fileNode = new FileNode(parentNode, name, new HeapFileContent(this, blockSize));

          parentNode.put(fileNode);
          publish(parentNode, canonicalPath, HeapEventType.CREATE);

          return fileNode;
        }
      } else if (parsed.createNew) {
        throw new FileAlreadyExistsException(canonicalPath.toString());
      } else if (!HeapNodeType.FILE.equals(heapNode.getType())) {
        throw new IOException("A directory may not be opened as a file");
      } else {
        if (parsed.write && parsed.truncateExisting) {
          ((FileNode)heapNode).getContent().truncate(0);
          touchModified(heapNode);
          publish(heapNode.getParent(), canonicalPath, HeapEventType.MODIFY);
        }

        return (FileNode)heapNode;
      }
    } finally {
      treeLock.writeLock().unlock();
    }
  }

  /**
   * Opens a byte channel over a file.
   *
   * @param path    the file to open
   * @param options the open options
   * @param attrs   the attributes to apply to a newly created file
   * @return a channel positioned at the start of the file, or at its end when appending
   * @throws IOException if the file cannot be opened
   */
  public SeekableByteChannel newByteChannel (EphemeralPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    ensureOpen();

    OpenOptions parsed = parseOpenOptions(options);
    EphemeralSeekableByteChannel channel = new EphemeralSeekableByteChannel(this, openFileNode(path, parsed, attrs), canonical(path), parsed);

    registerOpenResource(channel);

    return channel;
  }

  /**
   * Opens a file channel over a file.
   *
   * @param path    the file to open
   * @param options the open options
   * @param attrs   the attributes to apply to a newly created file
   * @return a channel positioned at the start of the file, or at its end when appending
   * @throws IOException if the file cannot be opened
   */
  public FileChannel newFileChannel (EphemeralPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    ensureOpen();

    OpenOptions parsed = parseOpenOptions(options);
    EphemeralFileChannel channel = new EphemeralFileChannel(this, openFileNode(path, parsed, attrs), canonical(path), parsed);

    registerOpenResource(channel);

    return channel;
  }

  /**
   * Records that a node's content changed, and tells the containing directory's watchers.
   *
   * @param heapNode the node that changed
   * @param path     the absolute path of the node
   */
  void reportModified (HeapNode heapNode, EphemeralPath path) {

    touchModified(heapNode);
    publish(heapNode.getParent(), path, HeapEventType.MODIFY);
  }

  /**
   * Stamps a node's modification and access times with the current time.
   *
   * @param heapNode the node that changed
   */
  private void touchModified (HeapNode heapNode) {

    FileTime now = FileTime.fromMillis(System.currentTimeMillis());

    heapNode.getAttributes().setLastModifiedTime(now);
    heapNode.getAttributes().setLastAccessTime(now);
  }

  /**
   * Stamps a node's access time with the current time.
   *
   * @param heapNode the node that was read
   */
  void reportAccessed (HeapNode heapNode) {

    heapNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));
  }

  /**
   * The outcome of resolving a path: the link-free absolute path, and the node found there.
   */
  private record Resolution(EphemeralPath path, HeapNode node) {

  }

  /**
   * The open options that govern a single channel, after validation.
   */
  static class OpenOptions {

    private boolean read;
    private boolean write;
    private boolean append;
    private boolean truncateExisting;
    private boolean createNew;
    private boolean create;
    private boolean deleteOnClose;
    private boolean noFollowLinks;

    boolean isRead () {

      return read;
    }

    boolean isWrite () {

      return write;
    }

    boolean isAppend () {

      return append;
    }

    boolean isDeleteOnClose () {

      return deleteOnClose;
    }
  }
}
