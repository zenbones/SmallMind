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
package org.smallmind.nutsnbolts.spring;

import java.util.HashMap;
import java.util.Map;
import org.smallmind.nutsnbolts.property.PropertyClosure;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.property.PropertyExpanderException;
import org.smallmind.nutsnbolts.util.Option;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SpringPropertyAccessorTest {

  private static SpringPropertyAccessor buildAccessor (Map<String, Object> propertyMap)
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, false);
    PropertyPlaceholderStringValueResolver resolver = new PropertyPlaceholderStringValueResolver(expander, propertyMap);

    return new SpringPropertyAccessor(resolver);
  }

  public void testKeySetExposesAllPropertyKeys ()
    throws PropertyExpanderException {

    Map<String, Object> properties = new HashMap<>();
    properties.put("a", "1");
    properties.put("b", "2");

    SpringPropertyAccessor accessor = buildAccessor(properties);

    Assert.assertEquals(accessor.getKeySet().size(), 2);
    Assert.assertTrue(accessor.getKeySet().contains("a"));
  }

  public void testAsStringReturnsResolvedValue ()
    throws PropertyExpanderException {

    Map<String, Object> properties = new HashMap<>();
    properties.put("host", "example.com");

    SpringPropertyAccessor accessor = buildAccessor(properties);

    Assert.assertEquals(accessor.asString("host"), "example.com");
  }

  public void testAsStringReturnsNullForMissingKey ()
    throws PropertyExpanderException {

    SpringPropertyAccessor accessor = buildAccessor(new HashMap<>());

    Assert.assertNull(accessor.asString("missing"));
  }

  public void testAsBooleanParsesTrueValue ()
    throws PropertyExpanderException {

    Map<String, Object> properties = new HashMap<>();
    properties.put("flag", "true");

    SpringPropertyAccessor accessor = buildAccessor(properties);
    Option<Boolean> option = accessor.asBoolean("flag");

    Assert.assertFalse(option.isNone());
    Assert.assertTrue(option.get());
  }

  public void testAsBooleanReturnsNoneForMissingKey ()
    throws PropertyExpanderException {

    SpringPropertyAccessor accessor = buildAccessor(new HashMap<>());

    Assert.assertTrue(accessor.asBoolean("missing").isNone());
  }

  public void testAsLongParsesValue ()
    throws PropertyExpanderException {

    Map<String, Object> properties = new HashMap<>();
    properties.put("count", "1234567890");

    SpringPropertyAccessor accessor = buildAccessor(properties);
    Option<Long> option = accessor.asLong("count");

    Assert.assertFalse(option.isNone());
    Assert.assertEquals(option.get().longValue(), 1234567890L);
  }

  public void testAsLongReturnsNoneForMissingKey ()
    throws PropertyExpanderException {

    SpringPropertyAccessor accessor = buildAccessor(new HashMap<>());

    Assert.assertTrue(accessor.asLong("missing").isNone());
  }

  @Test(expectedExceptions = RuntimeBeansException.class)
  public void testAsLongThrowsWhenValueNotNumeric ()
    throws PropertyExpanderException {

    Map<String, Object> properties = new HashMap<>();
    properties.put("count", "not-a-number");

    SpringPropertyAccessor accessor = buildAccessor(properties);

    accessor.asLong("count");
  }

  public void testAsIntParsesValue ()
    throws PropertyExpanderException {

    Map<String, Object> properties = new HashMap<>();
    properties.put("port", "8080");

    SpringPropertyAccessor accessor = buildAccessor(properties);
    Option<Integer> option = accessor.asInt("port");

    Assert.assertFalse(option.isNone());
    Assert.assertEquals(option.get().intValue(), 8080);
  }

  public void testAsIntReturnsNoneForMissingKey ()
    throws PropertyExpanderException {

    SpringPropertyAccessor accessor = buildAccessor(new HashMap<>());

    Assert.assertTrue(accessor.asInt("missing").isNone());
  }

  @Test(expectedExceptions = RuntimeBeansException.class)
  public void testAsIntThrowsWhenValueNotNumeric ()
    throws PropertyExpanderException {

    Map<String, Object> properties = new HashMap<>();
    properties.put("port", "abc");

    SpringPropertyAccessor accessor = buildAccessor(properties);

    accessor.asInt("port");
  }
}
