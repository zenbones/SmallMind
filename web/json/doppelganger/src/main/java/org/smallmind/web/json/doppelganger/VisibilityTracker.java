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

/**
 * Records which purposes and directions are applicable to classes during processing.
 * Used to enforce pledges and to locate subclasses with matching visibility.
 */
public class VisibilityTracker {

  private final HashMap<TypeElement, HashMap<String, Visibility>> trackingMap = new HashMap<>();
  private final HashMap<TypeElement, HashMap<String, Visibility>> forswornMap = new HashMap<>();

  /**
   * Propagates visibility information from a superclass to a subclass.
   *
   * @param classElement      subclass being processed
   * @param superclassElement superclass carrying visibility data
   */
  public void add (TypeElement classElement, TypeElement superclassElement) {

    HashMap<String, Visibility> purposeMap;

    if ((purposeMap = trackingMap.get(superclassElement)) != null) {
      for (Map.Entry<String, Visibility> purposeEntry : purposeMap.entrySet()) {
        add(classElement, purposeEntry.getKey(), purposeEntry.getValue(), false);
      }
    }
  }

  /**
   * Records visibility for a purpose when real or virtual properties are present.
   *
   * @param classElement    class being processed
   * @param purpose         purpose identifier
   * @param visibility      visibility to record
   * @param propertyLexicon property metadata informing whether fields exist
   */
  public void add (TypeElement classElement, String purpose, Visibility visibility, PropertyLexicon propertyLexicon) {

    if (propertyLexicon.isReal() || propertyLexicon.isVirtual()) {
      add(classElement, purpose, visibility, false);
    }
  }

  /**
   * Records visibility for a purpose, optionally marking it as pledged (excluded from forsworn tracking).
   *
   * @param classElement class being processed
   * @param purpose      purpose identifier
   * @param visibility   visibility to record
   * @param pledged      whether the visibility came from a pledge
   */
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

  /**
   * @param classElement class to inspect
   * @return {@code true} if no purposes have been recorded for the class
   */
  public boolean hasNoPurpose (TypeElement classElement) {

    HashMap<String, Visibility> purposeMap;

    return ((purposeMap = trackingMap.get(classElement)) == null) || purposeMap.isEmpty();
  }

  /**
   * Determines whether a pledged purpose/direction was never fulfilled.
   *
   * @param classElement class to inspect
   * @param purpose      pledged purpose
   * @param direction    direction being checked
   * @return {@code true} if the purpose/direction is missing from generated output
   */
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

  /**
   * Computes purposes that still need to be generated for the given direction.
   *
   * @param classElement class to inspect
   * @param direction    direction being evaluated
   * @param fulfilledMap purposes already generated
   * @return iterable of remaining purposes
   */
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

  /**
   * Retrieves visibility recorded for a specific class/purpose pair.
   *
   * @param classElement class to inspect
   * @param purpose      purpose identifier
   * @return visibility value or {@code null} if none recorded
   */
  public Visibility getVisibility (TypeElement classElement, String purpose) {

    HashMap<String, Visibility> purposeMap;

    return ((purposeMap = trackingMap.get(classElement)) == null) ? null : purposeMap.get(purpose);
  }

  /**
   * Determines whether a type is visible for a given purpose/direction either because it was recorded
   * during processing or already exists on the classpath.
   *
   * @param processingEnvironment current processing environment
   * @param classTracker          tracker that can detect precompiled generated types
   * @param purpose               purpose identifier
   * @param direction             direction being evaluated
   * @param typeElement           type to inspect
   * @return {@code true} if the type is usable for the given purpose/direction
   */
  public boolean isVisible (ProcessingEnvironment processingEnvironment, ClassTracker classTracker, String purpose, Direction direction, TypeElement typeElement) {

    Visibility visibility;

    return (((visibility = getVisibility(typeElement, purpose)) != null) && visibility.matches(direction)) || classTracker.isPreCompiled(processingEnvironment, purpose, direction, typeElement);
  }
}
