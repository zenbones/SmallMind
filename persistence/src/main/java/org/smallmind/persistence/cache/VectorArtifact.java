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
package org.smallmind.persistence.cache;

/**
 * Value object holding the namespace and index set that together define a cache vector key.
 */
public class VectorArtifact {

  private final String vectorNamespace;
  private final VectorIndex[] vectorIndices;

  /**
   * Constructs an artifact with the given namespace and index array.
   *
   * @param vectorNamespace namespace string that scopes the vector, typically derived from a cache annotation
   * @param vectorIndices   ordered array of index values used to compose the vector key
   */
  public VectorArtifact (String vectorNamespace, VectorIndex[] vectorIndices) {

    this.vectorNamespace = vectorNamespace;
    this.vectorIndices = vectorIndices;
  }

  /**
   * Returns the namespace that scopes this vector.
   *
   * @return vector namespace string
   */
  public String getVectorNamespace () {

    return vectorNamespace;
  }

  /**
   * Returns the array of index values used to construct the vector key.
   *
   * @return array of {@link VectorIndex} instances
   */
  public VectorIndex[] getVectorIndices () {

    return vectorIndices;
  }
}
