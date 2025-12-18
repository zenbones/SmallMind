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
 * Enum literal operand that stores the enum class name and constant value.
 *
 * @param <E> enum type
 */
@XmlRootElement(name = "enum", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class EnumWhereOperand<E extends Enum<E>> extends WhereOperand<E> {

  private EnumHint hint;
  private String value;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public EnumWhereOperand () {

  }

  /**
   * Creates an operand for the given enum constant.
   *
   * @param enumeration enum constant
   */
  public EnumWhereOperand (E enumeration) {

    hint = new EnumHint(enumeration.getClass());
    this.value = enumeration.name();
  }

  /**
   * Convenience factory for an enum operand.
   *
   * @param enumeration enum constant
   * @return operand instance
   */
  public static <E extends Enum<E>> EnumWhereOperand instance (E enumeration) {

    return new EnumWhereOperand<E>(enumeration);
  }

  /**
   * Enums are represented as strings (their names).
   *
   * @return {@link ElementType#STRING}
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.STRING;
  }

  /**
   * @return {@link OperandType#ENUM}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.ENUM;
  }

  /**
   * Resolves the enum constant using the stored class name and value.
   *
   * @return enum constant
   * @throws QueryProcessingException if the enum class cannot be loaded
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
   * Returns the hint containing the enum class name.
   *
   * @return enum hint
   */
  @XmlElement(name = "hint", required = true)
  public EnumHint getHint () {

    return hint;
  }

  /**
   * Sets the hint containing the enum class name.
   *
   * @param hint enum hint
   */
  public void setHint (EnumHint hint) {

    this.hint = hint;
  }

  /**
   * Returns the enum constant name.
   *
   * @return enum name
   */
  @XmlElement(name = "value", required = true)
  public String getValue () {

    return value;
  }

  /**
   * Sets the enum constant name.
   *
   * @param value enum name
   */
  public void setValue (String value) {

    this.value = value;
  }
}
