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
 * A {@link StringConverter} for enum types that converts constants to human-readable display-case
 * strings (underscores replaced with spaces, title-cased) and parses display-case strings back to
 * enum constants by delegating to {@link Enum#valueOf}.
 *
 * @param <E> the enum type managed by this converter
 */
public class EnumStringConverter<E extends Enum<E>> extends StringConverter<E> {

  private final Class<E> enumClass;

  /**
   * Creates a converter for the given enum class.
   *
   * @param enumClass the enum type to convert; must not be {@code null}
   */
  public EnumStringConverter (Class<E> enumClass) {

    this.enumClass = enumClass;
  }

  /**
   * Converts an enum constant to a display-case string by transforming the constant's name
   * (e.g. {@code SOME_VALUE} becomes {@code Some Value}).
   *
   * @param item the enum constant to convert; must not be {@code null}
   * @return the display-case string representation
   */
  @Override
  public String toString (E item) {

    return StringUtility.toDisplayCase(item.name(), '_');
  }

  /**
   * Parses a string back to an enum constant. The string is expected to match an enum constant
   * name exactly (case-sensitive) as understood by {@link Enum#valueOf}.
   *
   * @param name the string to parse; must not be {@code null}
   * @return the matching enum constant
   * @throws IllegalArgumentException if {@code name} does not correspond to any constant in the enum
   */
  @Override
  public E fromString (String name) {

    return Enum.valueOf(enumClass, name);
  }
}
