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
package org.smallmind.nutsnbolts.resource;

import java.util.Arrays;

/**
 * Immutable value object wrapping an array of scheme names that a {@link ResourceGenerator}
 * is capable of handling, used as a map key in {@link ResourceParser}.
 */
public class ResourceSchemes {

  private final String[] schemes;

  /**
   * Constructs a {@code ResourceSchemes} instance holding the given scheme names.
   *
   * @param schemes array of scheme identifiers supported by a generator
   */
  public ResourceSchemes (String[] schemes) {

    this.schemes = schemes;
  }

  /**
   * Tests whether the given scheme name is present in this collection.
   *
   * @param scheme the scheme name to search for
   * @return {@code true} if this collection contains {@code scheme}; {@code false} otherwise
   */
  public boolean containsScheme (String scheme) {

    for (String matchingScheme : schemes) {
      if (matchingScheme.equals(scheme)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns a hash code derived from the underlying scheme array for use in maps and sets.
   *
   * @return hash code based on array contents
   */
  public int hashCode () {

    return Arrays.hashCode(schemes);
  }

  /**
   * Compares this instance to another object for equality based on the scheme array contents.
   *
   * @param obj the object to compare against this instance
   * @return {@code true} if {@code obj} is a {@code String[]} with the same scheme names in the same order; {@code false} otherwise
   */
  public boolean equals (Object obj) {

    return (obj instanceof String[]) && Arrays.equals(schemes, (String[])obj);
  }
}
