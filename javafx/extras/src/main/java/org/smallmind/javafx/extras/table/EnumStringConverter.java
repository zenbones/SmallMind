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
package org.smallmind.javafx.extras.table;

import javafx.util.StringConverter;
import org.smallmind.nutsnbolts.util.StringUtility;

/**
 * {@link StringConverter} that renders enum constants using display case while parsing using the enum name.
 *
 * @param <E> the enum type
 */
public class EnumStringConverter<E extends Enum<E>> extends StringConverter<E> {

  private final Class<E> enumClass;

  /**
   * @param enumClass the enum type this converter supports
   */
  public EnumStringConverter (Class<E> enumClass) {

    this.enumClass = enumClass;
  }

  /**
   * Converts an enum constant to a display-friendly string.
   *
   * @param item the enum value
   * @return display-cased name
   */
  @Override
  public String toString (E item) {

    return StringUtility.toDisplayCase(item.name(), '_');
  }

  /**
   * Parses the provided string to an enum constant using {@link Enum#valueOf(Class, String)}.
   *
   * @param name the string representation
   * @return matching enum constant
   * @throws IllegalArgumentException if the string does not match an enum constant
   */
  @Override
  public E fromString (String name) {

    return Enum.valueOf(enumClass, name);
  }
}
