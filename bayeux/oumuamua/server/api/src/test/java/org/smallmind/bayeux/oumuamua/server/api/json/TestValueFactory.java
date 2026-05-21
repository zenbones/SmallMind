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
package org.smallmind.bayeux.oumuamua.server.api.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal in-memory {@link ValueFactory} used by API-level default-method tests so they
 * can exercise the interface defaults without depending on the SPI's orthodox codec.
 */
public final class TestValueFactory implements ValueFactory<TestValueFactory.TestValue> {

  @Override
  public ObjectValue<TestValue> objectValue () {

    return new TestObject(this);
  }

  @Override
  public ArrayValue<TestValue> arrayValue () {

    return new TestArray(this);
  }

  @Override
  public StringValue<TestValue> textValue (String text) {

    return new TestText(this, text);
  }

  @Override
  public NumberValue<TestValue> numberValue (int i) {

    return new TestNumber(this, i, NumberType.INTEGER);
  }

  @Override
  public NumberValue<TestValue> numberValue (long l) {

    return new TestNumber(this, l, NumberType.LONG);
  }

  @Override
  public NumberValue<TestValue> numberValue (double d) {

    return new TestNumber(this, d, NumberType.DOUBLE);
  }

  @Override
  public BooleanValue<TestValue> booleanValue (boolean bool) {

    return new TestBool(this, bool);
  }

  @Override
  public NullValue<TestValue> nullValue () {

    return new TestNull(this);
  }

  public TestMessage message () {

    return new TestMessage(this);
  }

  /**
   * Self-referential value bound for the test factory.
   */
  public interface TestValue extends Value<TestValue> {

  }

  /**
   * Concrete object value backed by a {@link LinkedHashMap}.
   */
  public static class TestObject implements ObjectValue<TestValue>, TestValue {

    private final TestValueFactory factory;
    private final LinkedHashMap<String, Value<TestValue>> map = new LinkedHashMap<>();

    TestObject (TestValueFactory factory) {

      this.factory = factory;
    }

    @Override
    public ValueFactory<TestValue> getFactory () {

      return factory;
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      writer.write("{}");
    }

    @Override
    public int size () {

      return map.size();
    }

    @Override
    public boolean isEmpty () {

      return map.isEmpty();
    }

    @Override
    public Iterator<String> fieldNames () {

      return map.keySet().iterator();
    }

    @Override
    public Value<TestValue> get (String field) {

      return map.get(field);
    }

    @Override
    public <U extends Value<TestValue>> ObjectValue<TestValue> put (String field, U value) {

      map.put(field, value);

      return this;
    }

    @Override
    public Value<TestValue> remove (String field) {

      return map.remove(field);
    }

    @Override
    public ObjectValue<TestValue> removeAll () {

      map.clear();

      return this;
    }
  }

  /**
   * Concrete array value backed by an {@link ArrayList}.
   */
  public static class TestArray implements ArrayValue<TestValue>, TestValue {

    private final TestValueFactory factory;
    private final List<Value<TestValue>> list = new ArrayList<>();

    TestArray (TestValueFactory factory) {

      this.factory = factory;
    }

    @Override
    public ValueFactory<TestValue> getFactory () {

      return factory;
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      writer.write("[]");
    }

    @Override
    public int size () {

      return list.size();
    }

    @Override
    public boolean isEmpty () {

      return list.isEmpty();
    }

    @Override
    public Value<TestValue> get (int index) {

      return list.get(index);
    }

    @Override
    public <U extends Value<TestValue>> ArrayValue<TestValue> add (U value) {

      list.add(value);

      return this;
    }

    @Override
    public <U extends Value<TestValue>> ArrayValue<TestValue> set (int index, U value) {

      list.set(index, value);

      return this;
    }

    @Override
    public <U extends Value<TestValue>> ArrayValue<TestValue> insert (int index, U value) {

      list.add(index, value);

      return this;
    }

    @Override
    public Value<TestValue> remove (int index) {

      return list.remove(index);
    }

