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
package org.smallmind.mongodb.throng.query;

import java.util.LinkedList;
import com.mongodb.client.model.Sorts;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * Fluent builder for MongoDB sort specifications.
 */
public class Sort {

  private final LinkedList<Bson> sortList = new LinkedList<>();

  /**
   * @return new sort builder
   */
  public static Sort on () {

    return new Sort();
  }

  /**
   * Adds an ascending sort on the given field.
   *
   * @param fieldName field name to sort ascending
   * @return this builder for chaining
   */
  public Sort asc (String fieldName) {

    sortList.add(Sorts.ascending(fieldName));

    return this;
  }

  /**
   * Adds a descending sort on the given field.
   *
   * @param fieldName field name to sort descending
   * @return this builder for chaining
   */
  public Sort desc (String fieldName) {

    sortList.add(Sorts.descending(fieldName));

    return this;
  }

  /**
   * Builds the BSON sort document for use with driver queries.
   *
   * @param documentClass target document class
   * @param codecRegistry registry used by the driver
   * @return BSON sort document
   */
  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    return Sorts.orderBy(sortList).toBsonDocument(documentClass, codecRegistry);
  }
}
