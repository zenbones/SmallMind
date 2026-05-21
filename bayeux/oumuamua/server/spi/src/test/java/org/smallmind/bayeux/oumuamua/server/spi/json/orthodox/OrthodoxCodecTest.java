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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberType;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OrthodoxCodecTest {

  private OrthodoxCodec codec;

  @BeforeClass
  public void beforeClass () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
  }

  public void testCreateReturnsEmptyMessage () {

    Message<OrthodoxValue> message = codec.create();

    Assert.assertNotNull(message);
    Assert.assertTrue(message.isEmpty());
    Assert.assertEquals(message.size(), 0);
    Assert.assertEquals(message.getType(), ValueType.OBJECT);
    Assert.assertTrue(message.getFactory() instanceof OrthodoxValueFactory);
  }

  public void testDecodeSingleObjectYieldsOneMessage ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"channel\":\"/foo\"}");

    Assert.assertEquals(messages.length, 1);
    Assert.assertEquals(messages[0].getChannel(), "/foo");
  }

  public void testDecodeEmptyObjectYieldsEmptyMessage ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{}");

    Assert.assertEquals(messages.length, 1);
    Assert.assertTrue(messages[0].isEmpty());
  }

  public void testDecodeArrayYieldsMultipleMessages ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("[{\"channel\":\"/a\"},{\"channel\":\"/b\"},{\"channel\":\"/c\"}]");

    Assert.assertEquals(messages.length, 3);
    Assert.assertEquals(messages[0].getChannel(), "/a");
    Assert.assertEquals(messages[1].getChannel(), "/b");
    Assert.assertEquals(messages[2].getChannel(), "/c");
  }

  public void testDecodeEmptyArrayYieldsZeroMessages ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("[]");

    Assert.assertEquals(messages.length, 0);
  }

  public void testDecodeBayeuxMetaFields ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"channel\":\"/meta/handshake\",\"id\":\"42\",\"clientId\":\"abc\",\"successful\":true}");

    Assert.assertEquals(messages.length, 1);

    Message<OrthodoxValue> message = messages[0];

    Assert.assertEquals(message.getChannel(), "/meta/handshake");
    Assert.assertEquals(message.getId(), "42");
    Assert.assertEquals(message.getSessionId(), "abc");
    Assert.assertTrue(message.isSuccessful());
  }

  public void testDecodeBooleanField ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"yes\":true,\"no\":false}");

    Value<OrthodoxValue> yes = messages[0].get("yes");
    Value<OrthodoxValue> no = messages[0].get("no");

    Assert.assertEquals(yes.getType(), ValueType.BOOLEAN);
    Assert.assertEquals(no.getType(), ValueType.BOOLEAN);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)yes).asBoolean());
    Assert.assertFalse(((BooleanValue<OrthodoxValue>)no).asBoolean());
  }

  public void testDecodeNullField ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"absent\":null}");

    Value<OrthodoxValue> value = messages[0].get("absent");

    Assert.assertNotNull(value);
    Assert.assertEquals(value.getType(), ValueType.NULL);
  }

  public void testDecodeIntegerField ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"n\":42}");

    Value<OrthodoxValue> value = messages[0].get("n");

    Assert.assertEquals(value.getType(), ValueType.NUMBER);

    NumberValue<OrthodoxValue> number = (NumberValue<OrthodoxValue>)value;

    Assert.assertEquals(number.getNumberType(), NumberType.INTEGER);
    Assert.assertEquals(number.asInt(), 42);
  }

  public void testDecodeLongField ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"n\":9000000000}");

    Value<OrthodoxValue> value = messages[0].get("n");
    NumberValue<OrthodoxValue> number = (NumberValue<OrthodoxValue>)value;

    Assert.assertEquals(number.getNumberType(), NumberType.LONG);
    Assert.assertEquals(number.asLong(), 9_000_000_000L);
  }

  public void testDecodeDoubleField ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"n\":3.5}");

    Value<OrthodoxValue> value = messages[0].get("n");
    NumberValue<OrthodoxValue> number = (NumberValue<OrthodoxValue>)value;

    Assert.assertEquals(number.getNumberType(), NumberType.DOUBLE);
    Assert.assertEquals(number.asDouble(), 3.5d);
  }

  public void testDecodeStringField ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"s\":\"hello\"}");

    Value<OrthodoxValue> value = messages[0].get("s");

    Assert.assertEquals(value.getType(), ValueType.STRING);
    Assert.assertEquals(((StringValue<OrthodoxValue>)value).asText(), "hello");
  }

  public void testDecodeStringWithEscapes ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"s\":\"a\\\"b\\nc\"}");

    StringValue<OrthodoxValue> value = (StringValue<OrthodoxValue>)messages[0].get("s");

    Assert.assertEquals(value.asText(), "a\"b\nc");
  }

  public void testDecodeNestedObject ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"data\":{\"k\":7}}");

    ObjectValue<OrthodoxValue> data = messages[0].getData();

    Assert.assertNotNull(data);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)data.get("k")).asInt(), 7);
  }

  public void testDecodeNestedArray ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"items\":[1,2,3]}");

    Value<OrthodoxValue> value = messages[0].get("items");

    Assert.assertEquals(value.getType(), ValueType.ARRAY);

    ArrayValue<OrthodoxValue> array = (ArrayValue<OrthodoxValue>)value;

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(0)).asInt(), 1);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(1)).asInt(), 2);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(2)).asInt(), 3);
  }

  public void testDecodeMixedTypeArray ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"mixed\":[true,null,\"x\",42]}");

    ArrayValue<OrthodoxValue> array = (ArrayValue<OrthodoxValue>)messages[0].get("mixed");

    Assert.assertEquals(array.size(), 4);
    Assert.assertEquals(array.get(0).getType(), ValueType.BOOLEAN);
    Assert.assertEquals(array.get(1).getType(), ValueType.NULL);
    Assert.assertEquals(array.get(2).getType(), ValueType.STRING);
    Assert.assertEquals(array.get(3).getType(), ValueType.NUMBER);
  }

  public void testDecodeDeeplyNested ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"a\":{\"b\":{\"c\":[{\"d\":\"deep\"}]}}}");

    ObjectValue<OrthodoxValue> a = (ObjectValue<OrthodoxValue>)messages[0].get("a");
    ObjectValue<OrthodoxValue> b = (ObjectValue<OrthodoxValue>)a.get("b");
    ArrayValue<OrthodoxValue> c = (ArrayValue<OrthodoxValue>)b.get("c");
    ObjectValue<OrthodoxValue> first = (ObjectValue<OrthodoxValue>)c.get(0);

    Assert.assertEquals(((StringValue<OrthodoxValue>)first.get("d")).asText(), "deep");
  }

  public void testDecodedMessageUsesOrthodoxFactory ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"channel\":\"/foo\"}");

    Assert.assertTrue(messages[0].getFactory() instanceof OrthodoxValueFactory);
  }

  public void testDecodeFromBytes ()
    throws IOException {

    byte[] buffer = "{\"channel\":\"/foo\",\"id\":\"1\"}".getBytes(StandardCharsets.UTF_8);
    Message<OrthodoxValue>[] messages = codec.from(buffer);

    Assert.assertEquals(messages.length, 1);
    Assert.assertEquals(messages[0].getChannel(), "/foo");
    Assert.assertEquals(messages[0].getId(), "1");
  }

  public void testDecodeArrayFromBytes ()
    throws IOException {

    byte[] buffer = "[{\"channel\":\"/a\"},{\"channel\":\"/b\"}]".getBytes(StandardCharsets.UTF_8);
    Message<OrthodoxValue>[] messages = codec.from(buffer);

    Assert.assertEquals(messages.length, 2);
    Assert.assertEquals(messages[0].getChannel(), "/a");
    Assert.assertEquals(messages[1].getChannel(), "/b");
  }

  @Test(expectedExceptions = IOException.class)
  public void testDecodeScalarRootThrows ()
    throws IOException {

    codec.from("42");
  }

  @Test(expectedExceptions = IOException.class)
  public void testDecodeStringRootThrows ()
    throws IOException {

    codec.from("\"hello\"");
  }

  @Test(expectedExceptions = IOException.class)
  public void testDecodeArrayWithNonObjectElementThrows ()
    throws IOException {

    codec.from("[{\"channel\":\"/a\"},5]");
  }

  public void testConvertMapToObjectValue ()
    throws IOException {

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "alice");
    map.put("age", 30);

    Value<OrthodoxValue> value = codec.convert(map);

    Assert.assertEquals(value.getType(), ValueType.OBJECT);

    ObjectValue<OrthodoxValue> object = (ObjectValue<OrthodoxValue>)value;

    Assert.assertEquals(((StringValue<OrthodoxValue>)object.get("name")).asText(), "alice");
    Assert.assertEquals(((NumberValue<OrthodoxValue>)object.get("age")).asInt(), 30);
  }

  public void testConvertListToArrayValue ()
    throws IOException {

    Value<OrthodoxValue> value = codec.convert(List.of(1, 2, 3));

    Assert.assertEquals(value.getType(), ValueType.ARRAY);

    ArrayValue<OrthodoxValue> array = (ArrayValue<OrthodoxValue>)value;

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)array.get(0)).asInt(), 1);
  }

  public void testConvertNestedMap ()
    throws IOException {

    Map<String, Object> inner = new LinkedHashMap<>();
    inner.put("k", "v");

    Map<String, Object> outer = new LinkedHashMap<>();
    outer.put("inner", inner);

    Value<OrthodoxValue> value = codec.convert(outer);
    ObjectValue<OrthodoxValue> object = (ObjectValue<OrthodoxValue>)value;
    ObjectValue<OrthodoxValue> nested = (ObjectValue<OrthodoxValue>)object.get("inner");

    Assert.assertEquals(((StringValue<OrthodoxValue>)nested.get("k")).asText(), "v");
  }

  public void testRoundTripSingleField ()
    throws Exception {

    Message<OrthodoxValue> original = codec.create();
    original.put("channel", "/foo");

    Message<OrthodoxValue>[] parsed = codec.from(original.encode());

    Assert.assertEquals(parsed.length, 1);
    Assert.assertEquals(parsed[0].getChannel(), "/foo");
  }
}
