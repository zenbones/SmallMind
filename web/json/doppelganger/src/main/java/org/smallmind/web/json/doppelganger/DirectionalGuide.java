/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import java.util.Map;
import java.util.Set;

/**
 * Holds the properties to include in generated views for a particular {@link Direction}.
 * Each purpose maps to a {@link PropertyLexicon} containing the per-field metadata.
 */
public class DirectionalGuide {

  private final HashMap<String, PropertyLexicon> lexiconMap = new HashMap<>();
  private final Direction direction;

  /**
   * Creates a guide for the supplied direction.
   *
   * @param direction whether the guide tracks inbound or outbound properties
   */
  public DirectionalGuide (Direction direction) {

    this.direction = direction;
  }

  /**
   * Registers a property for the given purpose and field name.
   *
   * @param purpose             the idiom/purpose bucket
   * @param fieldName           the source field name
   * @param propertyInformation metadata describing the property
   * @throws DefinitionException if the purpose is not a legal Java identifier fragment or a duplicate entry is detected
   */
  public void put (String purpose, String fieldName, PropertyInformation propertyInformation)
    throws DefinitionException {

    if (!isJavaNameFragment(purpose)) {
      throw new DefinitionException("The purpose(%s) must be a legal identifier fragment", purpose);
    }

    PropertyLexicon propertyLexicon;

    if ((propertyLexicon = lexiconMap.get(purpose)) == null) {
      lexiconMap.put(purpose, propertyLexicon = new PropertyLexicon());
    }

    if (propertyLexicon.containsKey(fieldName)) {
      throw new DefinitionException("The field(name=%s, purpose=%s, direction=%s) has already been processed", fieldName, (purpose.isEmpty()) ? "n/a" : purpose, direction.name());
    } else {
      propertyLexicon.put(fieldName, propertyInformation);
    }
  }

  /**
   * Returns all entries keyed by purpose for iteration.
   *
   * @return map entries of purpose to lexicon
   */
  public Set<Map.Entry<String, PropertyLexicon>> lexiconEntrySet () {

    return lexiconMap.entrySet();
  }

  /**
   * Validates that the supplied purpose contains only Java identifier parts.
   *
   * @param purpose the purpose text to validate
   * @return {@code true} when valid; {@code false} otherwise
   */
  private boolean isJavaNameFragment (String purpose) {

    for (int index = 0; index < purpose.length(); index++) {
      if (!Character.isJavaIdentifierPart(purpose.charAt(index))) {

        return false;
      }
    }

    return true;
  }
}
