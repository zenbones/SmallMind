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
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * Fluent builder for MongoDB update operations.
 */
public class Updates {

  private final LinkedList<Bson> updateList = new LinkedList<>();

  /**
   * @return new updates builder
   */
  public static Updates of () {

    return new Updates();
  }

  /**
   * Sets a field to the given value.
   */
  public Updates set (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.set(fieldName, value));

    return this;
  }

  /**
   * Removes the specified field.
   */
  public Updates unset (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.unset(fieldName));

    return this;
  }

  /**
   * Sets a field only when the document is inserted.
   */
  public Updates setOnInsert (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.setOnInsert(fieldName, value));

    return this;
  }

  /**
   * Increments a numeric field.
   */
  public Updates inc (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.inc(fieldName, number));

    return this;
  }

  /**
   * Multiplies a numeric field.
   */
  public Updates mul (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.mul(fieldName, number));

    return this;
  }

  /**
   * Applies a max comparison update.
   */
  public Updates max (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.max(fieldName, value));

    return this;
  }

  /**
   * Applies a min comparison update.
   */
  public Updates min (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.min(fieldName, value));

    return this;
  }

  /**
   * Pushes a value onto an array field.
   */
  public Updates push (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.push(fieldName, value));

    return this;
  }

  /**
   * Adds a value to a set if it is not already present.
   */
  public Updates addToSet (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.addToSet(fieldName, value));

    return this;
  }

  /**
   * Removes matching elements from an array field.
   */
  public Updates pull (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.pull(fieldName, value));

    return this;
  }

  /**
   * Pops the first element from an array field.
   */
  public Updates popFirst (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popFirst(fieldName));

    return this;
  }

  /**
   * Pops the last element from an array field.
   */
  public Updates popLast (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popLast(fieldName));

    return this;
  }

  /**
   * Renames a field.
   */
  public Updates rename (String fieldName, String updatedFieldName) {

    updateList.add(com.mongodb.client.model.Updates.rename(fieldName, updatedFieldName));

    return this;
  }

  /**
   * Combines the accumulated updates into a single BSON document.
   *
   * @param documentClass target document class
   * @param codecRegistry driver codec registry
   * @return BSON representation of the updates
   */
  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    return com.mongodb.client.model.Updates.combine(updateList).toBsonDocument(documentClass, codecRegistry);
  }
}
