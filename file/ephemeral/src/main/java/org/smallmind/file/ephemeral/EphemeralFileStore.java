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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.smallmind.file.ephemeral.heap.DirectoryNode;
import org.smallmind.file.ephemeral.heap.FileNode;
import org.smallmind.file.ephemeral.heap.HeapEvent;
import org.smallmind.file.ephemeral.heap.HeapEventListener;
import org.smallmind.file.ephemeral.heap.HeapEventType;
import org.smallmind.file.ephemeral.heap.HeapNode;
import org.smallmind.file.ephemeral.heap.HeapNodeType;
import org.smallmind.nutsnbolts.io.ByteArrayIOBuffer;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * In-memory {@link FileStore} implementation that backs the ephemeral file system. All file
 * and directory data are stored in a heap tree rooted at an anonymous {@link DirectoryNode}.
 * The store enforces a logical capacity ceiling and uses a fixed block size when allocating
 * new file nodes. All mutating operations are {@code synchronized} to guard against concurrent
 * access.
 */
public class EphemeralFileStore extends FileStore {

  private static final Map<String, Class<? extends FileAttributeView>> SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP = Map.of("basic", BasicFileAttributeView.class);
  private static final String[] BASIC_FILE_ATTRIBUTE_NAMES = new String[] {"creationTime", "lastModifiedTime", "lastAccessTime", "isDirectory", "isRegularFile", "isSymbolicLink", "isOther", "size", "fileKey"};
  private final EphemeralFileSystem fileSystem;
  private final EphemeralFileStoreAttributeView fileStoreAttributeView = new EphemeralFileStoreAttributeView();
  private final DirectoryNode rootNode = new DirectoryNode(null, null);
  private final long capacity;
  private final int blockSize;

  /**
   * Creates a file store bound to the given file system.
   *
   * @param fileSystem the owning {@link EphemeralFileSystem}
   * @param capacity   the maximum capacity in bytes that will be reported by {@link #getUsableSpace()}
   * @param blockSize  the allocation unit in bytes used when creating new file nodes
   * @throws IllegalArgumentException if {@code capacity} or {@code blockSize} are not positive
   */
  public EphemeralFileStore (EphemeralFileSystem fileSystem, long capacity, int blockSize) {

    if ((capacity <= 0) || (blockSize <= 0)) {
      throw new IllegalArgumentException("Both capacity and block size must be > 0");
    }

    this.fileSystem = fileSystem;
    this.capacity = capacity;
    this.blockSize = blockSize;
  }

  /**
   * Removes all child nodes from the root, effectively resetting the store to an empty state.
   */
  public void clear () {

    rootNode.clear();
  }

  /**
   * Returns the name of this file store.
   *
   * @return the simple class name of this store
   */
  @Override
  public String name () {

    return EphemeralFileStore.class.getSimpleName();
  }

  /**
   * Returns the type identifier of this file store, which is the same as its name.
   *
   * @return the type string; never {@code null}
   */
  @Override
  public String type () {

    return name();
  }

  /**
   * Indicates whether this file store is read-only.
   *
   * @return always {@code false}
   */
  @Override
  public boolean isReadOnly () {

    return false;
  }

