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
 * A where operand that holds a single enum constant, preserving both its class name and constant name.
 *
 * @param <E> the enum type
 */
@XmlRootElement(name = "enum", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class EnumWhereOperand<E extends Enum<E>> extends WhereOperand<E> {

  private EnumHint hint;
  private String value;

  /**
   * No-arg constructor for JAXB/Jackson deserialization.
   */
  public EnumWhereOperand () {

  }

  /**
   * Constructs an operand for the given enum constant.
   *
   * @param enumeration enum constant to wrap
   */
  public EnumWhereOperand (E enumeration) {

    hint = new EnumHint(enumeration.getClass());
    this.value = enumeration.name();
  }

  /**
   * Factory method that wraps an enum constant in an {@code EnumWhereOperand}.
   *
   * @param enumeration enum constant to wrap
   * @param <E>         the enum type
   * @return new operand instance
   */
  public static <E extends Enum<E>> EnumWhereOperand instance (E enumeration) {

    return new EnumWhereOperand<E>(enumeration);
  }

  /**
   * Returns the element type for enum operands, which are treated as strings.
   *
   * @return {@link ElementType#STRING}
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.STRING;
  }

  /**
   * Returns the operand type discriminator for this class.
   *
   * @return {@link OperandType#ENUM}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.ENUM;
  }

  /**
   * Resolves and returns the enum constant by loading the class from the hint and looking up the stored name.
   *
   * @return resolved enum constant
   * @throws QueryProcessingException if the enum class named in the hint cannot be loaded
   */
  @Override
  @XmlTransient
  public E get () {

    try {

      return Enum.valueOf((Class<E>)Class.forName(hint.getType()), value);
    } catch (ClassNotFoundException classNotFoundException) {
      throw new QueryProcessingException(classNotFoundException);
    }
  }

  /**
   * Returns the hint holding the enum class name.
   *
   * @return enum hint
   */
  @XmlElement(name = "hint", required = true)
  public EnumHint getHint () {

    return hint;
  }

  /**
   * Sets the hint holding the enum class name.
   *
   * @param hint enum hint
   */
  public void setHint (EnumHint hint) {

    this.hint = hint;
  }

  /**
   * Returns the name of the enum constant.
   *
   * @return enum constant name
   */
  @XmlElement(name = "value", required = true)
  public String getValue () {

    return value;
  }

  /**
   * Sets the name of the enum constant.
   *
   * @param value enum constant name
   */
  public void setValue (String value) {

    this.value = value;
  }
}
