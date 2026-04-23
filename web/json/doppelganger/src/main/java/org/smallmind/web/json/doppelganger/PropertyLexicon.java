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
 * Holds the set of properties for a single purpose/direction, split into real (entity-backed) and virtual maps.
 */
public class PropertyLexicon {

  private final HashMap<String, PropertyInformation> realMap = new HashMap<>();
  private final HashMap<String, PropertyInformation> virtualMap = new HashMap<>();

  /**
   * Adds a property entry, routing it to the real map or the virtual map based on its {@link PropertyInformation#isVirtual()} flag.
   *
   * @param key                 the logical property name
   * @param propertyInformation the metadata for the property
   */
  public void put (String key, PropertyInformation propertyInformation) {

    if (propertyInformation.isVirtual()) {

      virtualMap.put(key, propertyInformation);
    } else {

      realMap.put(key, propertyInformation);
    }
  }

  /**
   * @return {@code true} if at least one real property has been registered
   */
  public boolean isReal () {

    return !realMap.isEmpty();
  }

  /**
   * @return {@code true} if at least one virtual property has been registered
   */
  public boolean isVirtual () {

    return !virtualMap.isEmpty();
  }

  /**
   * Returns whether any property in either map carries a non-empty comment.
   *
   * @return {@code true} if at least one property has comment text
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
   * Returns whether any property in either map has a non-null {@code @As} type override.
   *
   * @return {@code true} if at least one property specifies an {@code @As} override
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
   * Returns whether any real property has a non-null nullifier message.
   *
   * @return {@code true} if at least one real property specifies a nullifier message
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
   * Returns whether a property with the given key exists in either the real or virtual map.
   *
   * @param key the property name to test
   * @return {@code true} if the key is present in either map
   */
  public boolean containsKey (String key) {

    return realMap.containsKey(key) || virtualMap.containsKey(key);
  }

  /**
   * @return the map of real (entity-backed) properties keyed by logical name
   */
  public HashMap<String, PropertyInformation> getRealMap () {

    return realMap;
  }

  /**
   * @return the map of virtual properties keyed by logical name
   */
  public HashMap<String, PropertyInformation> getVirtualMap () {

    return virtualMap;
  }
}