  /**
   * Returns the total size of this file store, which is equal to the usable space.
   *
   * @return the configured capacity in bytes
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public long getTotalSpace () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return getUsableSpace();
    }
  }

  /**
   * Returns the number of bytes available for use on this file store.
   *
   * @return the configured capacity in bytes
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public long getUsableSpace () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return capacity;
    }
  }

  /**
   * Returns the number of bytes not yet allocated in this file store, calculated as
   * {@code capacity - rootNode.size()}.
   *
   * @return unallocated bytes remaining
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public synchronized long getUnallocatedSpace () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return capacity - rootNode.size();
    }
  }

  /**
   * Indicates whether this file store supports the attribute view identified by the given class.
   *
   * @param type the attribute view class to test
   * @return {@code true} if the view is supported
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public boolean supportsFileAttributeView (Class<? extends FileAttributeView> type) {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsValue(type);
    }
  }

  /**
   * Indicates whether this file store supports the attribute view identified by the given name.
   *
   * @param name the attribute view name to test (e.g., {@code "basic"})
   * @return {@code true} if the named view is supported
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public boolean supportsFileAttributeView (String name) {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsKey(name);
    }
  }

  /**
   * Returns the set of attribute view names supported by this store.
   *
   * @return an unmodifiable set of supported view name strings
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  public Set<String> getSupportedFileAttributeViewNames () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.keySet();
    }
  }

  /**
   * Returns a file-store-level attribute view of the requested type, or {@code null} when
   * the type is not {@link EphemeralFileStoreAttributeView}.
   *
   * @param <V>  the view type
   * @param type the class of the desired view
   * @return the view instance, or {@code null} when unsupported
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView (Class<V> type) {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return EphemeralFileStoreAttributeView.class.equals(type) ? type.cast(fileStoreAttributeView) : null;
    }
  }

  /**
   * Returns a file-attribute view of the requested type for the specified path.
   *
   * @param <V>     the view type
   * @param path    the path whose attributes are requested
   * @param type    the class of the desired view
   * @param options link options (currently unused)
   * @return the view instance, or {@code null} when the type is unsupported or the path does
   * not exist
   * @throws NoSuchFileException       if a non-terminal path component does not exist
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  public synchronized <V extends FileAttributeView> V getFileAttributeView (EphemeralPath path, Class<V> type, LinkOption... options)
    throws NoSuchFileException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else if (!SUPPORTED_FILE_ATTRIBUTE_VIEW_MAP.containsValue(type)) {

      return null;
    } else {

      HeapNode heapNode;

      return ((heapNode = findNode(path)) != null) ? type.cast(new EphemeralBasicFileAttributeView(heapNode.getAttributes())) : null;
    }
  }

  /**
   * Reads basic file attributes for the specified path.
   *
   * @param <A>     the attribute type
   * @param path    the path whose attributes are to be read
   * @param type    the expected attribute class; must be assignable from
   *                {@link EphemeralBasicFileAttributes}
   * @param options link options (currently unused)
   * @return the attributes, or {@code null} if the path does not exist
   * @throws NoSuchFileException           if a non-terminal path component does not exist
   * @throws UnsupportedOperationException if {@code type} is not assignable from
   *                                       {@link EphemeralBasicFileAttributes}
   * @throws ClosedFileSystemException     if the owning file system has been closed
   */
  public synchronized <A extends BasicFileAttributes> A readAttributes (EphemeralPath path, Class<A> type, LinkOption... options)
    throws NoSuchFileException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else if (!type.isAssignableFrom(EphemeralBasicFileAttributes.class)) {
      throw new UnsupportedOperationException(type.getName());
    } else {
      HeapNode heapNode;

      return ((heapNode = findNode(path)) != null) ? type.cast(heapNode.getAttributes()) : null;
    }
  }

  /**
   * Reads a selected subset of basic file attributes, specified by name, for the given path.
   * The {@code attributes} string may optionally be prefixed with a view name followed by a
   * colon (e.g., {@code "basic:size,isDirectory"}). An asterisk ({@code *}) in the name list
   * selects all known attribute names.
   *
   * @param path       the path whose attributes are to be read
   * @param attributes the comma-separated attribute name selection, optionally prefixed with
   *                   a view name and colon
   * @param options    link options (currently unused)
   * @return a map from attribute name to value for the selected attributes
   * @throws NoSuchFileException           if the path does not exist
   * @throws UnsupportedOperationException if an unsupported view name is specified
   * @throws ClosedFileSystemException     if the owning file system has been closed
   */
  public synchronized Map<String, Object> readAttributes (EphemeralPath path, String attributes, LinkOption... options)
    throws NoSuchFileException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

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

      if ((heapNode = findNode(path)) == null) {
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
          }
        }

        return attributeMap;
      }
    }
  }

  /**
   * Sets a single basic file attribute for the given path. The {@code attribute} string may
   * optionally be prefixed with a view name and colon (e.g., {@code "basic:creationTime"}).
   * The supported settable attributes are {@code creationTime}, {@code lastModifiedTime}, and
   * {@code lastAccessTime}, each expecting a {@link FileTime} value.
   *
   * @param path      the path whose attribute is to be set
   * @param attribute the attribute name, optionally prefixed with a view name and colon
   * @param value     the new attribute value
   * @param options   link options (currently unused)
   * @throws NoSuchFileException           if the path does not exist
   * @throws UnsupportedOperationException if an unsupported view name is specified
   * @throws ClosedFileSystemException     if the owning file system has been closed
   */
  public synchronized void setAttribute (EphemeralPath path, String attribute, Object value, LinkOption... options)
    throws NoSuchFileException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

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

      if ((heapNode = findNode(path)) == null) {
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
        }
      }
    }
  }

  /**
   * Reads a file-store-level attribute by its qualified name ({@code viewName:attributeName}).
   *
   * @param attribute the qualified attribute name, which must contain a colon separator
   * @return the attribute value, or {@code null} if the view name does not match
   * @throws IOException               if the attribute field cannot be accessed via reflection
   * @throws IllegalArgumentException  if the attribute string does not contain a colon
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  @Override
  public Object getAttribute (String attribute)
    throws IOException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

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
  }

  /**
   * Registers a {@link HeapEventListener} on the directory node at the given path so that the
   * listener is notified of heap changes within that directory.
   *
   * @param path     the directory path to which the listener should be attached
   * @param listener the listener to register
   * @throws NoSuchFileException       if the path does not exist
   * @throws NotDirectoryException     if the path exists but is not a directory
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  public synchronized void registerHeapListener (EphemeralPath path, HeapEventListener listener)
    throws NoSuchFileException, NotDirectoryException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      HeapNode node;

      if (((node = findNode(path)) == null) || (!HeapNodeType.DIRECTORY.equals(node.getType()))) {
        throw new NotDirectoryException(path.toString());
      } else {
        node.registerListener(listener);
      }
    }
  }

  /**
   * Removes a previously registered {@link HeapEventListener} from the directory node at
   * the given path. If the path does not exist the method returns silently.
   *
   * @param path     the directory path from which the listener should be removed
   * @param listener the listener to unregister
   * @throws NoSuchFileException       if a non-terminal component of the path does not exist
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  public synchronized void unregisterHeapListener (EphemeralPath path, HeapEventListener listener)
    throws NoSuchFileException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      HeapNode node;

      if ((node = findNode(path)) != null) {
        node.unregisterListener(listener);
      }
    }
  }

  /**
   * Verifies that the given path exists in the heap.
   *
   * @param path the path to check
   * @throws NoSuchFileException if the path does not exist in the heap
   */
  public synchronized void checkAccess (EphemeralPath path)
    throws NoSuchFileException {

    if (findNode(path) == null) {
      throw new NoSuchFileException(path.toString());
    }
  }

  /**
   * Opens a {@link SecureDirectoryStream} for the directory at the given path.
   *
   * @param dir     the directory path to open
   * @param filter  an optional filter applied when iterating entries; may be {@code null}
   * @param options link options (currently unused)
   * @return a new {@link SecureDirectoryStream} for the directory
   * @throws NoSuchFileException       if the path does not exist
   * @throws NotDirectoryException     if the path is not a directory
   * @throws ClosedFileSystemException if the owning file system has been closed
   */
  public synchronized SecureDirectoryStream<Path> newDirectoryStream (EphemeralPath dir, DirectoryStream.Filter<? super Path> filter, LinkOption... options)
    throws NoSuchFileException, NotDirectoryException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      HeapNode heapNode;

      if (((heapNode = findNode(dir)) == null) || (!HeapNodeType.DIRECTORY.equals(heapNode.getType()))) {
        throw new NotDirectoryException(dir.toString());
      } else {

        return new EphemeralDirectoryStream(fileSystem.provider(), dir, (DirectoryNode)heapNode, filter);
      }
    }
  }

  /**
   * Creates a new directory at the specified path.
   *
   * @param path  the path of the directory to create
   * @param attrs optional file attributes; only {@code "posix:permissions"} is accepted
   * @throws NoSuchFileException           if the parent directory does not exist, or if a file
   *                                       occupies an intermediate path component
   * @throws FileAlreadyExistsException    if a node already exists at the given path
   * @throws UnsupportedOperationException if any attribute other than {@code "posix:permissions"}
   *                                       is supplied
   * @throws ClosedFileSystemException     if the owning file system has been closed
   */
  public synchronized void createDirectory (EphemeralPath path, FileAttribute<?>... attrs)
    throws NoSuchFileException, FileAlreadyExistsException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      for (FileAttribute<?> attribute : attrs) {
        if (!"posix:permissions".equals(attribute.name())) {
          throw new UnsupportedOperationException("Only posix permission file attributes are supported");
        }
      }

      if (path.getNameCount() > 0) {

        EphemeralPath parentPath;
        HeapNode parentNode;

        if ((parentNode = findNode(parentPath = path.getParent())) == null) {
          throw new NoSuchFileException(parentPath.toString());
        } else {
          switch (parentNode.getType()) {
            case FILE:
              throw new NoSuchFileException(path.toString());
            case DIRECTORY:
              if (((DirectoryNode)parentNode).exists(path.getNames()[path.getNameCount() - 1])) {
                throw new FileAlreadyExistsException(path.toString());
              } else {

                DirectoryNode createdDirectory = new DirectoryNode((DirectoryNode)parentNode, path.getNames()[path.getNameCount() - 1]);

                ((DirectoryNode)parentNode).put(createdDirectory);
                createdDirectory.bubble(new HeapEvent(this, path, HeapEventType.CREATE));
              }
              break;
            default:
              throw new UnknownSwitchCaseException(parentNode.getType().name());
          }
        }
      }
    }
  }

  /**
   * Deletes the file or directory at the given path.
   *
   * @param path the path to delete
   * @throws IOException                if deletion of the root is attempted, or another I/O error
   *                                    occurs
   * @throws NoSuchFileException        if the path does not exist
   * @throws DirectoryNotEmptyException if the path is a non-empty directory
   * @throws ClosedFileSystemException  if the owning file system has been closed
   */
  public synchronized void delete (EphemeralPath path)
    throws IOException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      HeapNode heapNode;

      if (path.getNameCount() == 0) {
        throw new IOException("Not allowed");
      } else if ((heapNode = findNode(path)) == null) {
        throw new NoSuchFileException(path.toString());
      } else {
        switch (heapNode.getType()) {
          case FILE:
            heapNode.getParent().remove(heapNode.getName());
            heapNode.bubble(new HeapEvent(this, path, HeapEventType.DELETE));
            break;
          case DIRECTORY:
            if (!((DirectoryNode)heapNode).isEmpty()) {
              throw new DirectoryNotEmptyException(path.toString());
            } else {
              heapNode.getParent().remove(heapNode.getName());
              heapNode.bubble(new HeapEvent(this, path, HeapEventType.DELETE));
            }
            break;
          default:
            throw new UnknownSwitchCaseException(heapNode.getType().name());
        }
      }
    }
  }

  /**
   * Copies a file or directory from {@code source} to {@code target}. When both paths refer
   * to the same location the operation is a no-op. The {@link StandardCopyOption#REPLACE_EXISTING}
   * option controls whether an existing target may be overwritten. File content is copied via
   * a new {@link ByteArrayIOBuffer} snapshot.
   *
   * @param source  the source path
   * @param target  the target path
   * @param options copy options; only {@link StandardCopyOption} values are accepted
   * @throws NoSuchFileException           if the source, or (when target is absent) the parent
   *                                       of the target, does not exist
   * @throws FileAlreadyExistsException    if the target already exists and
   *                                       {@link StandardCopyOption#REPLACE_EXISTING} was not
   *                                       specified
   * @throws DirectoryNotEmptyException    if the target is a non-empty directory
   * @throws UnsupportedOperationException if a non-standard copy option is provided
   * @throws IOException                   if the root directory is the target of a directory copy,
   *                                       or another I/O error occurs
   */
  public synchronized void copy (EphemeralPath source, EphemeralPath target, CopyOption... options)
    throws IOException {

    if (!source.equals(target)) {

      HeapNode sourceNode;
      boolean replaceExisting = false;

      for (CopyOption option : options) {
        if (!(option instanceof StandardCopyOption)) {
          throw new UnsupportedOperationException("Only standard open options are supported");
        } else if (StandardCopyOption.REPLACE_EXISTING.equals(option)) {
          replaceExisting = true;
        }
      }

      if ((sourceNode = findNode(source)) == null) {
        throw new NoSuchFileException(source.toString());
      } else {

        HeapNode targetNode;
        HeapNode parentOfTargetNode = null;

        if ((targetNode = findNode(target)) == null) {

          EphemeralPath parentOfTargetPath;

          if ((parentOfTargetNode = findNode(parentOfTargetPath = target.getParent())) == null) {
            throw new NoSuchFileException(parentOfTargetPath.toString());
          }
        }

        switch (sourceNode.getType()) {
          case FILE:
            if (targetNode != null) {
              switch (targetNode.getType()) {
                case FILE:
                  if (!replaceExisting) {
                    throw new FileAlreadyExistsException(target.toString());
                  } else {

                    // all sorts of nasty race condition, need ByteArrayIOBuffer to self-encapsulate and synchronize
                    FileNode replacedFile = new FileNode(targetNode.getParent(), targetNode.getName(), new ByteArrayIOBuffer(((FileNode)sourceNode).getSegmentBuffer()));

                    targetNode.getParent().put(replacedFile);
                    replacedFile.bubble(new HeapEvent(this, target, HeapEventType.MODIFY));
                  }
                  break;
                case DIRECTORY:
                  if (((DirectoryNode)targetNode).exists(sourceNode.getName()) && (!replaceExisting)) {
                    throw new FileAlreadyExistsException(target.resolve(source.getFileName()).toString());
                  } else {

                    // all sorts of nasty race condition, need ByteArrayIOBuffer to self-encapsulate and synchronize
                    FileNode copiedFile = new FileNode((DirectoryNode)targetNode, sourceNode.getName(), new ByteArrayIOBuffer(((FileNode)sourceNode).getSegmentBuffer()));

                    ((DirectoryNode)targetNode).put(copiedFile);
                    copiedFile.bubble(new HeapEvent(this, (EphemeralPath)target.resolve(sourceNode.getName()), HeapEventType.CREATE));
                  }
                  break;
                default:
                  throw new UnknownSwitchCaseException(targetNode.getType().name());
              }
            } else {
              switch (parentOfTargetNode.getType()) {
                case FILE:
                  throw new NoSuchFileException(target.toString());
                case DIRECTORY:

                  // all sorts of nasty race condition, need ByteArrayIOBuffer to self-encapsulate and synchronize
                  FileNode createdFile = new FileNode((DirectoryNode)parentOfTargetNode, target.getNames()[target.getNameCount() - 1], new ByteArrayIOBuffer(((FileNode)sourceNode).getSegmentBuffer()));

                  ((DirectoryNode)parentOfTargetNode).put(createdFile);
                  createdFile.bubble(new HeapEvent(this, target, HeapEventType.CREATE));
                  break;
                default:
                  throw new UnknownSwitchCaseException(parentOfTargetNode.getType().name());
              }
            }
            break;
          case DIRECTORY:
            if (targetNode != null) {
              switch (targetNode.getType()) {
                case FILE:
                  throw new FileAlreadyExistsException(target.toString());
                case DIRECTORY:
                  if (targetNode.getParent() == null) {
                    throw new IOException("Not Allowed");
                  } else if (!((DirectoryNode)targetNode).isEmpty()) {
                    throw new DirectoryNotEmptyException(target.toString());
                  } else if (!replaceExisting) {
                    throw new FileAlreadyExistsException(target.toString());
                  } else {

                    DirectoryNode renamedDirectory = new DirectoryNode(targetNode.getParent(), sourceNode.getName());

                    targetNode.getParent().put(renamedDirectory);
                    targetNode.getParent().remove(targetNode.getName());

                    EphemeralPath renamedPath = (EphemeralPath)target.getParent().resolve(sourceNode.getName());

                    renamedDirectory.bubble(new HeapEvent(this, renamedPath, HeapEventType.CREATE));
                    targetNode.bubble(new HeapEvent(this, target, HeapEventType.DELETE));
                  }
                  break;
                default:
                  throw new UnknownSwitchCaseException(targetNode.getType().name());
              }
            } else {
              switch (parentOfTargetNode.getType()) {
                case FILE:
                  throw new NoSuchFileException(target.toString());
                case DIRECTORY:

                  DirectoryNode createdDirectory = new DirectoryNode((DirectoryNode)parentOfTargetNode, sourceNode.getName());

                  ((DirectoryNode)parentOfTargetNode).put(createdDirectory);
                  createdDirectory.bubble(new HeapEvent(this, (EphemeralPath)target.getParent().resolve(sourceNode.getName()), HeapEventType.CREATE));
                  break;
                default:
                  throw new UnknownSwitchCaseException(parentOfTargetNode.getType().name());
              }
            }
            break;
          default:
            throw new UnknownSwitchCaseException(sourceNode.getType().name());
        }
      }
    }
  }

  /**
   * Moves a file or directory from {@code source} to {@code target}. When both paths refer to
   * the same location the operation is a no-op. Unlike {@link #copy}, the source node is
   * removed after placement at the target. File content is transferred by reference rather than
   * being duplicated.
   *
   * @param source  the source path
   * @param target  the target path
   * @param options move options; only {@link StandardCopyOption} values are accepted
   * @throws NoSuchFileException           if the source, or (when target is absent) the parent
   *                                       of the target, does not exist
   * @throws FileAlreadyExistsException    if the target already exists and
   *                                       {@link StandardCopyOption#REPLACE_EXISTING} was not
   *                                       specified
   * @throws DirectoryNotEmptyException    if the target is a non-empty directory
   * @throws UnsupportedOperationException if a non-standard copy option is provided
   * @throws IOException                   if the root directory is the target, or another I/O
   *                                       error occurs
   */
  public synchronized void move (EphemeralPath source, EphemeralPath target, CopyOption... options)
    throws IOException {

    if (!source.equals(target)) {

      HeapNode sourceNode;
      boolean replaceExisting = false;

      for (CopyOption option : options) {
        if (!(option instanceof StandardCopyOption)) {
          throw new UnsupportedOperationException("Only standard open options are supported");
        } else if (StandardCopyOption.REPLACE_EXISTING.equals(option)) {
          replaceExisting = true;
        }
      }

      if ((sourceNode = findNode(source)) == null) {
        throw new NoSuchFileException(source.toString());
      } else {

        HeapNode targetNode;
        HeapNode parentOfTargetNode = null;

        if ((targetNode = findNode(target)) == null) {

          EphemeralPath parentOfTargetPath;

          if ((parentOfTargetNode = findNode(parentOfTargetPath = target.getParent())) == null) {
            throw new NoSuchFileException(parentOfTargetPath.toString());
          }
        }

        switch (sourceNode.getType()) {
          case FILE:
            if (targetNode != null) {
              switch (targetNode.getType()) {
                case FILE:
                  if (!replaceExisting) {
                    throw new FileAlreadyExistsException(target.toString());
                  } else {

                    FileNode replacedFile = new FileNode(targetNode.getParent(), targetNode.getName(), ((FileNode)sourceNode).getSegmentBuffer());

                    targetNode.getParent().put(replacedFile);
                    replacedFile.bubble(new HeapEvent(this, target, HeapEventType.MODIFY));
                  }
                  break;
                case DIRECTORY:
                  if (((DirectoryNode)targetNode).exists(sourceNode.getName()) && (!replaceExisting)) {
                    throw new FileAlreadyExistsException(target.resolve(source.getFileName()).toString());
                  } else {

                    FileNode movedFile = new FileNode((DirectoryNode)targetNode, sourceNode.getName(), ((FileNode)sourceNode).getSegmentBuffer());

                    ((DirectoryNode)targetNode).put(movedFile);
                    movedFile.bubble(new HeapEvent(this, (EphemeralPath)target.resolve(sourceNode.getName()), HeapEventType.CREATE));
                  }
                  break;
                default:
                  throw new UnknownSwitchCaseException(targetNode.getType().name());
              }
            } else {
              switch (parentOfTargetNode.getType()) {
                case FILE:
                  throw new NoSuchFileException(target.toString());
                case DIRECTORY:

                  FileNode createdFile = new FileNode((DirectoryNode)parentOfTargetNode, target.getNames()[target.getNameCount() - 1], ((FileNode)sourceNode).getSegmentBuffer());

                  ((DirectoryNode)parentOfTargetNode).put(createdFile);
                  createdFile.bubble(new HeapEvent(this, target, HeapEventType.CREATE));
                  break;
                default:
                  throw new UnknownSwitchCaseException(parentOfTargetNode.getType().name());
              }
            }
            break;
          case DIRECTORY:
            if (targetNode != null) {
              switch (targetNode.getType()) {
                case FILE:
                  throw new FileAlreadyExistsException(target.toString());
                case DIRECTORY:
                  if (targetNode.getParent() == null) {
                    throw new IOException("Not Allowed");
                  } else if (!((DirectoryNode)targetNode).isEmpty()) {
                    throw new DirectoryNotEmptyException(target.toString());
                  } else if (!replaceExisting) {
                    throw new FileAlreadyExistsException(target.toString());
                  } else {

                    DirectoryNode renamedDirectory = new DirectoryNode(targetNode.getParent(), sourceNode.getName());

                    targetNode.getParent().put(renamedDirectory);
                    targetNode.getParent().remove(targetNode.getName());

                    renamedDirectory.bubble(new HeapEvent(this, (EphemeralPath)target.getParent().resolve(sourceNode.getName()), HeapEventType.CREATE));
                    targetNode.bubble(new HeapEvent(this, target, HeapEventType.DELETE));
                  }
                  break;
                default:
                  throw new UnknownSwitchCaseException(targetNode.getType().name());
              }
            } else {
              switch (parentOfTargetNode.getType()) {
                case FILE:
                  throw new NoSuchFileException(target.toString());
                case DIRECTORY:

                  DirectoryNode movedDirectory = new DirectoryNode((DirectoryNode)parentOfTargetNode, sourceNode.getName());

                  ((DirectoryNode)parentOfTargetNode).put(movedDirectory);
                  movedDirectory.bubble(new HeapEvent(this, (EphemeralPath)target.getParent().resolve(sourceNode.getName()), HeapEventType.CREATE));
                  break;
                default:
                  throw new UnknownSwitchCaseException(parentOfTargetNode.getType().name());
              }
            }
            break;
          default:
            throw new UnknownSwitchCaseException(sourceNode.getType().name());
        }

        sourceNode.getParent().remove(sourceNode.getName());
        sourceNode.bubble(new HeapEvent(this, source, HeapEventType.DELETE));
      }
    }
  }

  /**
   * Opens or creates a seekable byte channel for the file at the given path. The behaviour is
   * controlled by the supplied open options, which must all be instances of
   * {@link StandardOpenOption}. Only {@code "posix:permissions"} file attributes are accepted
   * on creation.
   *
   * @param path    the path of the file to open or create
   * @param options the set of open options; must not contain non-standard options
   * @param attrs   optional attributes to apply on creation; only {@code "posix:permissions"}
   *                is accepted
   * @return the opened or newly created {@link SeekableByteChannel}
   * @throws IOException                   if the path is the root, if an option combination is
   *                                       invalid, or if the file cannot be created or opened
   * @throws NoSuchFileException           if the file does not exist and no creation option was
   *                                       provided, or if the parent directory is absent
   * @throws FileAlreadyExistsException    if {@link StandardOpenOption#CREATE_NEW} was specified
   *                                       and the file already exists
   * @throws UnsupportedOperationException if a non-standard open option or unsupported file
   *                                       attribute is supplied
   * @throws ClosedFileSystemException     if the owning file system has been closed
   */
  public synchronized SeekableByteChannel newByteChannel (EphemeralPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else if (path.getNameCount() == 0) {
      throw new IOException("Cannot open a directory for read operations");
    } else {

      HeapNode heapNode = findNode(path);
      Boolean read = null;
      boolean append = false;
      boolean truncateExisting = false;
      boolean createNew = false;
      boolean create = false;
      boolean deleteOnClose = false;

      for (OpenOption option : options) {
        if (!(option instanceof StandardOpenOption)) {
          throw new UnsupportedOperationException("Only standard open options are supported");
        } else {
          if (StandardOpenOption.READ.equals(option)) {
            if (Boolean.FALSE.equals(read)) {
              throw new IllegalArgumentException("Invalid option combination");
            } else {
              read = Boolean.TRUE;
            }
          } else if (StandardOpenOption.WRITE.equals(option) || StandardOpenOption.APPEND.equals(option)) {
            if (Boolean.TRUE.equals(read)) {
              throw new IllegalArgumentException("Invalid option combination");
            } else {
              if (StandardOpenOption.APPEND.equals(option)) {
                if (truncateExisting) {
                  throw new IllegalArgumentException("Invalid option combination");
                } else {
                  append = true;
                }
              }
              read = Boolean.FALSE;
            }
          } else if (StandardOpenOption.TRUNCATE_EXISTING.equals(option)) {
            if (append) {
              throw new IllegalArgumentException("Invalid option combination");
            } else {
              truncateExisting = true;
            }
          } else if (StandardOpenOption.CREATE_NEW.equals(option)) {
            createNew = true;
          } else if (StandardOpenOption.CREATE.equals(option)) {
            create = true;
          } else if (StandardOpenOption.DELETE_ON_CLOSE.equals(option)) {
            deleteOnClose = true;
          }
        }
      }

      for (FileAttribute<?> attribute : attrs) {
        if (!"posix:permissions".equals(attribute.name())) {
          throw new UnsupportedOperationException("Only posix permission file attributes are supported");
        }
      }

      if (read == null) {
        read = Boolean.TRUE;
      }

      if (Boolean.TRUE.equals(read)) {
        if (heapNode == null) {
          throw new NoSuchFileException(path.toString());
        } else {
          switch (heapNode.getType()) {
            case FILE:

              return new EphemeralSeekableByteChannel(this, (FileNode)heapNode, path, true, false, deleteOnClose);
            case DIRECTORY:
              throw new IOException("Cannot open a directory for read operations");
            default:
              throw new UnknownSwitchCaseException(heapNode.getType().name());
          }
        }
      } else {
        if (heapNode == null) {
          if (!(createNew || create)) {
            throw new NoSuchFileException(path.toString());
          } else {

            EphemeralPath parentPath;
            HeapNode parentNode;

            if ((parentNode = findNode(parentPath = path.getParent())) == null) {
              throw new NoSuchFileException(parentPath.toString());
            } else {
              switch (parentNode.getType()) {
                case FILE:
                  throw new NoSuchFileException(path.toString());
                case DIRECTORY:

                  FileNode fileNode;

                  ((DirectoryNode)parentNode).put(fileNode = new FileNode((DirectoryNode)parentNode, path.getNames()[path.getNameCount() - 1], blockSize));
                  fileNode.bubble(new HeapEvent(this, path, HeapEventType.CREATE));

                  return new EphemeralSeekableByteChannel(this, fileNode, path, false, false, deleteOnClose);
                default:
                  throw new UnknownSwitchCaseException(parentNode.getType().name());
              }
            }
          }
        } else {
          if (createNew) {
            throw new FileAlreadyExistsException(path.toString());
          } else {
            switch (heapNode.getType()) {
              case FILE:
                if (truncateExisting) {
                  ((FileNode)heapNode).getSegmentBuffer().clear();
                  heapNode.bubble(new HeapEvent(this, path, HeapEventType.MODIFY));
                }

                return new EphemeralSeekableByteChannel(this, (FileNode)heapNode, path, false, append, deleteOnClose);
              case DIRECTORY:
                throw new IOException("Cannot open a directory for write operations");
              default:
                throw new UnknownSwitchCaseException(heapNode.getType().name());
            }
          }
        }
      }
    }
  }

  /**
   * Traverses the heap tree to locate the node corresponding to the given absolute path.
   * Returns {@code null} when a path component does not exist. Throws
   * {@link NoSuchFileException} when a non-terminal component resolves to a file node rather
   * than a directory.
   *
   * @param path the absolute path to resolve; must be absolute
   * @return the located {@link HeapNode}, or {@code null} if any component is absent
   * @throws NoSuchFileException if the path is relative, or if a file node appears at a
   *                             non-terminal position in the path
   */
  private HeapNode findNode (EphemeralPath path)
    throws NoSuchFileException {

    if (!path.isAbsolute()) {
      throw new NoSuchFileException(path.toString());
    } else {

      DirectoryNode currentNode = rootNode;

      for (int index = 0; index < path.getNames().length; index++) {

        HeapNode childNode;

        if ((childNode = currentNode.get(path.getNames()[index])) == null) {

          return null;
        } else {
          switch (childNode.getType()) {
            case FILE:
              if (index < path.getNames().length - 1) {
                throw new NoSuchFileException(path.toString());
              } else {

                return childNode;
              }
            case DIRECTORY:
              currentNode = (DirectoryNode)childNode;
              break;
            default:
              throw new UnknownSwitchCaseException(childNode.getType().name());
          }
        }
      }

      return currentNode;
    }
  }
}
