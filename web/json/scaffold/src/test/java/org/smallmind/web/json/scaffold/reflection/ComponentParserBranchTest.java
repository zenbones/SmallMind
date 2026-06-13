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
 * Fills the remaining {@link ComponentParser} state-machine branches: subscripts and dotted chaining
 * after method-argument lists, chaining after a subscript, whitespace handling in the various
 * post-component states, and the illegal-character / illegal-subscript / missing-termination error
 * transitions not exercised by {@link ComponentParserTest}.
 */
@Test(groups = "unit")
public class ComponentParserBranchTest {

  public void testSubscriptAfterParameters ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("lookup(1)[0]");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "lookup");
    Assert.assertEquals(components[0].getArguments().length, 1);
    Assert.assertEquals(components[0].getSubscripts(), new int[] {0});
  }

  public void testDottedChainAfterParameters ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("lookup(1).value");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[1].getName(), "value");
  }

  public void testDottedChainAfterSubscript ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("items[0].name");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[0].getSubscripts(), new int[] {0});
    Assert.assertEquals(components[1].getName(), "name");
  }

  public void testWhitespaceAfterParameters ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("lookup(1) .value");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[1].getName(), "value");
  }

  public void testWhitespaceAfterSubscript ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("items[0] .name");

    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[1].getName(), "name");
  }

  public void testTrailingWhitespaceAfterName ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("name ");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "name");
  }

  public void testEmptyArgumentList ()
    throws BeanAccessException {

    PathComponent[] components = ComponentParser.parse("ping()");

    Assert.assertEquals(components.length, 1);
    Assert.assertEquals(components[0].getName(), "ping");
    Assert.assertEquals(components[0].getArguments().length, 0);
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testIllegalCharacterInNameRejected ()
    throws BeanAccessException {

    ComponentParser.parse("na#me");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testIllegalCharacterAfterNameWhitespaceRejected ()
    throws BeanAccessException {

    ComponentParser.parse("name #");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testIllegalCharacterAfterParametersRejected ()
    throws BeanAccessException {

    ComponentParser.parse("lookup(1)#");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testIllegalCharacterAfterSubscriptRejected ()
    throws BeanAccessException {

    ComponentParser.parse("items[0]#");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testNonNumericSubscriptRejected ()
    throws BeanAccessException {

    ComponentParser.parse("items[abc]");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testTrailingDotRejected ()
    throws BeanAccessException {

    ComponentParser.parse("name.");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testUnterminatedSubscriptRejected ()
    throws BeanAccessException {

    ComponentParser.parse("items[0");
  }
}
