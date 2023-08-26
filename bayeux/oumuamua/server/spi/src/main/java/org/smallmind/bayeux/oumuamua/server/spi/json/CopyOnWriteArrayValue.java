package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;

public class CopyOnWriteArrayValue<V extends Value<V>> implements ArrayValue<V> {

  private final ArrayValue<V> innerArrayValue;
  private ArrayValue<V> outerArrayValue;

  public CopyOnWriteArrayValue (ArrayValue<V> innerArrayValue) {

    this.innerArrayValue = innerArrayValue;
  }

  @Override
  public ValueFactory<V> getFactory () {

    return innerArrayValue.getFactory();
  }

  @Override
  public int size () {

    return  (outerArrayValue == null) ? innerArrayValue.size() : outerArrayValue.size();
  }

  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  @Override
  public V get (int index) {

    return null;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> add (U value) {

    return null;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> set (int index, U value) {

    return null;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> insert (int index, U value) {

    return null;
  }

  @Override
  public V remove (int index) {

    return null;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values) {

    return null;
  }

  @Override
  public ArrayValue<V> removeAll () {

    return null;
  }

  @Override
  public Iterator<V> iterator () {

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
