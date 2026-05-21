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
import java.util.HashSet;
import java.util.Iterator;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MergingObjectValueTest {

  private OrthodoxValueFactory factory;

  @BeforeMethod
  public void beforeMethod () {

    factory = new OrthodoxValueFactory();
  }

  private String encode (ObjectValue<OrthodoxValue> value)
    throws IOException {

    StringWriter writer = new StringWriter();
    value.encode(writer);

    return writer.toString();
  }

  public void testFactoryDelegatesToInner () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    Assert.assertSame(merged.getFactory(), inner.getFactory());
  }

  public void testEmptyWrapperOverEmptyInner () {

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(factory.objectValue());

    Assert.assertTrue(merged.isEmpty());
    Assert.assertEquals(merged.size(), 0);
    Assert.assertFalse(merged.fieldNames().hasNext());
    Assert.assertNull(merged.get("anything"));
  }

  public void testReadsThroughToInner () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("k", factory.textValue("v"));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    Assert.assertEquals(((StringValue<OrthodoxValue>)merged.get("k")).asText(), "v");
    Assert.assertEquals(merged.size(), 1);
    Assert.assertFalse(merged.isEmpty());
  }

  public void testPutAddsNewField () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.put("k", factory.textValue("v"));

    Assert.assertEquals(((StringValue<OrthodoxValue>)merged.get("k")).asText(), "v");
    Assert.assertEquals(merged.size(), 1);
    Assert.assertEquals(inner.size(), 0);
  }

  public void testPutOverridesInner () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("k", factory.numberValue(1));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    merged.put("k", factory.numberValue(2));

    Assert.assertEquals(((NumberValue<OrthodoxValue>)merged.get("k")).asInt(), 2);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)inner.get("k")).asInt(), 1);
  }

  public void testRemoveHidesInnerField () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("k", factory.textValue("v"));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    Value<OrthodoxValue> removed = merged.remove("k");

    Assert.assertEquals(((StringValue<OrthodoxValue>)removed).asText(), "v");
    Assert.assertNull(merged.get("k"));
    Assert.assertTrue(merged.isEmpty());
    Assert.assertEquals(inner.size(), 1);
    Assert.assertNotNull(inner.get("k"));
  }

  public void testRemoveOuterReturnsOuterValue () {

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(factory.objectValue());
    merged.put("k", factory.textValue("outer"));

    Value<OrthodoxValue> removed = merged.remove("k");

    Assert.assertEquals(((StringValue<OrthodoxValue>)removed).asText(), "outer");
    Assert.assertNull(merged.get("k"));
  }

  public void testRemoveUnknownReturnsNull () {

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(factory.objectValue());

    Assert.assertNull(merged.remove("absent"));
  }

  public void testReputAfterRemove () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("k", factory.textValue("inner"));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    merged.remove("k");
    merged.put("k", factory.textValue("replacement"));

    Assert.assertEquals(((StringValue<OrthodoxValue>)merged.get("k")).asText(), "replacement");
    Assert.assertEquals(((StringValue<OrthodoxValue>)inner.get("k")).asText(), "inner");
  }

  public void testRemoveAllHidesEverything () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("a", factory.numberValue(1));
    inner.put("b", factory.numberValue(2));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    merged.put("c", factory.numberValue(3));
    merged.removeAll();

    Assert.assertTrue(merged.isEmpty());
    Assert.assertEquals(merged.size(), 0);
    Assert.assertNull(merged.get("a"));
    Assert.assertNull(merged.get("b"));
    Assert.assertNull(merged.get("c"));
    Assert.assertEquals(inner.size(), 2);
  }

  public void testSizeReflectsMergedView () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("a", factory.numberValue(1));
    inner.put("b", factory.numberValue(2));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    merged.put("c", factory.numberValue(3));

    Assert.assertEquals(merged.size(), 3);

    merged.put("a", factory.numberValue(99));

    Assert.assertEquals(merged.size(), 3);
  }

  public void testFieldNamesReflectsMergedView () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("a", factory.numberValue(1));
    inner.put("b", factory.numberValue(2));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    merged.put("c", factory.numberValue(3));
    merged.remove("a");

    HashSet<String> names = new HashSet<>();
    Iterator<String> iterator = merged.fieldNames();

    while (iterator.hasNext()) {
      names.add(iterator.next());
    }

    Assert.assertEquals(names.size(), 2);
    Assert.assertTrue(names.contains("b"));
    Assert.assertTrue(names.contains("c"));
    Assert.assertFalse(names.contains("a"));
  }

  public void testNestedObjectIsAutoWrapped () {

    ObjectValue<OrthodoxValue> child = factory.objectValue();
    child.put("x", factory.numberValue(1));

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("child", child);

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    Value<OrthodoxValue> result = merged.get("child");

    Assert.assertTrue(result instanceof MergingObjectValue);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)((ObjectValue<OrthodoxValue>)result).get("x")).asInt(), 1);
  }

  public void testNestedObjectMutationIsolatedFromInner () {

    ObjectValue<OrthodoxValue> child = factory.objectValue();
    child.put("x", factory.numberValue(1));

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("child", child);

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    ObjectValue<OrthodoxValue> wrapped = (ObjectValue<OrthodoxValue>)merged.get("child");

    wrapped.put("y", factory.numberValue(2));

    Assert.assertEquals(((NumberValue<OrthodoxValue>)wrapped.get("y")).asInt(), 2);
    Assert.assertNull(child.get("y"));
    Assert.assertEquals(child.size(), 1);
  }

  public void testNestedObjectGetReturnsSameWrapper () {

    ObjectValue<OrthodoxValue> child = factory.objectValue();
    child.put("x", factory.numberValue(1));

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("child", child);

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    Assert.assertSame(merged.get("child"), merged.get("child"));
  }

  public void testNestedArrayIsAutoWrapped () {

    ArrayValue<OrthodoxValue> array = factory.arrayValue();
    array.add(factory.numberValue(1));

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("items", array);

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    Value<OrthodoxValue> result = merged.get("items");

    Assert.assertTrue(result instanceof CopyOnWriteArrayValue);
  }

  public void testEncodeEmpty ()
    throws IOException {

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(factory.objectValue());

    Assert.assertEquals(encode(merged), "{}");
  }

  public void testEncodeReadOnlyMerged ()
    throws IOException {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("k", factory.textValue("v"));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    Assert.assertEquals(encode(merged), "{\"k\":\"v\"}");
  }

  public void testEncodeWithOverlay ()
    throws IOException {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("k", factory.numberValue(1));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    merged.put("k", factory.numberValue(2));

    Assert.assertEquals(encode(merged), "{\"k\":2}");
  }

  public void testEncodeAfterRemove ()
    throws IOException {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    inner.put("k", factory.numberValue(1));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    merged.remove("k");

    Assert.assertEquals(encode(merged), "{}");
  }

  public void testInnerNotMutatedByPut () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.put("a", factory.numberValue(1));
    merged.put("b", factory.numberValue(2));

    Assert.assertTrue(inner.isEmpty());
  }

  public void testGetUnknownReturnsNull () {

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(factory.objectValue());
    merged.put("a", factory.numberValue(1));

    Assert.assertNull(merged.get("absent"));
  }

  public void testRemoveTwiceReturnsNullSecondTime () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("k", factory.textValue("v"));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    Assert.assertNotNull(merged.remove("k"));
    Assert.assertNull(merged.remove("k"));
  }

  public void testRemoveAllThenPutRestoresOverlayVisibility () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("a", factory.numberValue(1));
    inner.put("b", factory.numberValue(2));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.removeAll();
    merged.put("a", factory.numberValue(99));

    Assert.assertNotNull(merged.get("a"));
    Assert.assertEquals(((NumberValue<OrthodoxValue>)merged.get("a")).asInt(), 99);
    Assert.assertNull(merged.get("b"));
  }

  public void testOverlayTypeMismatchKeepsOverlayValue () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("k", factory.textValue("string"));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.put("k", factory.objectValue().put("nested", factory.numberValue(1)));

    Value<OrthodoxValue> value = merged.get("k");

    Assert.assertEquals(value.getType(), ValueType.OBJECT);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)((ObjectValue<OrthodoxValue>)value).get("nested")).asInt(), 1);
    Assert.assertEquals(((StringValue<OrthodoxValue>)inner.get("k")).asText(), "string");
  }

  public void testNestedMergeIsolatesDeepMutation () {

    ObjectValue<OrthodoxValue> grandchild = factory.objectValue();

    grandchild.put("g", factory.numberValue(1));

    ObjectValue<OrthodoxValue> child = factory.objectValue();

    child.put("c", grandchild);

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("a", child);

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);
    ObjectValue<OrthodoxValue> wrappedChild = (ObjectValue<OrthodoxValue>)merged.get("a");
    ObjectValue<OrthodoxValue> wrappedGrandchild = (ObjectValue<OrthodoxValue>)wrappedChild.get("c");

    wrappedGrandchild.put("g", factory.numberValue(99));
    wrappedGrandchild.put("h", factory.numberValue(42));

    Assert.assertEquals(((NumberValue<OrthodoxValue>)wrappedGrandchild.get("g")).asInt(), 99);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)wrappedGrandchild.get("h")).asInt(), 42);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)grandchild.get("g")).asInt(), 1);
    Assert.assertNull(grandchild.get("h"));
  }

  public void testRemoveUnknownInnerKeyDoesNotCreateRemovedEntry () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    Assert.assertNull(merged.remove("ghost"));
    inner.put("ghost", factory.numberValue(7));
    Assert.assertNotNull(merged.get("ghost"));
  }

  public void testGetObjectFieldAfterOuterCreatedReusesExistingOverlay () {

    ObjectValue<OrthodoxValue> nested = factory.objectValue();

    nested.put("k", factory.numberValue(1));

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("nested", nested);

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.put("marker", factory.textValue("v"));

    ObjectValue<OrthodoxValue> wrapped = (ObjectValue<OrthodoxValue>)merged.get("nested");

    Assert.assertNotNull(wrapped);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)wrapped.get("k")).asInt(), 1);
  }

  public void testGetArrayFieldAfterOuterCreatedReusesExistingOverlay () {

    ArrayValue<OrthodoxValue> arr = factory.arrayValue();

    arr.add(factory.numberValue(7));

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("arr", arr);

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.put("marker", factory.textValue("v"));

    ArrayValue<OrthodoxValue> wrappedArray = (ArrayValue<OrthodoxValue>)merged.get("arr");

    Assert.assertNotNull(wrappedArray);
    Assert.assertEquals(wrappedArray.size(), 1);
  }

  public void testRemoveSecondFieldReusesExistingRemovedSet () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("a", factory.numberValue(1));
    inner.put("b", factory.numberValue(2));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    Assert.assertNotNull(merged.remove("a"));
    Assert.assertNotNull(merged.remove("b"));
    Assert.assertNull(merged.get("a"));
    Assert.assertNull(merged.get("b"));
  }

  public void testRemoveAllAfterRemoveReusesExistingRemovedSet () {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();

    inner.put("a", factory.numberValue(1));
    inner.put("b", factory.numberValue(2));

    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.remove("a");
    merged.removeAll();

    Assert.assertNull(merged.get("a"));
    Assert.assertNull(merged.get("b"));
    Assert.assertEquals(merged.size(), 0);
  }

  public void testEncodeWithOnlySingleOuterFieldHonorsFirstFlag ()
    throws IOException {

    ObjectValue<OrthodoxValue> inner = factory.objectValue();
    MergingObjectValue<OrthodoxValue> merged = new MergingObjectValue<>(inner);

    merged.put("only", factory.textValue("value"));

    String json = encode(merged);

    Assert.assertEquals(json, "{\"only\":\"value\"}");
    Assert.assertFalse(json.contains(",,"), "Single-field encode must not emit any commas");
  }
}
