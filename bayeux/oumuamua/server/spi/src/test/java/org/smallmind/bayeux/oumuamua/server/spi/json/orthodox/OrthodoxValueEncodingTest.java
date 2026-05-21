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
package org.smallmind.bayeux.oumuamua.server.spi.json.orthodox;

import java.io.IOException;
import java.io.StringWriter;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxDoubleValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxLongValue;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OrthodoxValueEncodingTest {

  private OrthodoxValueFactory factory;

  @BeforeClass
  public void beforeClass () {

    factory = new OrthodoxValueFactory();
  }

  private String encode (Value<OrthodoxValue> value)
    throws IOException {

    StringWriter writer = new StringWriter();
    value.encode(writer);

    return writer.toString();
  }

  public void testEncodeBooleanTrue ()
    throws IOException {

    Assert.assertEquals(encode(factory.booleanValue(true)), "true");
  }

  public void testEncodeBooleanFalse ()
    throws IOException {

    Assert.assertEquals(encode(factory.booleanValue(false)), "false");
  }

  public void testEncodeNull ()
    throws IOException {

    Assert.assertEquals(encode(factory.nullValue()), "null");
  }

  public void testEncodeInteger ()
    throws IOException {

    Assert.assertEquals(encode(factory.numberValue(0)), "0");
    Assert.assertEquals(encode(factory.numberValue(42)), "42");
    Assert.assertEquals(encode(factory.numberValue(-7)), "-7");
    Assert.assertEquals(encode(factory.numberValue(Integer.MAX_VALUE)), String.valueOf(Integer.MAX_VALUE));
  }

  public void testEncodeLong ()
    throws IOException {

    Assert.assertEquals(encode(factory.numberValue(9_000_000_000L)), "9000000000");
    Assert.assertEquals(encode(factory.numberValue(Long.MIN_VALUE)), String.valueOf(Long.MIN_VALUE));
  }

  public void testEncodeDouble ()
    throws IOException {

    Assert.assertEquals(encode(factory.numberValue(0.5d)), "0.5");
    Assert.assertEquals(encode(factory.numberValue(-1.25d)), "-1.25");
  }

  public void testEncodeEmptyString ()
    throws IOException {

    Assert.assertEquals(encode(factory.textValue("")), "\"\"");
  }

  public void testEncodePlainString ()
    throws IOException {

    Assert.assertEquals(encode(factory.textValue("hello")), "\"hello\"");
  }

  public void testEncodeStringWithEscapes ()
    throws IOException {

    Assert.assertEquals(encode(factory.textValue("a\"b")), "\"a\\\"b\"");
    Assert.assertEquals(encode(factory.textValue("a\\b")), "\"a\\\\b\"");
    Assert.assertEquals(encode(factory.textValue("a\nb")), "\"a\\nb\"");
    Assert.assertEquals(encode(factory.textValue("a\tb")), "\"a\\tb\"");
  }

  public void testEncodeEmptyObject ()
    throws IOException {

    Assert.assertEquals(encode(factory.objectValue()), "{}");
  }

  public void testEncodeObjectSingleField ()
    throws IOException {

    ObjectValue<OrthodoxValue> obj = factory.objectValue();
    obj.put("name", factory.textValue("alice"));

    Assert.assertEquals(encode(obj), "{\"name\":\"alice\"}");
  }

  public void testEncodeObjectSkipsNullEntries ()
    throws IOException {

    ObjectValue<OrthodoxValue> obj = factory.objectValue();
    obj.put("dropped", (Value<OrthodoxValue>)null);
    obj.put("kept", factory.numberValue(1));

    Assert.assertEquals(encode(obj), "{\"kept\":1}");
  }

  public void testEncodeEmptyArray ()
    throws IOException {

    Assert.assertEquals(encode(factory.arrayValue()), "[]");
  }

  public void testEncodeArraySingleElement ()
    throws IOException {

    ArrayValue<OrthodoxValue> arr = factory.arrayValue();
    arr.add(factory.numberValue(42));

    Assert.assertEquals(encode(arr), "[42]");
  }

  public void testEncodeArrayMultipleElements ()
    throws IOException {

    ArrayValue<OrthodoxValue> arr = factory.arrayValue();
    arr.add(factory.numberValue(1));
    arr.add(factory.numberValue(2));
    arr.add(factory.numberValue(3));

    Assert.assertEquals(encode(arr), "[1,2,3]");
  }

  public void testEncodeArrayMixedTypes ()
    throws IOException {

    ArrayValue<OrthodoxValue> arr = factory.arrayValue();
    arr.add(factory.booleanValue(true));
    arr.add(factory.nullValue());
    arr.add(factory.textValue("x"));

    Assert.assertEquals(encode(arr), "[true,null,\"x\"]");
  }

  public void testEncodeArraySkipsNullEntries ()
    throws IOException {

    ArrayValue<OrthodoxValue> arr = factory.arrayValue();
    arr.add(factory.numberValue(1));
    arr.add((Value<OrthodoxValue>)null);
    arr.add(factory.numberValue(2));

    Assert.assertEquals(encode(arr), "[1,2]");
  }

  public void testEncodeNestedObjectInsideArray ()
    throws IOException {

    ObjectValue<OrthodoxValue> obj = factory.objectValue();
    obj.put("k", factory.numberValue(7));

    ArrayValue<OrthodoxValue> arr = factory.arrayValue();
    arr.add(obj);

    Assert.assertEquals(encode(arr), "[{\"k\":7}]");
  }

  public void testEncodeNestedArrayInsideObject ()
    throws IOException {

    ArrayValue<OrthodoxValue> arr = factory.arrayValue();
    arr.add(factory.numberValue(1));
    arr.add(factory.numberValue(2));

    ObjectValue<OrthodoxValue> obj = factory.objectValue();
    obj.put("items", arr);

    Assert.assertEquals(encode(obj), "{\"items\":[1,2]}");
  }

  public void testToStringDelegatesToEncode ()
    throws IOException {

    Value<OrthodoxValue> value = factory.numberValue(99);

    Assert.assertEquals(value.toString(), encode(value));
  }

  public void testDoubleAsInt () {

    OrthodoxDoubleValue dv = (OrthodoxDoubleValue)factory.numberValue(3.9d);

    Assert.assertEquals(dv.asInt(), 3);
    Assert.assertEquals(((OrthodoxDoubleValue)factory.numberValue(-2.7d)).asInt(), -2);
  }

  public void testDoubleAsLong () {

    OrthodoxDoubleValue dv = (OrthodoxDoubleValue)factory.numberValue(1.5e10d);

    Assert.assertEquals(dv.asLong(), 15_000_000_000L);
  }

  public void testDoubleAsDouble () {

    OrthodoxDoubleValue dv = (OrthodoxDoubleValue)factory.numberValue(Math.PI);

    Assert.assertEquals(dv.asDouble(), Math.PI, 0.0);
  }

  public void testDoubleAsNumber () {

    OrthodoxDoubleValue dv = (OrthodoxDoubleValue)factory.numberValue(7.5d);

    Assert.assertEquals(dv.asNumber(), 7.5d);
    Assert.assertTrue(dv.asNumber() instanceof Double);
  }

  public void testLongAsInt () {

    OrthodoxLongValue lv = (OrthodoxLongValue)factory.numberValue(0xFFFFFFFFL);

    Assert.assertEquals(lv.asInt(), -1);
  }

  public void testLongAsLong () {

    OrthodoxLongValue lv = (OrthodoxLongValue)factory.numberValue(Long.MAX_VALUE);

    Assert.assertEquals(lv.asLong(), Long.MAX_VALUE);
  }

  public void testLongAsDouble () {

    OrthodoxLongValue lv = (OrthodoxLongValue)factory.numberValue(100L);

    Assert.assertEquals(lv.asDouble(), 100.0d, 0.0);
  }

  public void testLongAsNumber () {

    OrthodoxLongValue lv = (OrthodoxLongValue)factory.numberValue(42L);

    Assert.assertEquals(lv.asNumber(), 42L);
    Assert.assertTrue(lv.asNumber() instanceof Long);
  }
}
