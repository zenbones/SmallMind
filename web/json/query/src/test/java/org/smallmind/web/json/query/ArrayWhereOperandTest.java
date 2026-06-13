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

import java.time.LocalDateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the {@link ArrayWhereOperand} component-type round trips (Java array → JSON hint/array →
 * typed Java array) and the {@link EnumWhereOperand} class/name resolution.
 */
@Test(groups = "unit")
public class ArrayWhereOperandTest {

  private enum Color {RED, GREEN, BLUE}

  public void testIntegerArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Integer[] {1, 2, 3});

    Assert.assertEquals(operand.getOperandType(), OperandType.ARRAY);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.get(), new Integer[] {1, 2, 3});
  }

  public void testLongArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Long[] {10L, 20L});

    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.get(), new Long[] {10L, 20L});
  }

  public void testDoubleArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Double[] {1.5D, 2.5D});

    Assert.assertEquals(operand.get(), new Double[] {1.5D, 2.5D});
  }

  public void testStringArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new String[] {"a", "b"});

    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertEquals(operand.get(), new String[] {"a", "b"});
  }

  public void testBooleanArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Boolean[] {true, false});

    Assert.assertEquals(operand.getElementType(), ElementType.BOOLEAN);
    Assert.assertEquals(operand.get(), new Boolean[] {true, false});
  }

  public void testDateArrayRoundTrip () {

    LocalDateTime first = LocalDateTime.of(2020, 1, 1, 12, 30);
    LocalDateTime second = LocalDateTime.of(2021, 6, 15, 8, 0);

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new LocalDateTime[] {first, second});

    Assert.assertEquals(operand.getElementType(), ElementType.DATE);
    Assert.assertEquals(operand.get(), new LocalDateTime[] {first, second});
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testUnknownComponentTypeRejected () {

    ArrayWhereOperand.instance(new Object[] {new Object()});
  }

  public void testEnumOperandRoundTrip () {

    EnumWhereOperand<Color> operand = EnumWhereOperand.instance(Color.GREEN);

    Assert.assertEquals(operand.getOperandType(), OperandType.ENUM);
    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertEquals(operand.get(), Color.GREEN);
  }
}
