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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Fills the remaining {@link ArrayWhereOperand} component-type branches not exercised by
 * {@link ArrayWhereOperandTest} (byte, short, float, character, enum arrays), plus the empty-value,
 * hint/value accessor, and element-type derivation paths.
 */
@Test(groups = "unit")
public class ArrayWhereOperandBranchTest {

  private enum Suit {HEARTS, SPADES}

  public void testByteArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Byte[] {(byte)1, (byte)2});

    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.get(), new Byte[] {(byte)1, (byte)2});
  }

  public void testShortArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Short[] {(short)5, (short)6});

    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.get(), new Short[] {(short)5, (short)6});
  }

  public void testFloatArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Float[] {1.5F, 2.5F});

    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.get(), new Float[] {1.5F, 2.5F});
  }

  public void testCharacterArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Character[] {'a', 'b'});

    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertEquals(operand.get(), new Character[] {'a', 'b'});
  }

  public void testEnumArrayRoundTrip () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Suit[] {Suit.HEARTS, Suit.SPADES});

    Assert.assertEquals(operand.getOperandType(), OperandType.ARRAY);
    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertEquals(operand.get(), new Object[] {Suit.HEARTS, Suit.SPADES});
  }

  public void testNullValueProducesNull () {

    Assert.assertNull(new ArrayWhereOperand().get());
  }

  public void testHintAndValueAccessors () {

    ArrayWhereOperand operand = ArrayWhereOperand.instance(new Integer[] {1, 2, 3});

    Assert.assertNotNull(operand.getHint());
    Assert.assertNotNull(operand.getValue());
    Assert.assertEquals(operand.getValue().size(), 3);

    ComponentHint replacement = new ComponentHint(ComponentType.INTEGER);
    operand.setHint(replacement);
    Assert.assertSame(operand.getHint(), replacement);
  }

  public void testComponentHintElementTypesForAllNumberKinds () {

    Assert.assertEquals(new ArrayWhereOperand(new ComponentHint(ComponentType.DATE), null).getElementType(), ElementType.DATE);
    Assert.assertEquals(new ArrayWhereOperand(new ComponentHint(ComponentType.STRING), null).getElementType(), ElementType.STRING);
    Assert.assertEquals(new ArrayWhereOperand(new ComponentHint(ComponentType.CHARACTER), null).getElementType(), ElementType.STRING);
    Assert.assertEquals(new ArrayWhereOperand(new ComponentHint(ComponentType.BOOLEAN), null).getElementType(), ElementType.BOOLEAN);
    Assert.assertEquals(new ArrayWhereOperand(new ComponentHint(ComponentType.LONG), null).getElementType(), ElementType.NUMBER);
    Assert.assertEquals(new ArrayWhereOperand(new ComponentHint(ComponentType.SHORT), null).getElementType(), ElementType.NUMBER);
    Assert.assertEquals(new ArrayWhereOperand(new ComponentHint(ComponentType.BYTE), null).getElementType(), ElementType.NUMBER);
  }
}
