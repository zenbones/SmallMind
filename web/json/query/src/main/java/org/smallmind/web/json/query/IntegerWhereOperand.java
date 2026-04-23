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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A where operand that holds a single integer literal value.
 */
@XmlRootElement(name = "integer", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class IntegerWhereOperand extends WhereOperand<Integer> {

  private Integer value;

  /**
   * No-arg constructor for JAXB/Jackson deserialization.
   */
  public IntegerWhereOperand () {

  }

  /**
   * Constructs an operand wrapping the given integer.
   *
   * @param value integer literal
   */
  public IntegerWhereOperand (Integer value) {

    this.value = value;
  }

  /**
   * Factory method that wraps an integer in an {@code IntegerWhereOperand}.
   *
   * @param value integer literal
   * @return new operand instance
   */
  public static IntegerWhereOperand instance (Integer value) {

    return new IntegerWhereOperand(value);
  }

  /**
   * Returns the element type for integer operands.
   *
   * @return {@link ElementType#NUMBER}
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.NUMBER;
  }

  /**
   * Returns the operand type discriminator for this class.
   *
   * @return {@link OperandType#INTEGER}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.INTEGER;
  }

  /**
   * Returns the stored integer value.
   *
   * @return integer literal, or {@code null} if not set
   */
  @Override
  @XmlTransient
  public Integer get () {

    return value;
  }

  /**
   * Returns the integer value used during serialization.
   *
   * @return integer literal, or {@code null} if not set
   */
  @XmlElement(name = "value", required = true)
  public Integer getValue () {

    return value;
  }

  /**
   * Sets the integer value for this operand.
   *
   * @param value integer literal
   */
  public void setValue (Integer value) {

    this.value = value;
  }
}
