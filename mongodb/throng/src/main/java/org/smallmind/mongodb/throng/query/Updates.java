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
   * Creates a new, unconfigured update builder.
   *
   * @return a fresh {@link Updates} instance
   */
  public static Updates of () {

    return new Updates();
  }

  /**
   * Sets a field to the given value.
   *
   * @param fieldName field to update
   * @param value     new value for the field
   * @return this builder for chaining
   */
  public Updates set (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.set(fieldName, value));

    return this;
  }

  /**
   * Removes the specified field from the document.
   *
   * @param fieldName field to remove
   * @return this builder for chaining
   */
  public Updates unset (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.unset(fieldName));

    return this;
  }

  /**
   * Sets a field only when the document is inserted via an upsert.
   *
   * @param fieldName field to set on insert
   * @param value     value to assign
   * @return this builder for chaining
   */
  public Updates setOnInsert (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.setOnInsert(fieldName, value));

    return this;
  }

  /**
   * Increments a numeric field by the given amount.
   *
   * @param fieldName field to increment
   * @param number    amount to add
   * @return this builder for chaining
   */
  public Updates inc (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.inc(fieldName, number));

    return this;
  }

  /**
   * Multiplies a numeric field by the given factor.
   *
   * @param fieldName field to multiply
   * @param number    multiplication factor
   * @return this builder for chaining
   */
  public Updates mul (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.mul(fieldName, number));

    return this;
  }

  /**
   * Updates the field to the given value only if it is greater than the current value.
   *
   * @param fieldName field to update
   * @param value     candidate maximum value
   * @return this builder for chaining
   */
  public Updates max (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.max(fieldName, value));

    return this;
  }

  /**
   * Updates the field to the given value only if it is less than the current value.
   *
   * @param fieldName field to update
   * @param value     candidate minimum value
   * @return this builder for chaining
   */
  public Updates min (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.min(fieldName, value));

    return this;
  }

  /**
   * Appends a value to the end of an array field.
   *
   * @param fieldName array field to push to
   * @param value     value to append
   * @return this builder for chaining
   */
  public Updates push (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.push(fieldName, value));

    return this;
  }

  /**
   * Adds a value to an array field only if it is not already present.
   *
   * @param fieldName array field acting as a set
   * @param value     value to add
   * @return this builder for chaining
   */
  public Updates addToSet (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.addToSet(fieldName, value));

    return this;
  }

  /**
   * Removes all array elements equal to the given value.
   *
   * @param fieldName array field to pull from
   * @param value     value to remove
   * @return this builder for chaining
   */
  public Updates pull (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.pull(fieldName, value));

    return this;
  }

  /**
   * Removes the first element from an array field.
   *
   * @param fieldName array field to modify
   * @return this builder for chaining
   */
  public Updates popFirst (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popFirst(fieldName));

    return this;
  }

  /**
   * Removes the last element from an array field.
   *
   * @param fieldName array field to modify
   * @return this builder for chaining
   */
  public Updates popLast (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popLast(fieldName));

    return this;
  }

  /**
   * Renames a field to the given new name.
   *
   * @param fieldName        current field name
   * @param updatedFieldName new field name
   * @return this builder for chaining
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
