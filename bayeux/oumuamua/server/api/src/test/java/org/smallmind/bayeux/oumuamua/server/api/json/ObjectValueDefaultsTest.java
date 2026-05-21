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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Pins each default {@code put(field, primitive)} overload on {@link ObjectValue} to
 * delegate through the factory and store a value of the expected type.
 */
@Test(groups = "unit")
public class ObjectValueDefaultsTest {

  private TestValueFactory factory;
  private ObjectValue<TestValueFactory.TestValue> object;

  @BeforeMethod
  public void beforeMethod () {

    factory = new TestValueFactory();
    object = factory.objectValue();
  }

  public void testPutBooleanWrapsViaFactory () {

    object.put("b", true);

    Value<TestValueFactory.TestValue> stored = object.get("b");

    Assert.assertEquals(stored.getType(), ValueType.BOOLEAN);
    Assert.assertTrue(((BooleanValue<TestValueFactory.TestValue>)stored).asBoolean());
  }

  public void testPutIntWrapsViaFactory () {

    object.put("i", 42);

    Value<TestValueFactory.TestValue> stored = object.get("i");

    Assert.assertEquals(stored.getType(), ValueType.NUMBER);
    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)stored).asInt(), 42);
  }

  public void testPutLongWrapsViaFactory () {

    object.put("l", 1234567890123L);

    NumberValue<TestValueFactory.TestValue> stored = (NumberValue<TestValueFactory.TestValue>)object.get("l");

    Assert.assertEquals(stored.asLong(), 1234567890123L);
  }

  public void testPutDoubleWrapsViaFactory () {

    object.put("d", 3.14d);

    NumberValue<TestValueFactory.TestValue> stored = (NumberValue<TestValueFactory.TestValue>)object.get("d");

    Assert.assertEquals(stored.asDouble(), 3.14d);
  }

  public void testPutStringWrapsViaFactory () {

    object.put("s", "hello");

    StringValue<TestValueFactory.TestValue> stored = (StringValue<TestValueFactory.TestValue>)object.get("s");

    Assert.assertEquals(stored.asText(), "hello");
  }

  public void testPutNullStringStoresNullValue () {

    object.put("s", (String)null);

    Value<TestValueFactory.TestValue> stored = object.get("s");

    Assert.assertEquals(stored.getType(), ValueType.NULL);
  }

  public void testPutReturnsObjectForChaining () {

    Assert.assertSame(object.put("a", 1).put("b", 2), object);
  }

  public void testPutOverwritesExistingValue () {

    object.put("k", 1);
    object.put("k", "two");

    Assert.assertEquals(((StringValue<TestValueFactory.TestValue>)object.get("k")).asText(), "two");
  }
}
