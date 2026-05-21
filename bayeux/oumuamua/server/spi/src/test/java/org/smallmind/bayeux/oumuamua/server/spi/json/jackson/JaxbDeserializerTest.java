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
package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies the JSON-to-{@link OrthodoxValue} decoding contract of {@link JaxbDeserializer}: both
 * byte and string entry points, top-level object and array shapes, every value-tree node type
 * walked by {@code walk}, the {@link Codec#convert(Object)} round-trip path, and the failure modes
 * triggered by unsupported root types or non-object array elements.
 */
@Test(groups = "unit")
public class JaxbDeserializerTest {

  private Codec<OrthodoxValue> codec;

  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
  }

  public void testReadByteArrayWithSingleObjectReturnsOneMessage ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"channel\":\"/meta/handshake\"}".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(messages.length, 1);
    Assert.assertEquals(((StringValue<OrthodoxValue>)messages[0].get("channel")).asText(), "/meta/handshake");
  }

  public void testReadStringWithSingleObjectReturnsOneMessage ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"channel\":\"/meta/handshake\"}");

    Assert.assertEquals(messages.length, 1);
    Assert.assertEquals(((StringValue<OrthodoxValue>)messages[0].get("channel")).asText(), "/meta/handshake");
  }

  public void testReadArrayWithMultipleObjectsReturnsMessageArray ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("[{\"id\":\"1\"},{\"id\":\"2\"},{\"id\":\"3\"}]");

    Assert.assertEquals(messages.length, 3);
    Assert.assertEquals(((StringValue<OrthodoxValue>)messages[0].get("id")).asText(), "1");
    Assert.assertEquals(((StringValue<OrthodoxValue>)messages[1].get("id")).asText(), "2");
    Assert.assertEquals(((StringValue<OrthodoxValue>)messages[2].get("id")).asText(), "3");
  }

  public void testReadEmptyArrayReturnsZeroMessages ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("[]");

    Assert.assertEquals(messages.length, 0);
  }

  @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*object or array.*")
  public void testReadTopLevelStringThrowsIoException ()
    throws IOException {

    codec.from("\"just a string\"");
  }

  @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*object or array.*")
  public void testReadTopLevelNumberThrowsIoException ()
    throws IOException {

    codec.from("42");
  }

  @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*messages must represent json objects.*")
  public void testReadArrayWithNonObjectElementThrowsIoException ()
    throws IOException {

    codec.from("[{\"id\":\"1\"},\"not an object\"]");
  }

  public void testWalkRecursesIntoNestedObjects ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"data\":{\"inner\":{\"deepest\":\"reached\"}}}");

    ObjectValue<OrthodoxValue> data = (ObjectValue<OrthodoxValue>)messages[0].get("data");
    ObjectValue<OrthodoxValue> inner = (ObjectValue<OrthodoxValue>)data.get("inner");
    StringValue<OrthodoxValue> deepest = (StringValue<OrthodoxValue>)inner.get("deepest");

    Assert.assertEquals(deepest.asText(), "reached");
  }

  public void testWalkRecursesIntoArrays ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"items\":[\"a\",\"b\",\"c\"]}");

    ArrayValue<OrthodoxValue> items = (ArrayValue<OrthodoxValue>)messages[0].get("items");

    Assert.assertEquals(items.size(), 3);
    Assert.assertEquals(((StringValue<OrthodoxValue>)items.get(0)).asText(), "a");
    Assert.assertEquals(((StringValue<OrthodoxValue>)items.get(1)).asText(), "b");
    Assert.assertEquals(((StringValue<OrthodoxValue>)items.get(2)).asText(), "c");
  }

  public void testWalkHandlesIntegerNumber ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"n\":42}");

    NumberValue<OrthodoxValue> number = (NumberValue<OrthodoxValue>)messages[0].get("n");

    Assert.assertEquals(number.asInt(), 42);
  }

  public void testWalkHandlesLongNumber ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"n\":9999999999}");

    NumberValue<OrthodoxValue> number = (NumberValue<OrthodoxValue>)messages[0].get("n");

    Assert.assertEquals(number.asLong(), 9_999_999_999L);
  }

  public void testWalkHandlesDoubleNumber ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"n\":3.14}");

    NumberValue<OrthodoxValue> number = (NumberValue<OrthodoxValue>)messages[0].get("n");

    Assert.assertEquals(number.asDouble(), 3.14, 0.0001);
  }

  public void testWalkHandlesBooleanValues ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"yes\":true,\"no\":false}");

    Assert.assertEquals(messages[0].get("yes").getType(), ValueType.BOOLEAN);
    Assert.assertEquals(messages[0].get("no").getType(), ValueType.BOOLEAN);
  }

  public void testWalkHandlesNullValue ()
    throws IOException {

    Message<OrthodoxValue>[] messages = codec.from("{\"n\":null}");

    Assert.assertEquals(messages[0].get("n").getType(), ValueType.NULL);
  }

  public void testConvertObjectProducesValueTree ()
    throws IOException {

    Map<String, Object> source = new HashMap<>();

    source.put("greeting", "hello");
    source.put("count", 7);

    Value<OrthodoxValue> converted = codec.convert(source);

    Assert.assertEquals(converted.getType(), ValueType.OBJECT);

    ObjectValue<OrthodoxValue> object = (ObjectValue<OrthodoxValue>)converted;

    Assert.assertEquals(((StringValue<OrthodoxValue>)object.get("greeting")).asText(), "hello");
    Assert.assertEquals(((NumberValue<OrthodoxValue>)object.get("count")).asInt(), 7);
  }

  @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*Unknown number type.*")
  public void testWalkUnsupportedNumberTypeThrows ()
    throws IOException {

    codec.from("{\"n\":99999999999999999999999}");
  }
}
