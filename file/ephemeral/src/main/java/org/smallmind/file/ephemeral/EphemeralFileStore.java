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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Map;
import java.util.Set;
import org.smallmind.file.ephemeral.heap.DirectoryNode;
import org.smallmind.file.ephemeral.heap.FileNode;
import org.smallmind.file.ephemeral.heap.HeapEventListener;
import org.smallmind.file.ephemeral.heap.HeapNode;
import org.smallmind.file.ephemeral.heap.HeapNodeType;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class EphemeralFileStore extends FileStore {

  private static final Map<String, Class<? extends FileAttributeView>> SUPPORTED_FILE_VIEW_MAP = Map.of("basic", BasicFileAttributeView.class);
  private final EphemeralFileSystem fileSystem;
  private final EphemeralFileStoreAttributeView fileStoreAttributeView = new EphemeralFileStoreAttributeView();
  private final DirectoryNode rootNode = new DirectoryNode(null, null);
  private final long capacity;
  private final int blockSize;

  public EphemeralFileStore (EphemeralFileSystem fileSystem, long capacity, int blockSize) {

    this.fileSystem = fileSystem;
    this.capacity = capacity;
    this.blockSize = blockSize;
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

  @Override
  public long getTotalSpace () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return getUsableSpace();
    }
  }

  @Override
  public long getUsableSpace () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return capacity;
    }
  }

  @Override
  public synchronized long getUnallocatedSpace ()
    throws IOException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return capacity - rootNode.size();
    }
  }

  @Override
  public boolean supportsFileAttributeView (Class<? extends FileAttributeView> type) {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return SUPPORTED_FILE_VIEW_MAP.containsValue(type);
    }
  }

  @Override
  public boolean supportsFileAttributeView (String name) {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return SUPPORTED_FILE_VIEW_MAP.containsKey(name);
    }
  }

  public Set<String> getSupportedFileAttributeViewNames () {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return SUPPORTED_FILE_VIEW_MAP.keySet();
    }
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView (Class<V> type) {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      return EphemeralFileStoreAttributeView.class.equals(type) ? type.cast(fileStoreAttributeView) : null;
    }
  }

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

  public void registerHeapListener (EphemeralPath path, HeapEventListener listener)
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

  public void unregisterHeapListener (EphemeralPath path, HeapEventListener listener)
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

  public synchronized void createDirectory (EphemeralPath path, FileAttribute<?>... attrs)
    throws NoSuchFileException, FileNotFoundException, FileAlreadyExistsException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      for (FileAttribute<?> attribute : attrs) {
        if (!"posix:permissions".equals(attribute.name())) {
          throw new UnsupportedOperationException("Only posix permission file attributes are supported");
        }
      }

      if (path.getNameCount() == 0) {
        throw new NoSuchFileException(path.toString());
      } else {

        HeapNode parentNode;

        if ((parentNode = findNode(path.getParent())) == null) {
          throw new FileNotFoundException(path.toString());
        } else {
          switch (parentNode.getType()) {
            case FILE:
              throw new FileNotFoundException(path.toString());
            case DIRECTORY:
              if (((DirectoryNode)parentNode).get(path.getNames()[path.getNameCount() - 1]) != null) {
                throw new FileAlreadyExistsException(path.toString());
              } else {
                ((DirectoryNode)parentNode).put(new DirectoryNode((DirectoryNode)parentNode, path.getNames()[path.getNameCount() - 1]));
              }
            default:
              throw new UnknownSwitchCaseException(parentNode.getType().name());
          }
        }
      }
    }
  }

  public synchronized void delete (EphemeralPath path)
    throws NoSuchFileException, DirectoryNotEmptyException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      HeapNode heapNode;

      if (path.getNameCount() == 0) {
        throw new NoSuchFileException(path.toString());
      } else if ((heapNode = findNode(path)) == null) {
        throw new NoSuchFileException(path.toString());
      } else {
        switch (heapNode.getType()) {
          case FILE:
            heapNode.getParent().remove(heapNode.getName());
            break;
          case DIRECTORY:
            if (!((DirectoryNode)heapNode).isEmpty()) {
              throw new DirectoryNotEmptyException(path.toString());
            } else {
              heapNode.getParent().remove(heapNode.getName());
            }
            break;
          default:
            throw new UnknownSwitchCaseException(heapNode.getType().name());
        }
      }
    }
  }

  public synchronized SeekableByteChannel newByteChannel (EphemeralPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else if (path.getNameCount() == 0) {
      throw new NoSuchFileException(path.toString());
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
          throw new FileNotFoundException(path.toString());
        } else {
          switch (heapNode.getType()) {
            case FILE:

              return new EphemeralSeekableByteChannel(this, ((FileNode)heapNode).getStream(), true, false, deleteOnClose);
            case DIRECTORY:
              throw new IOException("Cannot open a directory for read operations");
            default:
              throw new UnknownSwitchCaseException(heapNode.getType().name());
          }
        }
      } else {
        if (heapNode == null) {
          if (!(createNew || create)) {
            throw new FileNotFoundException(path.toString());
          } else {

            HeapNode parentNode;

            if ((parentNode = findNode(path.getParent())) == null) {
              throw new FileNotFoundException(path.toString());
            } else {
              switch (parentNode.getType()) {
                case FILE:
                  throw new FileNotFoundException(path.toString());
                case DIRECTORY:

                  FileNode fileNode;

                  ((DirectoryNode)parentNode).put(fileNode = new FileNode((DirectoryNode)parentNode, path.getNames()[path.getNameCount() - 1], blockSize));

                  return new EphemeralSeekableByteChannel(this, fileNode.getStream(), false, false, deleteOnClose);
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
                  ((FileNode)heapNode).getStream().clear();
                }

                return new EphemeralSeekableByteChannel(this, ((FileNode)heapNode).getStream(), false, append, deleteOnClose);
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

  private synchronized HeapNode findNode (EphemeralPath path)
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
