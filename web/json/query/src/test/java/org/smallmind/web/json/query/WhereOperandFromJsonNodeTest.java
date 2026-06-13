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
package org.smallmind.web.json.query;

import org.smallmind.web.json.scaffold.util.JsonCodec;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

/**
 * Covers {@link WhereOperand#fromJsonNode}, the server-side factory that maps a parsed JSON literal to
 * the matching concrete operand subtype, including the array and error branches.
 */
@Test(groups = "unit")
public class WhereOperandFromJsonNodeTest {

  private WhereOperand<?> from (String json) {

    return WhereOperand.fromJsonNode(JsonCodec.readAsJsonNode(json));
  }

  public void testBoolean () {

    WhereOperand<?> operand = from("true");

    Assert.assertEquals(operand.getOperandType(), OperandType.BOOLEAN);
    Assert.assertEquals(operand.get(), Boolean.TRUE);
  }

  public void testInteger () {

    WhereOperand<?> operand = from("42");

    Assert.assertEquals(operand.getOperandType(), OperandType.INTEGER);
    Assert.assertEquals(operand.get(), 42);
  }

  public void testLong () {

    WhereOperand<?> operand = from("5000000000");

    Assert.assertEquals(operand.getOperandType(), OperandType.LONG);
    Assert.assertEquals(operand.get(), 5000000000L);
  }

  public void testDouble () {

    WhereOperand<?> operand = from("3.5");

    Assert.assertEquals(operand.getOperandType(), OperandType.DOUBLE);
    Assert.assertEquals(operand.get(), 3.5D);
  }

  public void testString () {

    WhereOperand<?> operand = from("\"hello\"");

    Assert.assertEquals(operand.getOperandType(), OperandType.STRING);
    Assert.assertEquals(operand.get(), "hello");
  }

  public void testNull () {

    WhereOperand<?> operand = from("null");

    Assert.assertEquals(operand.getOperandType(), OperandType.NULL);
    Assert.assertNull(operand.get());
  }

  public void testBooleanArray () {

    WhereOperand<?> operand = from("[true,false,true]");

    Assert.assertEquals(operand.getOperandType(), OperandType.ARRAY);
    Assert.assertEquals(operand.getElementType(), ElementType.BOOLEAN);
    Assert.assertEquals(((Object[])operand.get()).length, 3);
  }

  public void testStringArray () {

    WhereOperand<?> operand = from("[\"a\",\"b\"]");

    Assert.assertEquals(operand.getOperandType(), OperandType.ARRAY);
    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertEquals(operand.get(), new Object[] {"a", "b"});
  }

  public void testIntegerArray () {

    WhereOperand<?> operand = from("[1,2,3]");

    Assert.assertEquals(operand.getOperandType(), OperandType.ARRAY);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(((Object[])operand.get()).length, 3);
  }

  public void testDoubleArray () {

    WhereOperand<?> operand = from("[1.5,2.5]");

    Assert.assertEquals(operand.getOperandType(), OperandType.ARRAY);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(((Object[])operand.get()).length, 2);
  }

  public void testNullNodeBecomesNullOperand () {

    Assert.assertEquals(WhereOperand.fromJsonNode(null).getOperandType(), OperandType.NULL);
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testEmptyArrayRejected () {

    from("[]");
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testUnsupportedNumberArrayTypeRejected () {

    // A value too large for long parses as BigInteger, which hits the number-array default branch.
    from("[99999999999999999999999999]");
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testObjectNodeRejected () {

    from("{\"x\":1}");
  }
}
