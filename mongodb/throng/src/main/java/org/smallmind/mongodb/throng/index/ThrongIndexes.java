/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import org.smallmind.mongodb.throng.index.annotation.Indexed;
import org.smallmind.mongodb.throng.index.annotation.Indexes;

public class ThrongIndexes {

  private final LinkedList<IndexedField> indexedFieldList = new LinkedList<>();
  private final LinkedList<CompoundIndex> compoundIndexList = new LinkedList<>();

  public IndexedField[] getIndexedFields () {

    return indexedFieldList.toArray(new IndexedField[0]);
  }

  public CompoundIndex[] getCompoundIndexes () {

    return compoundIndexList.toArray(new CompoundIndex[0]);
  }

  public void add (ThrongIndexes throngIndexes) {

    indexedFieldList.addAll(throngIndexes.indexedFieldList);
    compoundIndexList.addAll(throngIndexes.compoundIndexList);
  }

  public void accumulate (String prolog, ThrongIndexes throngIndexes) {

    for (IndexedField indexedField : throngIndexes.indexedFieldList) {
      indexedFieldList.add(indexedField.accumulate(prolog));
    }
    for (CompoundIndex compoundIndex : throngIndexes.compoundIndexList) {
      compoundIndexList.add(compoundIndex.accumulate(prolog));
    }
  }

  public void addIndexed (String field, Indexed indexed) {

    indexedFieldList.add(new IndexedField(field, indexed));
  }

  public void addIndexes (Indexes[] indexesArray) {

    for (Indexes indexes : indexesArray) {
      compoundIndexList.add(new CompoundIndex(indexes));
    }
  }
}
