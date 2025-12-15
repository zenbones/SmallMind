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
package org.smallmind.nutsnbolts.reflection.type;

import java.util.Arrays;

/**
 * Collects candidate types discovered during generic resolution and enforces a single inference.
 */
public class TypeInference {

  Class[] possibilities;

  /**
   * Adds another possible concrete class for the inferred type.
   *
   * @param clazz the candidate class
   */
  public void addPossibility (Class clazz) {

    if (possibilities == null) {
      possibilities = new Class[] {clazz};
    } else {

      Class[] expandedPossibilities = new Class[possibilities.length + 1];

      System.arraycopy(possibilities, 0, expandedPossibilities, 0, possibilities.length);
      expandedPossibilities[possibilities.length] = clazz;

      possibilities = expandedPossibilities;
    }
  }

  /**
   * Returns the inferred class once collection is complete.
   *
   * @return the inferred class
   * @throws TypeInferenceException if no or multiple possibilities were collected
   */
  public Class getInference () {

    if (possibilities == null) {
      throw new TypeInferenceException("No class inference could be made, please override the appropriate method to statically declare an appropriate type");
    } else if (possibilities.length > 1) {
      throw new TypeInferenceException("Multiple inferences were possible (%s), please override the appropriate method to statically declare the appropriate type", Arrays.toString(possibilities));
    }

    return possibilities[0];
  }
}
