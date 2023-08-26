package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.OutputStream;
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
  public V copy () {

    return null;
  }

  @Override
  public void encode (OutputStream outputStream) {

  }
}
