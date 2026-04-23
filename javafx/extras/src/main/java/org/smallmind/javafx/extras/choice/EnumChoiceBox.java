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
package org.smallmind.javafx.extras.choice;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.nutsnbolts.util.AlphaNumericConverter;
import org.smallmind.nutsnbolts.util.StringUtility;

/**
 * A {@link ChoiceBox} pre-populated with the constants of an {@link Enum} type. Available constants
 * are sorted alphabetically by their display-case names. Constants may be excluded at construction
 * time and an initial selection may be provided. When no initial selection is specified the first
 * remaining entry is selected automatically.
 *
 * @param <E> the enum type displayed by this choice box
 */
public class EnumChoiceBox<E extends Enum<E>> extends ChoiceBox<E> {

  private final Class<E> enumClass;

  /**
   * Constructs the choice box showing all constants of {@code enumClass}, sorted alphabetically,
   * with the first entry selected.
   *
   * @param enumClass the enum type to display; must not be {@code null}
   */
  public EnumChoiceBox (Class<E> enumClass) {

    this(enumClass, null, null);
  }

  /**
   * Constructs the choice box showing all constants of {@code enumClass} and pre-selects
   * {@code selectedEnum}.
   *
   * @param enumClass    the enum type to display; must not be {@code null}
   * @param selectedEnum the constant to select initially, or {@code null} to select the first entry
   */
  public EnumChoiceBox (Class<E> enumClass, E selectedEnum) {

    this(enumClass, selectedEnum, null);
  }

  /**
   * Constructs the choice box omitting the constants in {@code exemptEnums} and selecting the first
   * remaining entry.
   *
   * @param enumClass   the enum type to display; must not be {@code null}
   * @param exemptEnums constants that must not appear in the list, or {@code null} to include all
   */
  public EnumChoiceBox (Class<E> enumClass, List<E> exemptEnums) {

    this(enumClass, null, exemptEnums);
  }

  /**
   * Constructs the choice box, optionally excluding constants and specifying an initial selection.
   * The remaining constants are sorted alphabetically by display-case name.
   *
   * @param enumClass    the enum type to display; must not be {@code null}
   * @param selectedEnum the constant to select initially, or {@code null} to select the first remaining entry
   * @param exemptEnums  constants to remove from the available options, or {@code null} to include all
   */
  public EnumChoiceBox (Class<E> enumClass, E selectedEnum, List<E> exemptEnums) {

    LinkedList<E> availableEnums = new LinkedList<E>(Arrays.asList(enumClass.getEnumConstants()));

    this.enumClass = enumClass;

    if (exemptEnums != null) {
      for (E exemptEnum : exemptEnums) {
        availableEnums.remove(exemptEnum);
      }
    }

    Collections.sort(availableEnums, new AlphaNumericComparator<E>(new AlphaNumericConverter<E>() {

      @Override
      public String toString (E enumerated) {

        return StringUtility.toDisplayCase(enumerated.name(), '_');
      }
    }));

    setConverter(new StringConverter<E>() {

      @Override
      public String toString (E enumerated) {

        return StringUtility.toDisplayCase(enumerated.name(), '_');
      }

      @Override
      public E fromString (String name) {

        return Enum.valueOf(getEnumClass(), StringUtility.toStaticFieldName(name, '_'));
      }
    });

    getItems().addAll(availableEnums);
    setValue((selectedEnum != null) ? selectedEnum : availableEnums.getFirst());
  }

  /**
   * Returns the enum class whose constants are displayed by this choice box.
   *
   * @return the enum type; never {@code null}
   */
  public Class<E> getEnumClass () {

    return enumClass;
  }
}
