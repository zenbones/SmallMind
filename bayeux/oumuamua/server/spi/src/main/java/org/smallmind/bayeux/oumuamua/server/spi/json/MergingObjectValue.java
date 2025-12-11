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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;
import org.smallmind.nutsnbolts.util.IterableIterator;

/**
 * Object value wrapper that supports overlay mutations while preserving the original backing object.
 *
 * @param <V> concrete value type used in messages
 */
public class MergingObjectValue<V extends Value<V>> implements ObjectValue<V> {

  private final ObjectValue<V> innerObjectValue;
  private ObjectValue<V> outerObjectValue;
  private HashSet<String> removedSet;

  /**
   * Wraps an existing object value for copy-on-write style updates.
   *
   * @param innerObjectValue underlying object to wrap
   */
  public MergingObjectValue (ObjectValue<V> innerObjectValue) {

    this.innerObjectValue = innerObjectValue;
  }

  /**
   * @return factory associated with the underlying object
   */
  @Override
  public ValueFactory<V> getFactory () {

    return innerObjectValue.getFactory();
  }

  /**
   * @return number of visible fields
   */
  @Override
  public int size () {

    return fieldNameSet().size();
  }

  /**
   * @return {@code true} when no fields are visible
   */
  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  /**
   * @return iterator over visible field names
   */
  @Override
  public Iterator<String> fieldNames () {

    return fieldNameSet().iterator();
  }

  /**
   * Computes the merged set of field names considering removals and additions.
   *
   * @return set of field names
   */
  private HashSet<String> fieldNameSet () {

    HashSet<String> nameSet = new HashSet<>();

    for (String fieldName : new IterableIterator<>(innerObjectValue.fieldNames())) {
      if ((removedSet == null) || (!removedSet.contains(fieldName))) {
        nameSet.add(fieldName);
      }
    }

    if (outerObjectValue != null) {
      for (String fieldName : new IterableIterator<>(outerObjectValue.fieldNames())) {
        nameSet.add(fieldName);
      }
    }

    return nameSet;
  }

  /**
   * Retrieves a field value, creating wrappers for nested structures when necessary.
   *
   * @param field field name
   * @return value or {@code null} if removed or missing
   */
  @Override
  public Value<V> get (String field) {

    Value<V> value;

    if ((outerObjectValue != null) && ((value = outerObjectValue.get(field)) != null)) {

      return value;
    } else if (((removedSet == null) || (!removedSet.contains(field))) && ((value = innerObjectValue.get(field)) != null)) {
      switch (value.getType()) {
        case OBJECT:

          MergingObjectValue<V> mergedValue;

          if (outerObjectValue == null) {
            outerObjectValue = innerObjectValue.getFactory().objectValue();
          }

          outerObjectValue.put(field, mergedValue = new MergingObjectValue<>((ObjectValue<V>)value));

          return mergedValue;
        case ARRAY:

          CopyOnWriteArrayValue<V> copyOnWriteValue;

          if (outerObjectValue == null) {
            outerObjectValue = innerObjectValue.getFactory().objectValue();
          }

          outerObjectValue.put(field, copyOnWriteValue = new CopyOnWriteArrayValue<>((ArrayValue<V>)value));

          return copyOnWriteValue;
        default:

          return value;
      }
    } else {

      return null;
    }
  }

  /**
   * Puts or replaces a field in the overlay object.
   *
   * @param field field name
   * @param value value to store
   * @return this object
   */
  @Override
  public <U extends Value<V>> ObjectValue<V> put (String field, U value) {

    if (outerObjectValue == null) {
      outerObjectValue = innerObjectValue.getFactory().objectValue();
    }

    outerObjectValue.put(field, value);

    return this;
  }

  /**
   * Removes a field from both overlay and base objects.
   *
   * @param field field name to remove
   * @return removed value if present
   */
  @Override
  public Value<V> remove (String field) {

    Value<V> outerRemovedValue = (outerObjectValue == null) ? null : outerObjectValue.remove(field);
    Value<V> innerRemovedValue = null;

    if (((removedSet == null) || (!removedSet.contains(field))) && ((innerRemovedValue = innerObjectValue.get(field)) != null)) {
      if (removedSet == null) {
        removedSet = new HashSet<>();
      }
      removedSet.add(field);
    }

    return (outerRemovedValue != null) ? outerRemovedValue : innerRemovedValue;
  }

  /**
   * Removes all visible fields.
   *
   * @return this object
   */
  @Override
  public ObjectValue<V> removeAll () {

    if (outerObjectValue != null) {
      outerObjectValue = null;
    }
    if (removedSet == null) {
      removedSet = new HashSet<>();
    }
    for (String fieldName : new IterableIterator<>(innerObjectValue.fieldNames())) {
      removedSet.add(fieldName);
    }

    return this;
  }

  /**
   * Encodes the merged view of overlay and base objects.
   *
   * @param writer destination writer
   * @throws IOException if encoding fails
   */
  @Override
  public void encode (Writer writer)
    throws IOException {

    HashSet<String> writtenSet = new HashSet<>();
    boolean first = true;

    writer.write('{');

    if (outerObjectValue != null) {
      for (String fieldName : new IterableIterator<>(outerObjectValue.fieldNames())) {

        Value<V> value;

        if ((value = outerObjectValue.get(fieldName)) != null) {
          writtenSet.add(fieldName);

          if (!first) {
            writer.write(',');
          }

          writer.write('"');
          writer.write(fieldName);
          writer.write("\":");
          value.encode(writer);

          first = false;
        }
      }
    }

    for (String fieldName : new IterableIterator<>(innerObjectValue.fieldNames())) {
      if ((!writtenSet.contains(fieldName)) && ((removedSet == null) || (!removedSet.contains(fieldName)))) {

        Value<V> value;

        if ((value = innerObjectValue.get(fieldName)) != null) {
          if (!first) {
            writer.write(',');
          }

          writer.write('"');
          writer.write(fieldName);
          writer.write("\":");
          value.encode(writer);

          first = false;
        }
      }
    }

    writer.write('}');
  }
}
