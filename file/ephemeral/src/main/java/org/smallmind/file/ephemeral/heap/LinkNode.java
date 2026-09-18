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

import java.nio.charset.StandardCharsets;

/**
 * Represents a symbolic link in the ephemeral heap file-system tree.
 *
 * <p>A link stores its target exactly as it was supplied, unresolved. The target may be absolute or
 * relative, and it may not exist at all — a dangling link is legal, and resolution therefore happens
 * at lookup time rather than at creation time. A relative target is interpreted against the
 * directory holding the link.
 *
 * @see HeapNode
 * @see HeapNodeType#SYMBOLIC_LINK
 */
public class LinkNode extends HeapNode {

  /**
   * The raw, unresolved target of this link.
   */
  private final String target;

  /**
   * Constructs a new symbolic link node.
   *
   * @param parent the {@link DirectoryNode} that will contain this link
   * @param name   the simple name of the link
   * @param target the raw target of the link, which need not exist
   */
  public LinkNode (DirectoryNode parent, String name, String target) {

    super(parent, name);

    this.target = target;
  }

  /**
   * Returns the type identifier for this node.
   *
   * @return {@link HeapNodeType#SYMBOLIC_LINK}, always
   */
  @Override
  public HeapNodeType getType () {

    return HeapNodeType.SYMBOLIC_LINK;
  }

  /**
   * Returns the raw, unresolved target of this link.
   *
   * @return the link target; never {@code null}
   */
  public String getTarget () {

    return target;
  }

  /**
   * Returns the size of this link, which a POSIX file system reports as the length of its target.
   *
   * @return the encoded length of the link target
   */
  @Override
  public long size () {

    return target.getBytes(StandardCharsets.UTF_8).length;
  }
}
