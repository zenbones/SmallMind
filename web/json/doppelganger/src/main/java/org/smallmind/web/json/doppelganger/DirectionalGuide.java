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
import java.util.Map;
import java.util.Set;

/**
 * Organizes the properties to include in generated views for a single {@link Direction},
 * grouped by purpose into {@link PropertyLexicon} instances.
 */
public class DirectionalGuide {

  private final HashMap<String, PropertyLexicon> lexiconMap = new HashMap<>();
  private final Direction direction;

  /**
   * Constructs a guide for the given direction.
   *
   * @param direction the direction (IN or OUT) this guide covers
   */
  public DirectionalGuide (Direction direction) {

    this.direction = direction;
  }

  /**
   * Registers a property under the given purpose, raising an error on duplicate entries or invalid purpose names.
   *
   * @param purpose             the idiom purpose bucket (must contain only Java identifier characters)
   * @param fieldName           the logical field name of the property
   * @param propertyInformation metadata describing the property
   * @throws DefinitionException if the purpose contains illegal characters or the field has already been registered
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
   * Returns the set of all purpose-to-lexicon entries for iteration.
   *
   * @return entry set mapping purpose strings to their property lexicons
   */
  public Set<Map.Entry<String, PropertyLexicon>> lexiconEntrySet () {

    return lexiconMap.entrySet();
  }

  /**
   * Returns whether every character in the purpose string is a valid Java identifier part.
   *
   * @param purpose the purpose string to validate
   * @return {@code true} if every character passes {@link Character#isJavaIdentifierPart(char)}
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
