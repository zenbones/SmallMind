/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.web.json.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DirectionalGuide {

  private final HashMap<String, PropertyLexicon> lexiconMap = new HashMap<>();
  private final Direction direction;

  public DirectionalGuide (Direction direction) {

    this.direction = direction;
  }

  private HashMap<String, PropertyLexicon> getLexiconMap () {

    return lexiconMap;
  }

  public void put (String purpose, String fieldName, PropertyInformation propertyInformation)
    throws DtoDefinitionException {

    if (!isJavaNameFragment(purpose)) {
      throw new DtoDefinitionException("The purpose(%s) must be a legal identifier fragment", purpose);
    }

    PropertyLexicon propertyLexicon;

    if ((propertyLexicon = lexiconMap.get(purpose)) == null) {
      lexiconMap.put(purpose, propertyLexicon = new PropertyLexicon());
    }

    if (propertyLexicon.containsKey(fieldName)) {
      throw new DtoDefinitionException("The field(name=%s, purpose=%s, direction=%s) has already been processed", fieldName, (purpose.isEmpty()) ? "n/a" : purpose, direction.name());
    } else {
      propertyLexicon.put(fieldName, propertyInformation);
    }
  }

  public Set<String> keySet () {

    return lexiconMap.keySet();
  }

  public Set<Map.Entry<String, PropertyLexicon>> entrySet () {

    return lexiconMap.entrySet();
  }

  private boolean isJavaNameFragment (String purpose) {

    for (int index = 0; index < purpose.length(); index++) {
      if (!Character.isJavaIdentifierPart(purpose.charAt(index))) {

        return false;
      }
    }

    return true;
  }
}
