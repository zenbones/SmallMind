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
import java.util.HashSet;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public class VisibilityTracker {

  private final HashMap<TypeElement, HashMap<String, Visibility>> trackingMap = new HashMap<>();
  private final HashMap<TypeElement, HashMap<String, Visibility>> forswornMap = new HashMap<>();

  public void add (TypeElement classElement, TypeElement superclassElement) {

    HashMap<String, Visibility> purposeMap;

    if ((purposeMap = trackingMap.get(superclassElement)) != null) {
      for (Map.Entry<String, Visibility> purposeEntry : purposeMap.entrySet()) {
        add(classElement, purposeEntry.getKey(), purposeEntry.getValue(), false);
      }
    }
  }

  public void add (TypeElement classElement, String purpose, Visibility visibility, PropertyLexicon propertyLexicon) {

    if (propertyLexicon.isReal() || propertyLexicon.isVirtual()) {
      add(classElement, purpose, visibility, false);
    }
  }

  public void add (TypeElement classElement, String purpose, Visibility visibility, boolean pledged) {

    HashMap<String, Visibility> purposeMap;

    if ((purposeMap = trackingMap.get(classElement)) == null) {
      trackingMap.put(classElement, purposeMap = new HashMap<>());
    }
    purposeMap.put(purpose, visibility.compose(purposeMap.get(purpose)));

    if (!pledged) {
      if ((purposeMap = forswornMap.get(classElement)) == null) {
        forswornMap.put(classElement, purposeMap = new HashMap<>());
      }
      purposeMap.put(purpose, visibility.compose(purposeMap.get(purpose)));
    }
  }

  public boolean hasNoPurpose (TypeElement classElement) {

    HashMap<String, Visibility> purposeMap;

    return ((purposeMap = trackingMap.get(classElement)) == null) || purposeMap.isEmpty();
  }

  public boolean isForsworn (TypeElement classElement, String purpose, Direction direction) {

    HashMap<String, Visibility> purposeMap;

    if ((purposeMap = forswornMap.get(classElement)) != null) {

      Visibility visibility;

      if ((visibility = purposeMap.get(purpose)) != null) {

        return visibility.matches(direction);
      }
    }

    return false;
  }

  public Iterable<String> unfulfilledPurposes (TypeElement classElement, Direction direction, HashMap<String, Visibility> fulfilledMap) {

    HashMap<String, Visibility> purposeMap;
    HashSet<String> remainingSet = new HashSet<>();

    if ((purposeMap = trackingMap.get(classElement)) != null) {
      for (Map.Entry<String, Visibility> purposeEntry : purposeMap.entrySet()) {
        if (purposeEntry.getValue().matches(direction)) {
          remainingSet.add(purposeEntry.getKey());
        }
      }
    }
    for (Map.Entry<String, Visibility> fulfilledEntry : fulfilledMap.entrySet()) {
      if (fulfilledEntry.getValue().matches(direction)) {
        remainingSet.remove(fulfilledEntry.getKey());
      }
    }

    return remainingSet;
  }

  public Visibility getVisibility (TypeElement classElement, String purpose) {

    HashMap<String, Visibility> purposeMap;

    return ((purposeMap = trackingMap.get(classElement)) == null) ? null : purposeMap.get(purpose);
  }

  public boolean isVisible (ProcessingEnvironment processingEnvironment, ClassTracker classTracker, String purpose, Direction direction, TypeElement typeElement) {

    Visibility visibility;

    return (((visibility = getVisibility(typeElement, purpose)) != null) && visibility.matches(direction)) || classTracker.isPreCompiled(processingEnvironment, purpose, direction, typeElement);
  }
}
