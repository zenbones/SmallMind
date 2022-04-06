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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import org.smallmind.file.ephemeral.heap.HeapNode;
import org.smallmind.file.ephemeral.heap.HeapNodeType;
import org.smallmind.nutsnbolts.util.SnowflakeId;

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

  @Override
  public FileTime lastModifiedTime () {

    return lastModifiedTime;
  }

  public void setLastModifiedTime (FileTime lastModifiedTime) {

    this.lastModifiedTime = lastModifiedTime;
  }

  @Override
  public FileTime lastAccessTime () {

    return lastAccessTime;
  }

  public void setLastAccessTime (FileTime lastAccessTime) {

    this.lastAccessTime = lastAccessTime;
  }

  @Override
  public FileTime creationTime () {

    return creationTime;
  }

  public void setCreationTime (FileTime creationTime) {

    this.creationTime = creationTime;
  }

  @Override
  public boolean isRegularFile () {

    return regularFile;
  }

  @Override
  public boolean isDirectory () {

    return directory;
  }

  @Override
  public boolean isSymbolicLink () {

    return symbolicLink;
  }

  @Override
  public boolean isOther () {

    return other;
  }

  @Override
  public long size () {

    return heapNode.size();
  }

  @Override
  public Object fileKey () {

    return id;
  }
}
