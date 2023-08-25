package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
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
  public V get (String field) {

    return null;
  }

  @Override
  public <U extends Value<V>> ObjectValue<V> put (String field, U value) {

    return null;
  }

  @Override
  public V remove (String field) {

    return null;
  }

  @Override
  public ObjectValue<V> removeAll () {

    return null;
  }

  @Override
  public V copy () {

    return null;
  }

  @Override
  public void encode (OutputStream outputStream) {

  }
}
