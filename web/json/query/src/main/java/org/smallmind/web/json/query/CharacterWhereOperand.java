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
 * Character literal operand.
 */
@XmlRootElement(name = "character", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class CharacterWhereOperand extends WhereOperand<Character> {

  private Character value;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public CharacterWhereOperand () {

  }

  /**
   * Creates an operand with the provided character value.
   *
   * @param value character literal
   */
  public CharacterWhereOperand (Character value) {

    this.value = value;
  }

  /**
   * Convenience factory for a character operand.
   *
   * @param value character literal
   * @return the operand instance
   */
  public static CharacterWhereOperand instance (Character value) {

    return new CharacterWhereOperand(value);
  }

  /**
   * @return {@link ElementType#STRING}
   */
  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.STRING;
  }

  /**
   * @return {@link OperandType#CHARACTER}
   */
  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.CHARACTER;
  }

  /**
   * Returns the character value.
   *
   * @return character literal or {@code null}
   */
  @Override
  @XmlTransient
  public Character get () {

    return value;
  }

  /**
   * Returns the serialized character value.
   *
   * @return character literal or {@code null}
   */
  @XmlElement(name = "value", required = true)
  public Character getValue () {

    return value;
  }

  /**
   * Sets the character value.
   *
   * @param value character literal
   */
  public void setValue (Character value) {

    this.value = value;
  }
}
