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
package org.smallmind.nutsnbolts.json;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.util.EnumUtility;

/**
 * Abstract JAXB adapter base class that converts enum constants to and from their string names, with name normalization on unmarshal.
 *
 * @param <E> the concrete enum type handled by this adapter
 */
public abstract class EnumXmlAdapter<E extends Enum<E>> extends XmlAdapter<String, E> {

  private final Class<E> enumClass;

  /**
   * Reflectively resolves the concrete enum type from the generic superclass signature for use during unmarshalling.
   */
  public EnumXmlAdapter () {

    enumClass = (Class<E>)GenericUtility.getTypeArgumentsOfSubclass(EnumXmlAdapter.class, this.getClass()).get(0);
  }

  /**
   * Converts a string to the corresponding enum constant, normalizing the value via {@link EnumUtility#toEnumName(String)} before lookup.
   *
   * @param value the string representation of the enum constant
   * @return the matching enum constant, or {@code null} when {@code value} is {@code null}
   */
  @Override
  public E unmarshal (String value) {

    return (value == null) ? null : Enum.valueOf(enumClass, EnumUtility.toEnumName(value));
  }

  /**
   * Converts an enum constant to its string representation via {@link Enum#toString()}.
   *
   * @param enumeration the enum constant to marshal
   * @return the string representation, or {@code null} when {@code enumeration} is {@code null}
   */
  @Override
  public String marshal (E enumeration) {

    return (enumeration == null) ? null : enumeration.toString();
  }
}
