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
 * {@link ChoiceBox} pre-populated with the constants of an {@link Enum} type. Values are displayed using a
 * human-friendly converter and can optionally omit certain enum constants or select a default value.
 *
 * @param <E> the enum type displayed by this choice box
 */
public class EnumChoiceBox<E extends Enum<E>> extends ChoiceBox<E> {

  private final Class<E> enumClass;

  /**
   * Constructs the choice box showing all enum constants and selects the first value.
   *
   * @param enumClass the enum type to display
   */
  public EnumChoiceBox (Class<E> enumClass) {

    this(enumClass, null, null);
  }

  /**
   * Constructs the choice box showing all enum constants and selects the provided value.
   *
   * @param enumClass    the enum type to display
   * @param selectedEnum the value to select initially, or {@code null} to select the first entry
   */
  public EnumChoiceBox (Class<E> enumClass, E selectedEnum) {

    this(enumClass, selectedEnum, null);
  }

  /**
   * Constructs the choice box excluding the provided enum constants and selecting the first remaining value.
   *
   * @param enumClass   the enum type to display
   * @param exemptEnums constants that should not appear in the list
   */
  public EnumChoiceBox (Class<E> enumClass, List<E> exemptEnums) {

    this(enumClass, null, exemptEnums);
  }

  /**
   * Constructs the choice box, allowing both a default value and a list of excluded constants.
   *
   * @param enumClass    the enum type to display
   * @param selectedEnum the value to select initially, or {@code null} to select the first available
   * @param exemptEnums  constants that should be removed from the available options
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
   * @return the enum class rendered by this choice box
   */
  public Class<E> getEnumClass () {

    return enumClass;
  }
}
