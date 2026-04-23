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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import org.smallmind.file.ephemeral.heap.HeapNode;
import org.smallmind.file.ephemeral.heap.HeapNodeType;
import org.smallmind.nutsnbolts.util.SnowflakeId;

/**
 * {@link BasicFileAttributes} implementation that tracks timestamps and type information for
 * nodes held in the ephemeral heap. The file key is an immutable, globally unique hex-encoded
 * identifier generated via {@link SnowflakeId} at construction time.
 */
public class EphemeralBasicFileAttributes implements BasicFileAttributes {

  private final String id = SnowflakeId.newInstance().generateHexEncoding();
  private final HeapNode heapNode;
  private final boolean regularFile;
  private final boolean directory;
  private final boolean symbolicLink;
  private final boolean other;
  private FileTime lastModifiedTime;
  private FileTime lastAccessTime;
  private FileTime creationTime;

  /**
   * Initialises attributes for the given heap node. All three timestamps are set to the
   * current wall-clock time. The {@code regularFile} and {@code directory} flags are derived
   * from the node's {@link HeapNodeType}; symbolic links and "other" types are always
   * {@code false}.
   *
   * @param heapNode the heap node whose attributes are being tracked; must not be {@code null}
   */
  public EphemeralBasicFileAttributes (HeapNode heapNode) {

    this.heapNode = heapNode;

    creationTime = FileTime.fromMillis(System.currentTimeMillis());
    lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis());
    lastAccessTime = FileTime.fromMillis(System.currentTimeMillis());

    directory = HeapNodeType.DIRECTORY.equals(heapNode.getType());
    regularFile = HeapNodeType.FILE.equals(heapNode.getType());
    symbolicLink = false;
    other = false;
  }

  /**
   * Returns the time at which the file was last modified.
   *
   * @return the last-modified {@link FileTime}; never {@code null}
   */
  @Override
  public FileTime lastModifiedTime () {

    return lastModifiedTime;
  }

  /**
   * Updates the recorded last-modified time.
   *
   * @param lastModifiedTime the new timestamp; must not be {@code null}
   */
  public void setLastModifiedTime (FileTime lastModifiedTime) {

    this.lastModifiedTime = lastModifiedTime;
  }

  /**
   * Returns the time at which the file was last accessed.
   *
   * @return the last-access {@link FileTime}; never {@code null}
   */
  @Override
  public FileTime lastAccessTime () {

    return lastAccessTime;
  }

  /**
   * Updates the recorded last-access time.
   *
   * @param lastAccessTime the new timestamp; must not be {@code null}
   */
  public void setLastAccessTime (FileTime lastAccessTime) {

    this.lastAccessTime = lastAccessTime;
  }

  /**
   * Returns the creation time of the file.
   *
   * @return the creation {@link FileTime}; never {@code null}
   */
  @Override
  public FileTime creationTime () {

    return creationTime;
  }

  /**
   * Updates the recorded creation time.
   *
   * @param creationTime the new timestamp; must not be {@code null}
   */
  public void setCreationTime (FileTime creationTime) {

    this.creationTime = creationTime;
  }

  /**
   * Indicates whether the heap node represents a regular file.
   *
   * @return {@code true} if the node is of type {@link HeapNodeType#FILE}
   */
  @Override
  public boolean isRegularFile () {

    return regularFile;
  }

  /**
   * Indicates whether the heap node represents a directory.
   *
   * @return {@code true} if the node is of type {@link HeapNodeType#DIRECTORY}
   */
  @Override
  public boolean isDirectory () {

    return directory;
  }

  /**
   * Indicates whether the heap node represents a symbolic link.
   *
   * @return always {@code false}; symbolic links are not supported
   */
  @Override
  public boolean isSymbolicLink () {

    return symbolicLink;
  }

  /**
   * Indicates whether the heap node represents a file type other than a regular file,
   * directory, or symbolic link.
   *
   * @return always {@code false}
   */
  @Override
  public boolean isOther () {

    return other;
  }

  /**
   * Returns the size of the file in bytes as reported by the underlying heap node.
   *
   * @return the file size in bytes
   */
  @Override
  public long size () {

    return heapNode.size();
  }

  /**
   * Returns a unique, immutable object that identifies the file. The key is a hex-encoded
   * Snowflake identifier assigned at construction time.
   *
   * @return a non-{@code null} hex string that is unique across all attribute instances
   */
  @Override
  public Object fileKey () {

    return id;
  }
}
