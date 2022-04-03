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

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.Files;
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
import org.smallmind.file.ephemeral.heap.HeapNode;

public class EphemeralFileStore extends FileStore {

  private static final Map<String, Class<? extends FileAttributeView>> SUPPORTED_FILE_VIEW_MAP = Map.of("basic", BasicFileAttributeView.class);
  private final EphemeralFileStoreAttributeView fileStoreAttributeView = new EphemeralFileStoreAttributeView();
  private final HeapNode rootNode = new DirectoryNode();
  private final long capacity;
  private final long blockSize;

  public EphemeralFileStore (long capacity, long blockSize) {

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

    return getUsableSpace();
  }

  @Override
  public long getUsableSpace () {

    return capacity;
  }

  @Override
  public long getUnallocatedSpace () {

    return capacity - rootNode.size();
  }

  @Override
  public boolean supportsFileAttributeView (Class<? extends FileAttributeView> type) {

    return SUPPORTED_FILE_VIEW_MAP.containsValue(type);
  }

  @Override
  public boolean supportsFileAttributeView (String name) {

    return SUPPORTED_FILE_VIEW_MAP.containsKey(name);
  }

  public Set<String> getSupportedFileAttributeViewNames () {

    return SUPPORTED_FILE_VIEW_MAP.keySet();
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView (Class<V> type) {

    return EphemeralFileStoreAttributeView.class.equals(type) ? type.cast(fileStoreAttributeView) : null;
  }

  @Override
  public Object getAttribute (String attribute)
    throws IOException {

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

  public SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {

    Boolean read = null;
    boolean skipToEnd;

    for (OpenOption option : options) {
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
          read = Boolean.FALSE;
        }
      }
    }

    return null;
//    Files.newByteChannel()
  }
}