    @Override
    public <U extends Value<TestValue>> ArrayValue<TestValue> addAll (java.util.Collection<U> values) {

      list.addAll(values);

      return this;
    }

    @Override
    public ArrayValue<TestValue> removeAll () {

      list.clear();

      return this;
    }
  }

  /**
   * Concrete number value backed by a primitive double plus a number-type tag.
   */
  public static class TestNumber implements NumberValue<TestValue>, TestValue {

    private final TestValueFactory factory;
    private final double number;
    private final NumberType numberType;

    TestNumber (TestValueFactory factory, double number, NumberType numberType) {

      this.factory = factory;
      this.number = number;
      this.numberType = numberType;
    }

    @Override
    public ValueFactory<TestValue> getFactory () {

      return factory;
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      writer.write(Double.toString(number));
    }

    @Override
    public NumberType getNumberType () {

      return numberType;
    }

    @Override
    public Number asNumber () {

      return number;
    }

    @Override
    public int asInt () {

      return (int)number;
    }

    @Override
    public long asLong () {

      return (long)number;
    }

    @Override
    public double asDouble () {

      return number;
    }
  }

  /**
   * Concrete boolean value.
   */
  public static class TestBool implements BooleanValue<TestValue>, TestValue {

    private final TestValueFactory factory;
    private final boolean bool;

    TestBool (TestValueFactory factory, boolean bool) {

      this.factory = factory;
      this.bool = bool;
    }

    @Override
    public ValueFactory<TestValue> getFactory () {

      return factory;
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      writer.write(Boolean.toString(bool));
    }

    @Override
    public boolean asBoolean () {

      return bool;
    }
  }

  /**
   * Concrete text value.
   */
  public static class TestText implements StringValue<TestValue>, TestValue {

    private final TestValueFactory factory;
    private final String text;

    TestText (TestValueFactory factory, String text) {

      this.factory = factory;
      this.text = text;
    }

    @Override
    public ValueFactory<TestValue> getFactory () {

      return factory;
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      writer.write('"');
      writer.write(text);
      writer.write('"');
    }

    @Override
    public String asText () {

      return text;
    }
  }

  /**
   * Concrete null value.
   */
  public static class TestNull implements NullValue<TestValue>, TestValue {

    private final TestValueFactory factory;

    TestNull (TestValueFactory factory) {

      this.factory = factory;
    }

    @Override
    public ValueFactory<TestValue> getFactory () {

      return factory;
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      writer.write("null");
    }
  }

  /**
   * Concrete {@link Message} implementation delegating object operations to a backing
   * {@link TestObject}.
   */
  public static class TestMessage implements Message<TestValue> {

    private final TestObject backing;

    TestMessage (TestValueFactory factory) {

      this.backing = new TestObject(factory);
    }

    @Override
    public ValueFactory<TestValue> getFactory () {

      return backing.getFactory();
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      backing.encode(writer);
    }

    @Override
    public int size () {

      return backing.size();
    }

    @Override
    public boolean isEmpty () {

      return backing.isEmpty();
    }

    @Override
    public Iterator<String> fieldNames () {

      return backing.fieldNames();
    }

    @Override
    public Value<TestValue> get (String field) {

      return backing.get(field);
    }

    @Override
    public <U extends Value<TestValue>> ObjectValue<TestValue> put (String field, U value) {

      return backing.put(field, value);
    }

    @Override
    public Value<TestValue> remove (String field) {

      return backing.remove(field);
    }

    @Override
    public ObjectValue<TestValue> removeAll () {

      return backing.removeAll();
    }

    public Map<String, Value<TestValue>> snapshot () {

      LinkedHashMap<String, Value<TestValue>> copy = new LinkedHashMap<>();
      Iterator<String> fields = backing.fieldNames();

      while (fields.hasNext()) {
        String key = fields.next();

        copy.put(key, backing.get(key));
      }

      return copy;
    }
  }
}
