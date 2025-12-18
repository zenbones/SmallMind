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
 * Short literal operand.
 */
@XmlRootElement(name = "short", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class ShortWhereOperand extends WhereOperand<Short> {

  private Short value;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public ShortWhereOperand () {

  }

  /**
   * Creates an operand with the provided short value.
   *
   * @param value short literal
   */
  public ShortWhereOperand (Short value) {

    this.value = value;
  }

  /**
   * Convenience factory for a short operand.
   *
   * @param value short literal
   * @return operand instance
   */
  public static ShortWhereOperand instance (Short value) {

    return new ShortWhereOperand(value);
  }

  /**
   * @return {@link ElementType#NUMBER}
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.NUMBER;
  }

  /**
   * @return {@link OperandType#SHORT}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.SHORT;
  }

  /**
   * Returns the short value.
   *
   * @return short literal or {@code null}
   */
  @Override
  @XmlTransient
  public Short get () {

    return value;
  }

  /**
   * Returns the serialized short value.
   *
   * @return short literal or {@code null}
   */
  @XmlElement(name = "value", required = true)
  public Short getValue () {

    return value;
  }

  /**
   * Sets the short value.
   *
   * @param value short literal
   */
  public void setValue (Short value) {

    this.value = value;
  }
}
