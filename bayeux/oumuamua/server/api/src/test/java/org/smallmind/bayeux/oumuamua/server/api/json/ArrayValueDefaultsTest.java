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
 * Pins each default {@code add} / {@code set} / {@code insert} primitive overload on
 * {@link ArrayValue} to wrap its primitive argument via the factory and delegate to the
 * canonical generic overload.
 */
@Test(groups = "unit")
public class ArrayValueDefaultsTest {

  private TestValueFactory factory;
  private ArrayValue<TestValueFactory.TestValue> array;

  @BeforeMethod
  public void beforeMethod () {

    factory = new TestValueFactory();
    array = factory.arrayValue();
  }

  public void testAddBoolean () {

    array.add(true);

    Assert.assertEquals(array.get(0).getType(), ValueType.BOOLEAN);
    Assert.assertTrue(((BooleanValue<TestValueFactory.TestValue>)array.get(0)).asBoolean());
  }

  public void testAddInt () {

    array.add(7);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asInt(), 7);
  }

  public void testAddLong () {

    array.add(123L);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asLong(), 123L);
  }

  public void testAddDouble () {

    array.add(1.5d);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asDouble(), 1.5d);
  }

  public void testAddString () {

    array.add("xyz");

    Assert.assertEquals(((StringValue<TestValueFactory.TestValue>)array.get(0)).asText(), "xyz");
  }

  public void testSetBooleanReplacesAtIndex () {

    array.add("placeholder");
    array.set(0, true);

    Assert.assertEquals(array.size(), 1);
    Assert.assertTrue(((BooleanValue<TestValueFactory.TestValue>)array.get(0)).asBoolean());
  }

  public void testSetIntReplacesAtIndex () {

    array.add("placeholder");
    array.set(0, 99);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asInt(), 99);
  }

  public void testSetLongReplacesAtIndex () {

    array.add("placeholder");
    array.set(0, 9999L);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asLong(), 9999L);
  }

  public void testSetDoubleReplacesAtIndex () {

    array.add("placeholder");
    array.set(0, 2.5d);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asDouble(), 2.5d);
  }

  public void testSetStringReplacesAtIndex () {

    array.add("placeholder");
    array.set(0, "replaced");

    Assert.assertEquals(((StringValue<TestValueFactory.TestValue>)array.get(0)).asText(), "replaced");
  }

  public void testInsertBooleanShiftsLater () {

    array.add("after");
    array.insert(0, true);

    Assert.assertEquals(array.size(), 2);
    Assert.assertTrue(((BooleanValue<TestValueFactory.TestValue>)array.get(0)).asBoolean());
    Assert.assertEquals(((StringValue<TestValueFactory.TestValue>)array.get(1)).asText(), "after");
  }

  public void testInsertIntShiftsLater () {

    array.add("after");
    array.insert(0, 5);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asInt(), 5);
  }

  public void testInsertLongShiftsLater () {

    array.add("after");
    array.insert(0, 12345L);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asLong(), 12345L);
  }

  public void testInsertDoubleShiftsLater () {

    array.add("after");
    array.insert(0, 0.25d);

    Assert.assertEquals(((NumberValue<TestValueFactory.TestValue>)array.get(0)).asDouble(), 0.25d);
  }

  public void testInsertStringShiftsLater () {

    array.add("after");
    array.insert(0, "before");

    Assert.assertEquals(((StringValue<TestValueFactory.TestValue>)array.get(0)).asText(), "before");
    Assert.assertEquals(((StringValue<TestValueFactory.TestValue>)array.get(1)).asText(), "after");
  }

  public void testAddReturnsArrayForChaining () {

    Assert.assertSame(array.add(1).add(2), array);
  }
}
