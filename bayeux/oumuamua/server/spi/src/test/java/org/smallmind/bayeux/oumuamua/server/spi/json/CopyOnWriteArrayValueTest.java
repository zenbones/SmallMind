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
import java.io.StringWriter;
import java.util.List;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CopyOnWriteArrayValueTest {

  private OrthodoxValueFactory factory;

  @BeforeMethod
  public void beforeMethod () {

    factory = new OrthodoxValueFactory();
  }

  private ArrayValue<OrthodoxValue> innerArray (int... values) {

    ArrayValue<OrthodoxValue> array = factory.arrayValue();

    for (int value : values) {
      array.add(factory.numberValue(value));
    }

    return array;
  }

  private String encode (ArrayValue<OrthodoxValue> value)
    throws IOException {

    StringWriter writer = new StringWriter();
    value.encode(writer);

    return writer.toString();
  }

  public void testGetTypeIsArray () {

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(factory.arrayValue());

    Assert.assertEquals(array.getType(), ValueType.ARRAY);
  }

  public void testFactoryDelegatesToInner () {

    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    Assert.assertSame(array.getFactory(), inner.getFactory());
  }

  public void testEmptyWrapperOverEmptyInner () {

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(factory.arrayValue());

    Assert.assertTrue(array.isEmpty());
    Assert.assertEquals(array.size(), 0);
  }

  public void testReadsThroughToInner () {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2, 3);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(0)).asInt(), 1);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(1)).asInt(), 2);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(2)).asInt(), 3);
  }

  public void testAddTriggersCopyAndLeavesInnerUnchanged () {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.add(factory.numberValue(3));

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(2)).asInt(), 3);
    Assert.assertEquals(inner.size(), 2);
  }

  public void testSetTriggersCopyAndLeavesInnerUnchanged () {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2, 3);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.set(1, factory.numberValue(99));

    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(1)).asInt(), 99);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)inner.get(1)).asInt(), 2);
  }

  public void testInsertTriggersCopyAndLeavesInnerUnchanged () {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 3);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.insert(1, factory.numberValue(2));

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(0)).asInt(), 1);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(1)).asInt(), 2);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(2)).asInt(), 3);
    Assert.assertEquals(inner.size(), 2);
  }

  public void testRemoveTriggersCopyAndLeavesInnerUnchanged () {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2, 3);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    Value<OrthodoxValue> removed = array.remove(1);

    Assert.assertEquals(((NumberValue<OrthodoxValue>)removed).asInt(), 2);
    Assert.assertEquals(array.size(), 2);
    Assert.assertEquals(inner.size(), 3);
  }

  public void testAddAllTriggersCopyAndLeavesInnerUnchanged () {

    ArrayValue<OrthodoxValue> inner = innerArray(1);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.addAll(List.of(factory.numberValue(2), factory.numberValue(3)));

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(inner.size(), 1);
  }

  public void testRemoveAllReplacesWithEmpty () {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2, 3);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.removeAll();

    Assert.assertEquals(array.size(), 0);
    Assert.assertTrue(array.isEmpty());
    Assert.assertEquals(inner.size(), 3);
  }

  public void testMultipleMutationsShareSameCopy () {

    ArrayValue<OrthodoxValue> inner = innerArray(1);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.add(factory.numberValue(2));
    array.add(factory.numberValue(3));
    array.set(0, factory.numberValue(99));

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(0)).asInt(), 99);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(1)).asInt(), 2);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(2)).asInt(), 3);
    Assert.assertEquals(inner.size(), 1);
  }

  public void testEncodeUsesInnerWhenNoMutation ()
    throws IOException {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2, 3);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    Assert.assertEquals(encode(array), "[1,2,3]");
  }

  public void testEncodeUsesOuterAfterMutation ()
    throws IOException {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.add(factory.numberValue(3));

    Assert.assertEquals(encode(array), "[1,2,3]");
    Assert.assertEquals(encode(inner), "[1,2]");
  }

  public void testEncodeEmptyAfterRemoveAll ()
    throws IOException {

    ArrayValue<OrthodoxValue> inner = innerArray(1, 2, 3);
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.removeAll();

    Assert.assertEquals(encode(array), "[]");
  }

  public void testNestedObjectIsAutoWrapped () {

    ObjectValue<OrthodoxValue> child = factory.objectValue();
    child.put("k", factory.numberValue(1));

    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    inner.add(child);

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);
    Value<OrthodoxValue> result = array.get(0);

    Assert.assertTrue(result instanceof MergingObjectValue);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)((ObjectValue<OrthodoxValue>)result).get("k")).asInt(), 1);
  }

  public void testNestedObjectMutationIsolatedFromInner () {

    ObjectValue<OrthodoxValue> child = factory.objectValue();
    child.put("k", factory.numberValue(1));

    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    inner.add(child);

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);
    ObjectValue<OrthodoxValue> wrapped = (ObjectValue<OrthodoxValue>)array.get(0);

    wrapped.put("k", factory.numberValue(99));

    Assert.assertEquals(((NumberValue<OrthodoxValue>)wrapped.get("k")).asInt(), 99);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)child.get("k")).asInt(), 1);
  }

  public void testNestedArrayIsAutoWrapped () {

    ArrayValue<OrthodoxValue> child = innerArray(7, 8);
    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    inner.add(child);

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);
    Value<OrthodoxValue> result = array.get(0);

    Assert.assertTrue(result instanceof CopyOnWriteArrayValue);
    Assert.assertEquals(((ArrayValue<OrthodoxValue>)result).size(), 2);
  }

  public void testNestedArrayMutationIsolatedFromInner () {

    ArrayValue<OrthodoxValue> child = innerArray(7, 8);
    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    inner.add(child);

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);
    ArrayValue<OrthodoxValue> wrapped = (ArrayValue<OrthodoxValue>)array.get(0);

    wrapped.add(factory.numberValue(9));

    Assert.assertEquals(wrapped.size(), 3);
    Assert.assertEquals(child.size(), 2);
  }

  public void testNestedAccessMaterializesOuter ()
    throws IOException {

    ObjectValue<OrthodoxValue> child = factory.objectValue();
    child.put("k", factory.numberValue(1));

    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    inner.add(child);

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);
    Value<OrthodoxValue> firstAccess = array.get(0);
    Value<OrthodoxValue> secondAccess = array.get(0);

    Assert.assertSame(firstAccess, secondAccess);
  }

  public void testIsEmptyReturnsTrueForEmptyInnerArray () {

    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    Assert.assertTrue(array.isEmpty(), "An empty inner array must report empty before any mutation");
    Assert.assertEquals(array.size(), 0);
  }

  public void testIsEmptyReturnsFalseAfterAdd () {

    ArrayValue<OrthodoxValue> inner = factory.arrayValue();
    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);

    array.add(factory.numberValue(42));

    Assert.assertFalse(array.isEmpty(), "Adding an element through the overlay must make the array non-empty");
  }

  public void testGetReturnsNullWhenInnerSlotIsNull () {

    ArrayValue<OrthodoxValue> inner = factory.arrayValue();

    inner.add(factory.nullValue());

    CopyOnWriteArrayValue<OrthodoxValue> array = new CopyOnWriteArrayValue<>(inner);
    Value<OrthodoxValue> value = array.get(0);

    Assert.assertNotNull(value, "Explicit null sentinel must round-trip");
    Assert.assertEquals(value.getType(), ValueType.NULL);
  }
}
