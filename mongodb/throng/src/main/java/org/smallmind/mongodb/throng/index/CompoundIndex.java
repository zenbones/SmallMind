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
package org.smallmind.mongodb.throng.index;

import java.util.LinkedList;
import org.smallmind.mongodb.throng.index.annotation.Index;
import org.smallmind.mongodb.throng.index.annotation.IndexOptions;
import org.smallmind.mongodb.throng.index.annotation.Indexes;

/**
 * Represents a compound index definition built from {@link Indexes} annotations, including shared options.
 */
public class CompoundIndex {

  private final IndexOptions indexOptions;
  private final LinkedList<IndexedElement> indexedElementList = new LinkedList<>();

  /**
   * Creates a compound index definition from the {@link Indexes} annotation.
   *
   * @param indexes collection of index elements and options
   */
  public CompoundIndex (Indexes indexes) {

    indexOptions = indexes.options();

    for (Index index : indexes.value()) {
      indexedElementList.add(new IndexedElement(index.value(), index.type()));
    }
  }

  private CompoundIndex (LinkedList<IndexedElement> indexedElementList, IndexOptions indexOptions) {

    this.indexOptions = indexOptions;

    this.indexedElementList.addAll(indexedElementList);
  }

  /**
   * Prefixes each indexed element's field path with the provided prolog, returning a new compound definition.
   *
   * @param prolog field path prefix to apply
   * @return new compound index with adjusted field paths
   */
  public CompoundIndex accumulate (String prolog) {

    LinkedList<IndexedElement> indexedElementList = new LinkedList<>();

    for (IndexedElement indexedElement : this.indexedElementList) {
      indexedElementList.add(indexedElement.accumulate(prolog));
    }

    return new CompoundIndex(indexedElementList, indexOptions);
  }

  /**
   * @return array of indexed elements that make up the compound index
   */
  public IndexedElement[] getIndexedElements () {

    return indexedElementList.toArray(new IndexedElement[0]);
  }

  /**
   * @return options applied to this compound index
   */
  public IndexOptions getIndexOptions () {

    return indexOptions;
  }
}
