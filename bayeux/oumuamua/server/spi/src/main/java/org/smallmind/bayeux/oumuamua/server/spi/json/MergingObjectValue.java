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
 * Overlay {@link ObjectValue} that layers a mutable outer object on top of an immutable inner object,
 * tracking explicit removals separately so that the merged view always reflects the intended state
 * without modifying the original.
 *
 * @param <V> the concrete {@link Value} subtype carried by this object
 */
public class MergingObjectValue<V extends Value<V>> implements ObjectValue<V> {

  private final ObjectValue<V> innerObjectValue;
  private ObjectValue<V> outerObjectValue;
  private HashSet<String> removedSet;

  /**
   * Wraps {@code innerObjectValue} as the read-through backing object.
   *
   * @param innerObjectValue original object whose fields are exposed through this view; never modified
   */
  public MergingObjectValue (ObjectValue<V> innerObjectValue) {

    this.innerObjectValue = innerObjectValue;
  }

  /**
   * Returns the {@link ValueFactory} associated with the inner object.
   *
   * @return value factory for creating new values of type {@code V}
   */
  @Override
  public ValueFactory<V> getFactory () {

    return innerObjectValue.getFactory();
  }

  /**
   * Returns the number of fields visible in the merged view, accounting for additions and removals.
   *
   * @return distinct field count across inner and outer objects, excluding removed fields
   */
  @Override
  public int size () {

    return fieldNameSet().size();
  }

  /**
   * Reports whether the merged view contains no visible fields.
   *
   * @return {@code true} when the effective field count is zero
   */
  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  /**
   * Returns an iterator over the names of all fields visible in the merged view.
   *
   * @return iterator of field name strings reflecting the current merged state
   */
  @Override
  public Iterator<String> fieldNames () {

    return fieldNameSet().iterator();
  }

  /**
   * Computes the effective set of field names by unioning inner and outer fields, then excluding
   * any names that have been explicitly removed.
   *
   * @return mutable set of currently visible field names
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
   * Retrieves the value for {@code field} from the merged view, giving priority to the outer overlay.
   * Nested objects and arrays read from the inner object are promoted into the overlay as
   * {@link MergingObjectValue} and {@link CopyOnWriteArrayValue} wrappers respectively so that
   * future mutations remain isolated.
   *
   * @param field name of the field to look up
   * @return the effective value, or {@code null} if the field is absent or has been removed
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
   * Stores {@code value} under {@code field} in the outer overlay, creating the overlay lazily,
   * and also removes the field from the removal set if it was previously deleted.
   *
   * @param field name of the field to set
   * @param value value to associate with the field
   * @return this object for chaining
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
   * Removes {@code field} from the merged view by deleting it from the overlay and recording it in
   * the removal set so that the inner object's copy is suppressed.
   *
   * @param field name of the field to remove
   * @return the previously effective value (from the overlay if present, otherwise from the inner object),
   * or {@code null} if the field was not visible
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
   * Clears the merged view by discarding the outer overlay and marking every inner field as removed.
   *
   * @return this object for chaining
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
   * Writes the merged JSON object representation to {@code writer}, emitting overlay fields first
   * and then any inner fields not shadowed by the overlay or suppressed by the removal set.
   *
   * @param writer destination for the JSON output
   * @throws IOException if writing to {@code writer} fails
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
