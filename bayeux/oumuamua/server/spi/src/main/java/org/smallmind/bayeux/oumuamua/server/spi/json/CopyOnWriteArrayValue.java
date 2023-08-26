package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.OutputStream;
import java.util.Collection;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;

public class CopyOnWriteArrayValue<V extends Value<V>> implements ArrayValue<V> {

  private final ArrayValue<V> innerArrayValue;
  private ArrayValue<V> outerArrayValue;

  public CopyOnWriteArrayValue (ArrayValue<V> innerArrayValue) {

    this.innerArrayValue = innerArrayValue;
  }

  private ArrayValue<V> fill () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();
    for (int index = 0; index < innerArrayValue.size(); index++) {
      outerArrayValue.add(innerArrayValue.get(index));
    }

    return outerArrayValue;
  }

  @Override
  public ValueFactory<V> getFactory () {

    return innerArrayValue.getFactory();
  }

  @Override
  public int size () {

    return (outerArrayValue != null) ? outerArrayValue.size() : innerArrayValue.size();
  }

  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  @Override
  public Value<V> get (int index) {

    if (outerArrayValue != null) {

      return outerArrayValue.get(index);
    } else {

      Value<V> value;

      if ((value = innerArrayValue.get(index)) != null) {
        switch (value.getType()) {
          case OBJECT:

            MergingObjectValue<V> mergedValue;

            fill().set(index, mergedValue = new MergingObjectValue<>((ObjectValue<V>)value));

            return mergedValue;
          case ARRAY:

            CopyOnWriteArrayValue<V> copyOnWriteValue;

            fill().set(index, copyOnWriteValue = new CopyOnWriteArrayValue<>((ArrayValue<V>)value));

            return copyOnWriteValue;
          default:

            return value;
        }
      } else {

        return null;
      }
    }
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> add (U value) {

    fill().add(value);

    return this;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> set (int index, U value) {

    fill().set(index, value);

    return this;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> insert (int index, U value) {

     fill().insert(index, value);

     return this;
  }

  @Override
  public Value<V> remove (int index) {

    return fill().remove(index);
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values) {

    fill().addAll(values);

    return this;
  }

  @Override
  public ArrayValue<V> removeAll () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();

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
