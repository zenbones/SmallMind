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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Map;
import java.util.Set;
import org.smallmind.file.ephemeral.heap.DirectoryNode;
import org.smallmind.file.ephemeral.heap.HeapEventListener;
import org.smallmind.file.ephemeral.heap.HeapNode;
import org.smallmind.file.ephemeral.heap.HeapNodeType;

public class EphemeralFileStore extends FileStore {

  private static final Map<String, Class<? extends FileAttributeView>> SUPPORTED_FILE_VIEW_MAP = Map.of("basic", BasicFileAttributeView.class);
  private final EphemeralFileSystem fileSystem;
  private final EphemeralFileStoreAttributeView fileStoreAttributeView = new EphemeralFileStoreAttributeView();
  private final HeapNode rootNode = new DirectoryNode();
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
  public long getUnallocatedSpace () {

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
    throws NotDirectoryException {

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

  public void unregisterHeapListener (EphemeralPath path, HeapEventListener listener) {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
    } else {

      HeapNode node;

      if ((node = findNode(path)) != null) {
        node.unregisterListener(listener);
      }
    }
  }

  /*
READ
WRITE

  APPEND
If this option is present then the file is opened for writing and each invocation of the channel's write method first advances the position to the end of the file and then writes the requested data. Whether the advancement of the position and the writing of the data are done in a single atomic operation is system-dependent and therefore unspecified. This option may not be used in conjunction with the READ or TRUNCATE_EXISTING options.
TRUNCATE_EXISTING
If this option is present then the existing file is truncated to a size of 0 bytes. This option is ignored when the file is opened only for reading.
CREATE_NEW
If this option is present then a new file is created, failing if the file already exists or is a symbolic link. When creating a file the check for the existence of the file and the creation of the file if it does not exist is atomic with respect to other file system operations. This option is ignored when the file is opened only for reading.
CREATE
If this option is present then an existing file is opened if it exists, otherwise a new file is created. This option is ignored if the CREATE_NEW option is also present or the file is opened only for reading.
DELETE_ON_CLOSE
When this option is present then the implementation makes a best effort attempt to delete the file when closed by the close method. If the close method is not invoked then a best effort attempt is made to delete the file when the Java virtual machine terminates.
SPARSE
When creating a new file this option is a hint that the new file will be sparse. This option is ignored when not creating a new file.
SYNC
Requires that every update to the file's content or metadata be written synchronously to the underlying storage device. (see Synchronized I/O file integrity).
DSYNC
Requires that every update to the file's content be written synchronously to the underlying storage device. (see Synchronized I/O file integrity).
   */

  /*
IllegalArgumentException – if the set contains an invalid combination of options
UnsupportedOperationException – if an unsupported open option is specified or the array contains attributes that cannot be set atomically when creating the file
FileAlreadyExistsException – if a file of that name already exists and the CREATE_NEW option is specified (optional specific exception)
IOException – if an I/O error occurs
SecurityException – In the case of the default provider, and a security manager is installed, the checkRead method is invoked to check read access to the path if the file is opened for reading. The checkWrite method is invoked to check write access to the path if the file is opened for writing. The checkDelete method is invoked to check delete access if the file is opened with the DELETE_ON_CLOSE option.
   */

  public SeekableByteChannel newByteChannel (EphemeralPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (!fileSystem.isOpen()) {
      throw new ClosedFileSystemException();
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
        } else if (HeapNodeType.DIRECTORY.equals(heapNode.getType())) {
          throw new IOException("Cannot open a directory for read operations");
        } else {
          // open (delOnC)
        }
      } else {
        if (heapNode == null) {
          if (!(createNew || create)) {
            throw new FileNotFoundException(path.toString());
          } else {
            // create and open
          }
        } else {
         if (createNew) {
           throw new FileAlreadyExistsException(path.toString());
         } else {
           if (truncateExisting) {
             // empty
           } else if (append) {
             // open and set to end
           } else {
             // open
           }
         }
        }

      }

      return null;
//    Files.newByteChannel()
    }
  }

  private HeapNode findNode (EphemeralPath path) {

    HeapNode node = rootNode;

    for (Path segment : path.toAbsolutePath()) {
      if (segment =)
    }

    return node;
  }
}
