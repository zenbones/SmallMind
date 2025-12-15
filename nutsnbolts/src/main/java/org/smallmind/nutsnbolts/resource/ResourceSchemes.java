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
 * Immutable wrapper around a set of schemes supported by a {@link ResourceGenerator}.
 */
public class ResourceSchemes {

  private final String[] schemes;

  /**
   * Creates a wrapper for the supplied scheme names.
   *
   * @param schemes array of supported scheme identifiers
   */
  public ResourceSchemes (String[] schemes) {

    this.schemes = schemes;
  }

  /**
   * Checks whether this collection contains the provided scheme.
   *
   * @param scheme scheme name to look for
   * @return {@code true} if the scheme is supported; otherwise {@code false}
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
   * Computes a hash code based on the underlying scheme array.
   *
   * @return hash code for use in maps/sets
   */
  public int hashCode () {

    return Arrays.hashCode(schemes);
  }

  /**
   * Compares the underlying scheme array for equality.
   *
   * @param obj object to compare
   * @return {@code true} if the other object holds the same scheme array; otherwise {@code false}
   */
  public boolean equals (Object obj) {

    return (obj instanceof String[]) && Arrays.equals(schemes, (String[])obj);
  }
}
