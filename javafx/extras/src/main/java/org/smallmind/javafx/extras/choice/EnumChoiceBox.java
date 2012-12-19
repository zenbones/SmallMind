/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.smallmind.nutsnbolts.util.StringUtilities;

public class EnumChoiceBox<E extends Enum<E>> extends ChoiceBox<E> {

  private final Class<E> enumClass;

  public EnumChoiceBox (Class<E> enumClass) {

    this(enumClass, null, null);
  }

  public EnumChoiceBox (Class<E> enumClass, E selectedEnum) {

    this(enumClass, selectedEnum, null);
  }

  public EnumChoiceBox (Class<E> enumClass, List<E> exemptEnums) {

    this(enumClass, null, exemptEnums);
  }

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

        return StringUtilities.toDisplayCase(enumerated.name(), '_');
      }
    }));

    setConverter(new StringConverter<E>() {

      @Override
      public String toString (E enumerated) {

        return StringUtilities.toDisplayCase(enumerated.name(), '_');
      }

      @Override
      public E fromString (String name) {

        String staticName = StringUtilities.toStaticFieldName(name, '_');

        for (E enumerated : getEnumClass().getEnumConstants()) {
          if (enumerated.name().equals(staticName)) {

            return enumerated;
          }
        }

        return null;
      }
    });

    getItems().addAll(availableEnums);
    setValue((selectedEnum != null) ? selectedEnum : availableEnums.getFirst());
  }

  public Class<E> getEnumClass () {

    return enumClass;
  }
}
