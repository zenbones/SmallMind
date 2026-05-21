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

import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NullValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberType;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OrthodoxValueFactoryTest {

  private OrthodoxValueFactory factory;

  @BeforeClass
  public void beforeClass () {

    factory = new OrthodoxValueFactory();
  }

  public void testObjectValue () {

    ObjectValue<OrthodoxValue> value = factory.objectValue();

    Assert.assertNotNull(value);
    Assert.assertEquals(value.getType(), ValueType.OBJECT);
    Assert.assertTrue(value.isEmpty());
    Assert.assertEquals(value.size(), 0);
    Assert.assertSame(value.getFactory(), factory);
  }

  public void testArrayValue () {

    ArrayValue<OrthodoxValue> value = factory.arrayValue();

    Assert.assertNotNull(value);
    Assert.assertEquals(value.getType(), ValueType.ARRAY);
    Assert.assertTrue(value.isEmpty());
    Assert.assertEquals(value.size(), 0);
    Assert.assertSame(value.getFactory(), factory);
  }

  public void testTextValue () {

    StringValue<OrthodoxValue> value = factory.textValue("hello");

    Assert.assertEquals(value.getType(), ValueType.STRING);
    Assert.assertEquals(value.asText(), "hello");
  }

  public void testIntegerValue () {

    NumberValue<OrthodoxValue> value = factory.numberValue(42);

    Assert.assertEquals(value.getType(), ValueType.NUMBER);
    Assert.assertEquals(value.getNumberType(), NumberType.INTEGER);
    Assert.assertEquals(value.asInt(), 42);
    Assert.assertEquals(value.asLong(), 42L);
    Assert.assertEquals(value.asDouble(), 42.0d);
    Assert.assertEquals(value.asNumber().intValue(), 42);
  }

  public void testLongValue () {

    NumberValue<OrthodoxValue> value = factory.numberValue(9_000_000_000L);

    Assert.assertEquals(value.getNumberType(), NumberType.LONG);
    Assert.assertEquals(value.asLong(), 9_000_000_000L);
    Assert.assertEquals(value.asDouble(), 9_000_000_000.0d);
    Assert.assertEquals(value.asNumber().longValue(), 9_000_000_000L);
  }

  public void testDoubleValue () {

    NumberValue<OrthodoxValue> value = factory.numberValue(3.5d);

    Assert.assertEquals(value.getNumberType(), NumberType.DOUBLE);
    Assert.assertEquals(value.asDouble(), 3.5d);
    Assert.assertEquals(value.asInt(), 3);
    Assert.assertEquals(value.asLong(), 3L);
  }

  public void testBooleanValue () {

    BooleanValue<OrthodoxValue> trueValue = factory.booleanValue(true);
    BooleanValue<OrthodoxValue> falseValue = factory.booleanValue(false);

    Assert.assertEquals(trueValue.getType(), ValueType.BOOLEAN);
    Assert.assertTrue(trueValue.asBoolean());
    Assert.assertFalse(falseValue.asBoolean());
  }

  public void testNullValue () {

    NullValue<OrthodoxValue> value = factory.nullValue();

    Assert.assertEquals(value.getType(), ValueType.NULL);
  }
}
