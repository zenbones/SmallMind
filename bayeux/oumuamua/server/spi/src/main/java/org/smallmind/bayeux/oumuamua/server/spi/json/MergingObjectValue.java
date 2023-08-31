/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class MergingObjectValue<V extends Value<V>> implements ObjectValue<V> {

  private final ObjectValue<V> innerObjectValue;
  private ObjectValue<V> outerObjectValue;
  private HashSet<String> removedSet;

  public MergingObjectValue (ObjectValue<V> innerObjectValue) {

    this.innerObjectValue = innerObjectValue;
  }

  @Override
  public ValueFactory<V> getFactory () {

    return innerObjectValue.getFactory();
  }

  @Override
  public int size () {

    return fieldNameSet().size();
  }

  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  @Override
  public Iterator<String> fieldNames () {

    return fieldNameSet().iterator();
  }

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

  @Override
  public <U extends Value<V>> ObjectValue<V> put (String field, U value) {

    if (outerObjectValue == null) {
      outerObjectValue = innerObjectValue.getFactory().objectValue();
    }

    outerObjectValue.put(field, value);

    return this;
  }

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
