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
import java.util.HashSet;
import java.util.Map;
import javax.lang.model.element.TypeElement;

public class ClassTracker {

  private final HashMap<TypeElement, HashMap<String, Visibility>> trackingMap = new HashMap<>();
  private final HashSet<TypeElement> processedSet = new HashSet<>();

  public void add (TypeElement classElement) {

    processedSet.add(classElement);
  }

  public boolean contains (TypeElement classElement) {

    return processedSet.contains(classElement);
  }

  public void update (TypeElement classElement, GeneratorInformation generatorInformation) {

    HashMap<String, Visibility> purposeMap;

    trackingMap.put(classElement, purposeMap = new HashMap<>());

    for (String pledgedPurpose : generatorInformation.pledgedPurposes(Direction.IN)) {
      purposeMap.put(pledgedPurpose, Visibility.IN);
    }
    for (Map.Entry<String, PropertyLexicon> purposeEntry : generatorInformation.getInDirectionalGuide().entrySet()) {
      if (purposeEntry.getValue().isReal() || purposeEntry.getValue().isVirtual()) {
        purposeMap.put(purposeEntry.getKey(), Visibility.IN);
      }
    }
    for (String pledgedPurpose : generatorInformation.pledgedPurposes(Direction.OUT)) {

      Visibility visibility;

      if ((visibility = purposeMap.get(pledgedPurpose)) == null) {
        purposeMap.put(pledgedPurpose, Visibility.OUT);
      } else if (Visibility.IN.equals(visibility)) {
        purposeMap.put(pledgedPurpose, Visibility.BOTH);
      }
    }
    for (Map.Entry<String, PropertyLexicon> purposeEntry : generatorInformation.getOutDirectionalGuide().entrySet()) {
      if (purposeEntry.getValue().isReal() || purposeEntry.getValue().isVirtual()) {

        Visibility visibility;

        if ((visibility = purposeMap.get(purposeEntry.getKey())) == null) {
          purposeMap.put(purposeEntry.getKey(), Visibility.OUT);
        } else if (Visibility.IN.equals(visibility)) {
          purposeMap.put(purposeEntry.getKey(), Visibility.BOTH);
        }
      }
    }
  }

  public HashMap<String, Visibility> getPurposesMap (TypeElement classElement) {

    return trackingMap.get(classElement);
  }

  public boolean hasNoPurpose (TypeElement classElement) {

    HashMap<String, Visibility> purposeMap;

    return ((purposeMap = trackingMap.get(classElement)) == null) || purposeMap.isEmpty();
  }

  public Visibility getVisibility (TypeElement classElement, String purpose) {

    HashMap<String, Visibility> purposeMap;

    return ((purposeMap = trackingMap.get(classElement)) == null) ? null : purposeMap.get(purpose);
  }
}
