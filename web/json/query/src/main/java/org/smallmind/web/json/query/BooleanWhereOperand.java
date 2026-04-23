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
 * A where operand that holds a single boolean literal value.
 */
@XmlRootElement(name = "boolean", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class BooleanWhereOperand extends WhereOperand<Boolean> {

  private Boolean value;

  /**
   * No-arg constructor for JAXB/Jackson deserialization.
   */
  public BooleanWhereOperand () {

  }

  /**
   * Constructs an operand wrapping the given boolean.
   *
   * @param value boolean literal
   */
  public BooleanWhereOperand (Boolean value) {

    this.value = value;
  }

  /**
   * Factory method that wraps a boolean in a {@code BooleanWhereOperand}.
   *
   * @param value boolean literal
   * @return new operand instance
   */
  public static BooleanWhereOperand instance (Boolean value) {

    return new BooleanWhereOperand(value);
  }

  /**
   * Returns the element type for boolean operands.
   *
   * @return {@link ElementType#BOOLEAN}
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.BOOLEAN;
  }

  /**
   * Returns the operand type discriminator for this class.
   *
   * @return {@link OperandType#BOOLEAN}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.BOOLEAN;
  }

  /**
   * Returns the stored boolean value.
   *
   * @return boolean literal, or {@code null} if not set
   */
  @Override
  @XmlTransient
  public Boolean get () {

    return value;
  }

  /**
   * Returns the boolean value used during serialization.
   *
   * @return boolean literal, or {@code null} if not set
   */
  @XmlElement(name = "value", required = true)
  public Boolean getValue () {

    return value;
  }

  /**
   * Sets the boolean value for this operand.
   *
   * @param value boolean literal
   */
  public void setValue (Boolean value) {

    this.value = value;
  }
}
