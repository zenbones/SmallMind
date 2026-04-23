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
package org.smallmind.file.ephemeral.heap;

import org.smallmind.nutsnbolts.io.ByteArrayIOBuffer;

/**
 * Represents an in-memory file node in the ephemeral heap file-system tree.
 *
 * <p>A {@code FileNode} stores its content in a {@link ByteArrayIOBuffer}, which grows
 * as data is written. The initial capacity of the buffer can be specified at construction
 * time, or an existing buffer can be provided directly (for example when copying a file).
 * The node's {@link #size()} reflects the current write position within the buffer.
 *
 * @see HeapNode
 * @see DirectoryNode
 */
public class FileNode extends HeapNode {

  /**
   * The backing buffer that holds the file's raw byte content.
   */
  private final ByteArrayIOBuffer segmentBuffer;

  /**
   * Constructs a new file node with a freshly allocated backing buffer.
   *
   * @param parent     the {@link DirectoryNode} that will contain this file
   * @param name       the simple name of the file
   * @param allocation the initial byte capacity to pre-allocate for the backing buffer
   */
  public FileNode (DirectoryNode parent, String name, int allocation) {

    super(parent, name);

    segmentBuffer = new ByteArrayIOBuffer(allocation);
  }

  /**
   * Constructs a new file node backed by an existing buffer.
   *
   * <p>Use this constructor to wrap a pre-populated {@link ByteArrayIOBuffer}, for
   * instance when duplicating or moving file content.
   *
   * @param parent        the {@link DirectoryNode} that will contain this file
   * @param name          the simple name of the file
   * @param segmentBuffer the buffer that provides the initial file content
   */
  public FileNode (DirectoryNode parent, String name, ByteArrayIOBuffer segmentBuffer) {

    super(parent, name);

    this.segmentBuffer = segmentBuffer;
  }

  /**
   * Returns the type identifier for this node.
   *
   * @return {@link HeapNodeType#FILE}, always
   */
  @Override
  public HeapNodeType getType () {

    return HeapNodeType.FILE;
  }

  /**
   * Returns the backing buffer used to store this file's byte content.
   *
   * <p>Callers may read from or write to the buffer directly; concurrent access must
   * be coordinated externally.
   *
   * @return the {@link ByteArrayIOBuffer} holding the file's raw bytes
   */
  public ByteArrayIOBuffer getSegmentBuffer () {

    return segmentBuffer;
  }

  /**
   * Returns the current size of the file in bytes.
   *
   * <p>The size is determined by the position of the limit bookmark within the backing
   * buffer, which reflects how many bytes have been written to the file.
   *
   * @return the number of bytes currently in the file
   */
  @Override
  public long size () {

    return segmentBuffer.getLimitBookmark().position();
  }
}
