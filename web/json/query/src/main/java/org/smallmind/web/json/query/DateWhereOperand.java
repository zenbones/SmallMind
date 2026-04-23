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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.nutsnbolts.json.LocalDateTimeXmlAdapter;

/**
 * A where operand that holds a single {@link LocalDateTime} literal value.
 */
@XmlRootElement(name = "date", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class DateWhereOperand extends WhereOperand<LocalDateTime> {

  private LocalDateTime value;

  /**
   * No-arg constructor for JAXB/Jackson deserialization.
   */
  public DateWhereOperand () {

  }

  /**
   * Constructs an operand wrapping the given date-time.
   *
   * @param value date-time literal
   */
  public DateWhereOperand (LocalDateTime value) {

    this.value = value;
  }

  /**
   * Factory method that wraps a date-time in a {@code DateWhereOperand}.
   *
   * @param value date-time literal
   * @return new operand instance
   */
  public static DateWhereOperand instance (LocalDateTime value) {

    return new DateWhereOperand(value);
  }

  /**
   * Returns the element type for date operands.
   *
   * @return {@link ElementType#DATE}
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.DATE;
  }

  /**
   * Returns the operand type discriminator for this class.
   *
   * @return {@link OperandType#DATE}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.DATE;
  }

  /**
   * Returns the stored date-time value.
   *
   * @return date-time literal, or {@code null} if not set
   */
  @Override
  @XmlTransient
  public LocalDateTime get () {

    return value;
  }

  /**
   * Returns the date-time value used during serialization.
   *
   * @return date-time literal, or {@code null} if not set
   */
  @XmlElement(name = "value", required = true)
  @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
  public LocalDateTime getValue () {

    return value;
  }

  /**
   * Sets the date-time value for this operand.
   *
   * @param value date-time literal
   */
  public void setValue (LocalDateTime value) {

    this.value = value;
  }
}
