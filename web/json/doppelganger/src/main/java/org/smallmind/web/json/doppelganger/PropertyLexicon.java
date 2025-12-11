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
package org.smallmind.web.json.doppelganger;

import java.util.HashMap;

/**
 * Holds the set of properties belonging to a particular purpose/direction, split into real and virtual fields.
 */
public class PropertyLexicon {

  private final HashMap<String, PropertyInformation> realMap = new HashMap<>();
  private final HashMap<String, PropertyInformation> virtualMap = new HashMap<>();

  /**
   * Adds a property keyed by its logical name, routing to the real or virtual map.
   *
   * @param key                 logical property name
   * @param propertyInformation property metadata
   */
  public void put (String key, PropertyInformation propertyInformation) {

    if (propertyInformation.isVirtual()) {

      virtualMap.put(key, propertyInformation);
    } else {

      realMap.put(key, propertyInformation);
    }
  }

  /**
   * @return {@code true} if at least one real property exists
   */
  public boolean isReal () {

    return !realMap.isEmpty();
  }

  /**
   * @return {@code true} if at least one virtual property exists
   */
  public boolean isVirtual () {

    return !virtualMap.isEmpty();
  }

  /**
   * Checks whether any property carries comment text.
   *
   * @return {@code true} if comments are present
   */
  public boolean hasComment () {

    if (!realMap.isEmpty()) {
      for (PropertyInformation propertyInformation : realMap.values()) {
        if (!propertyInformation.getComment().isEmpty()) {

          return true;
        }
      }
    }
    if (!virtualMap.isEmpty()) {
      for (PropertyInformation propertyInformation : virtualMap.values()) {
        if (!propertyInformation.getComment().isEmpty()) {

          return true;
        }
      }
    }

    return false;
  }

  /**
   * Checks whether any property uses an {@code @As} override.
   *
   * @return {@code true} if such overrides are present
   */
  public boolean hasAs () {

    if (!realMap.isEmpty()) {
      for (PropertyInformation propertyInformation : realMap.values()) {
        if (propertyInformation.getAs() != null) {

          return true;
        }
      }
    }
    if (!virtualMap.isEmpty()) {
      for (PropertyInformation propertyInformation : virtualMap.values()) {
        if (propertyInformation.getAs() != null) {

          return true;
        }
      }
    }

    return false;
  }

  /**
   * Checks whether any real property has a nullifier message.
   *
   * @return {@code true} if a nullifier exists
   */
  public boolean hasNullifier () {

    if (!realMap.isEmpty()) {
      for (PropertyInformation propertyInformation : realMap.values()) {
        if (propertyInformation.getNullifierMessage() != null) {

          return true;
        }
      }
    }

    return false;
  }

  /**
   * Determines whether the lexicon already contains the supplied key in either map.
   *
   * @param key property key to test
   * @return {@code true} if present
   */
  public boolean containsKey (String key) {

    return realMap.containsKey(key) || virtualMap.containsKey(key);
  }

  /**
   * @return map of real properties keyed by name
   */
  public HashMap<String, PropertyInformation> getRealMap () {

    return realMap;
  }

  /**
   * @return map of virtual properties keyed by name
   */
  public HashMap<String, PropertyInformation> getVirtualMap () {

    return virtualMap;
  }
}
