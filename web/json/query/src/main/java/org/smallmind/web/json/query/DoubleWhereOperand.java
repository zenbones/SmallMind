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
 * A where operand that holds a single double literal value.
 */
@XmlRootElement(name = "double", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class DoubleWhereOperand extends WhereOperand<Double> {

  private Double value;

  /**
   * No-arg constructor for JAXB/Jackson deserialization.
   */
  public DoubleWhereOperand () {

  }

  /**
   * Constructs an operand wrapping the given double.
   *
   * @param value double literal
   */
  public DoubleWhereOperand (Double value) {

    this.value = value;
  }

  /**
   * Factory method that wraps a double in a {@code DoubleWhereOperand}.
   *
   * @param value double literal
   * @return new operand instance
   */
  public static DoubleWhereOperand instance (Double value) {

    return new DoubleWhereOperand(value);
  }

  /**
   * Returns the element type for double operands.
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
   * @return {@link OperandType#DOUBLE}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.DOUBLE;
  }

  /**
   * Returns the stored double value.
   *
   * @return double literal, or {@code null} if not set
   */
  @Override
  @XmlTransient
  public Double get () {

    return value;
  }

  /**
   * Returns the double value used during serialization.
   *
   * @return double literal, or {@code null} if not set
   */
  @XmlElement(name = "value", required = true)
  public Double getValue () {

    return value;
  }

  /**
   * Sets the double value for this operand.
   *
   * @param value double literal
   */
  public void setValue (Double value) {

    this.value = value;
  }
}
