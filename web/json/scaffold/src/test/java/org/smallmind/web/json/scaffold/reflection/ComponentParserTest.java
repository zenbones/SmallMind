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
package org.smallmind.web.json.scaffold.reflection;

import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives the {@link ComponentParser} state machine across names, dotted chains, method argument lists,
 * array subscripts, whitespace handling, and the malformed-expression error conditions.
 */
@Test(groups = "unit")
public class ComponentParserTest {

  public void testNullChainProducesEmptyArray ()
    throws BeanAccessException {

    Assert.assertEquals(ComponentParser.parse(null).length, 0);
  }

  public void testSingleName ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("name");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "name");
    Assert.assertNull(components[0].getArguments());
    Assert.assertNull(components[0].getSubscripts());
  }

  public void testDottedChain ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("billingAddress.city");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[0].getName(), "billingAddress");
    Assert.assertEquals(components[1].getName(), "city");
  }

  public void testMethodWithArguments ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("lookup(42, \"abc\").value");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[0].getName(), "lookup");
    Assert.assertEquals(components[0].getArguments().length, 2);
    Assert.assertEquals(components[1].getName(), "value");
  }

  public void testSubscripts ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("items[2].quantity");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[0].getName(), "items");
    Assert.assertEquals(components[0].getSubscripts(), new int[] {2});
    Assert.assertEquals(components[1].getName(), "quantity");
  }

  public void testConsecutiveSubscripts ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("grid[1][3]");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "grid");
    Assert.assertEquals(components[0].getSubscripts(), new int[] {1, 3});
  }

  public void testWhitespaceTolerated ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("first . second");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[0].getName(), "first");
    Assert.assertEquals(components[1].getName(), "second");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testEmptyChainRejected ()
    throws BeanAccessException {

    ComponentParser.parse("");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testIllegalIdentifierStartRejected ()
    throws BeanAccessException {

    ComponentParser.parse("9name");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testUnterminatedParametersRejected ()
    throws BeanAccessException {

    ComponentParser.parse("lookup(42");
  }

  public void testEscapedQuoteInStringArgument ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("greet(\"a\\\"b\")");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "greet");
    Assert.assertEquals(components[0].getArguments().length, 1);
    Assert.assertEquals(components[0].getArguments()[0], "a\"b");
  }

  public void testWhitespaceBetweenNameAndParameters ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("lookup (\"x\")");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "lookup");
    Assert.assertEquals(components[0].getArguments().length, 1);
  }

  public void testWhitespaceBetweenNameAndSubscript ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("items [2]");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "items");
    Assert.assertEquals(components[0].getSubscripts(), new int[] {2});
  }
}
