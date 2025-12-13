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
 * Basic file attributes held for ephemeral heap nodes.
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
   * Initializes attributes based on the provided heap node type and current time.
   *
   * @param heapNode the node whose attributes are being tracked
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
   * {@inheritDoc}
   */
  @Override
  public FileTime lastModifiedTime () {

    return lastModifiedTime;
  }

  /**
   * Updates the recorded last modified time.
   *
   * @param lastModifiedTime the new timestamp
   */
  public void setLastModifiedTime (FileTime lastModifiedTime) {

    this.lastModifiedTime = lastModifiedTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileTime lastAccessTime () {

    return lastAccessTime;
  }

  /**
   * Updates the recorded last access time.
   *
   * @param lastAccessTime the new timestamp
   */
  public void setLastAccessTime (FileTime lastAccessTime) {

    this.lastAccessTime = lastAccessTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileTime creationTime () {

    return creationTime;
  }

  /**
   * Updates the recorded creation time.
   *
   * @param creationTime the new timestamp
   */
  public void setCreationTime (FileTime creationTime) {

    this.creationTime = creationTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRegularFile () {

    return regularFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectory () {

    return directory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSymbolicLink () {

    return symbolicLink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOther () {

    return other;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long size () {

    return heapNode.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object fileKey () {

    return id;
  }
}
