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
 * Exercises the no-arg constructor plus {@code getValue}/{@code setValue} serialization accessors of every
 * concrete scalar {@link WhereOperand} subtype, mirroring the field round trip a JAXB/Jackson provider
 * performs. (Full polymorphic JSON serialization is not exercised here because the bytecode-generating
 * polymorphic adapter dependency is not on this module's test classpath; only the per-property accessors
 * touched during binding are covered.)
 */
@Test(groups = "unit")
public class WhereOperandRoundTripTest {

  private enum Color {RED, GREEN, BLUE}

  public void testBooleanValueAccessors () {

    BooleanWhereOperand operand = new BooleanWhereOperand();
    operand.setValue(Boolean.TRUE);

    Assert.assertEquals(operand.getOperandType(), OperandType.BOOLEAN);
    Assert.assertEquals(operand.getElementType(), ElementType.BOOLEAN);
    Assert.assertEquals(operand.getValue(), Boolean.TRUE);
    Assert.assertEquals(operand.get(), Boolean.TRUE);
  }

  public void testByteValueAccessors () {

    ByteWhereOperand operand = new ByteWhereOperand();
    operand.setValue((byte)7);

    Assert.assertEquals(operand.getOperandType(), OperandType.BYTE);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.getValue(), (byte)7);
    Assert.assertEquals(operand.get(), (byte)7);
  }

  public void testCharacterValueAccessors () {

    CharacterWhereOperand operand = new CharacterWhereOperand();
    operand.setValue('q');

    Assert.assertEquals(operand.getOperandType(), OperandType.CHARACTER);
    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertEquals(operand.getValue(), (Character)'q');
    Assert.assertEquals(operand.get(), (Character)'q');
  }

  public void testDateValueAccessors () {

    LocalDateTime when = LocalDateTime.of(2022, 3, 4, 5, 6, 7);
    DateWhereOperand operand = new DateWhereOperand();
    operand.setValue(when);

    Assert.assertEquals(operand.getOperandType(), OperandType.DATE);
    Assert.assertEquals(operand.getElementType(), ElementType.DATE);
    Assert.assertEquals(operand.getValue(), when);
    Assert.assertEquals(operand.get(), when);
  }

  public void testDoubleValueAccessors () {

    DoubleWhereOperand operand = new DoubleWhereOperand();
    operand.setValue(2.75D);

    Assert.assertEquals(operand.getOperandType(), OperandType.DOUBLE);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.getValue(), 2.75D);
    Assert.assertEquals(operand.get(), 2.75D);
  }

  public void testFloatValueAccessors () {

    FloatWhereOperand operand = new FloatWhereOperand();
    operand.setValue(1.5F);

    Assert.assertEquals(operand.getOperandType(), OperandType.FLOAT);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.getValue(), 1.5F);
    Assert.assertEquals(operand.get(), 1.5F);
  }

  public void testIntegerValueAccessors () {

    IntegerWhereOperand operand = new IntegerWhereOperand();
    operand.setValue(99);

    Assert.assertEquals(operand.getOperandType(), OperandType.INTEGER);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.getValue(), 99);
    Assert.assertEquals(operand.get(), 99);
  }

  public void testLongValueAccessors () {

    LongWhereOperand operand = new LongWhereOperand();
    operand.setValue(9000000000L);

    Assert.assertEquals(operand.getOperandType(), OperandType.LONG);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.getValue(), 9000000000L);
    Assert.assertEquals(operand.get(), 9000000000L);
  }

  public void testShortValueAccessors () {

    ShortWhereOperand operand = new ShortWhereOperand();
    operand.setValue((short)33);

    Assert.assertEquals(operand.getOperandType(), OperandType.SHORT);
    Assert.assertEquals(operand.getElementType(), ElementType.NUMBER);
    Assert.assertEquals(operand.getValue(), (short)33);
    Assert.assertEquals(operand.get(), (short)33);
  }

  public void testStringValueAccessors () {

    StringWhereOperand operand = new StringWhereOperand();
    operand.setValue("hello");

    Assert.assertEquals(operand.getOperandType(), OperandType.STRING);
    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertEquals(operand.getValue(), "hello");
    Assert.assertEquals(operand.get(), "hello");
  }

  public void testNullOperandAccessors () {

    NullWhereOperand operand = new NullWhereOperand();

    Assert.assertEquals(operand.getOperandType(), OperandType.NULL);
    Assert.assertEquals(operand.getElementType(), ElementType.NULL);
    Assert.assertNull(operand.get());
    Assert.assertSame(NullWhereOperand.instance(), NullWhereOperand.instance());
  }

  public void testEnumValueAccessors () {

    EnumHint hint = new EnumHint(Color.class);
    EnumWhereOperand<Color> operand = new EnumWhereOperand<>();
    operand.setHint(hint);
    operand.setValue("GREEN");

    Assert.assertEquals(operand.getOperandType(), OperandType.ENUM);
    Assert.assertEquals(operand.getElementType(), ElementType.STRING);
    Assert.assertSame(operand.getHint(), hint);
    Assert.assertEquals(operand.getValue(), "GREEN");
    Assert.assertEquals(operand.get(), Color.GREEN);
  }

  @Test(expectedExceptions = QueryProcessingException.class)
  public void testEnumOperandUnknownClassRejected () {

    EnumWhereOperand<Color> operand = new EnumWhereOperand<>();
    operand.setHint(new EnumHintStub());
    operand.setValue("GREEN");

    operand.get();
  }

  private static final class EnumHintStub extends EnumHint {

    @Override
    public String getType () {

      return "org.smallmind.does.not.Exist";
    }
  }
}
