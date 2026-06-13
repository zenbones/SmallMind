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
package org.smallmind.web.json.scaffold.fault;

import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Covers {@link FaultInformation}: the code/template accessors, the {@code Object...} constructor that
 * serializes arguments to a JSON array, the typed {@code getArgumentsAs}/{@code getArgumentAs} converters,
 * and their out-of-range and mismatched-class error paths.
 */
@Test(groups = "unit")
public class FaultInformationTest {

  public void testNoArgConstructorAndAccessors () {

    FaultInformation information = new FaultInformation();

    information.setCode(7);
    information.setTemplate("hello %s");

    ArrayNode arguments = JsonNodeFactory.instance.arrayNode();
    arguments.add("world");
    information.setArguments(arguments);

    Assert.assertEquals(information.getCode(), 7);
    Assert.assertEquals(information.getTemplate(), "hello %s");
    Assert.assertEquals(information.getArguments(), arguments);
  }

  public void testArgumentConstructorSerializesArguments () {

    FaultInformation information = new FaultInformation(42, "between %d and %s", 5, "ten");

    Assert.assertEquals(information.getCode(), 42);
    Assert.assertEquals(information.getTemplate(), "between %d and %s");
    Assert.assertEquals(information.getArguments().size(), 2);
  }

  public void testGetArgumentsAsConvertsEachPosition () {

    FaultInformation information = new FaultInformation(1, "t", 5, "ten");

    Object[] converted = information.getArgumentsAs(new Class<?>[] {Integer.class, String.class});

    Assert.assertEquals(converted[0], 5);
    Assert.assertEquals(converted[1], "ten");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetArgumentsAsRejectsClassCountMismatch () {

    new FaultInformation(1, "t", 5, "ten").getArgumentsAs(new Class<?>[] {Integer.class});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetArgumentsAsRejectsNullClasses () {

    new FaultInformation(1, "t", 5).getArgumentsAs(null);
  }

  public void testGetArgumentAsConvertsPosition () {

    FaultInformation information = new FaultInformation(1, "t", 5, "ten");

    Assert.assertEquals(information.getArgumentAs(0, Integer.class), Integer.valueOf(5));
    Assert.assertEquals(information.getArgumentAs(1, String.class), "ten");
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testGetArgumentAsRejectsNegativeIndex () {

    new FaultInformation(1, "t", 5).getArgumentAs(-1, Integer.class);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testGetArgumentAsRejectsTooLargeIndex () {

    new FaultInformation(1, "t", 5).getArgumentAs(9, Integer.class);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testGetArgumentAsRejectsNullArguments () {

    new FaultInformation().getArgumentAs(0, Integer.class);
  }
}
