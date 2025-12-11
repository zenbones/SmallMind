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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Tracks polymorphic and hierarchical relationships discovered while parsing {@link Doppelganger} annotations.
 * The tracker helps determine generated adapter configuration and whether classes are already compiled.
 */
public class ClassTracker {

  private final HashMap<String, Boolean> preCompiledMap = new HashMap<>();
  private final HashMap<TypeElement, PolymorphicInformation> polymorphicInformationMap = new HashMap<>();
  private final HashMap<TypeElement, HierarchyInformation> hierarchyInformationMap = new HashMap<>();
  private final HashMap<TypeElement, TypeElement> polymorphicBaseClassMap = new HashMap<>();
  private final HashMap<TypeElement, TypeElement> hierarchyBaseClassMap = new HashMap<>();

  /**
   * Registers a class annotated with {@link Polymorphic} and its subclasses.
   *
   * @param typeElement            the polymorphic base class
   * @param polymorphicInformation parsed information for the annotation
   */
  public void addPolymorphic (TypeElement typeElement, PolymorphicInformation polymorphicInformation) {

    polymorphicInformationMap.put(typeElement, polymorphicInformation);
    for (TypeElement subClassElement : polymorphicInformation.getSubClassList()) {
      polymorphicBaseClassMap.put(subClassElement, typeElement);
    }
  }

  /**
   * Determines whether the provided element participates in a polymorphic graph.
   *
   * @param typeElement the class element to inspect
   * @return {@code true} when the class is a polymorphic base or subclass
   */
  public boolean isPolymorphic (TypeElement typeElement) {

    return polymorphicInformationMap.containsKey(typeElement) || polymorphicBaseClassMap.containsKey(typeElement);
  }

  /**
   * Checks if a polymorphic base has any registered subclasses.
   *
   * @param typeElement the base class
   * @return {@code true} when at least one subclass was declared
   */
  public boolean hasPolymorphicSubClasses (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return !polymorphicInformation.getSubClassList().isEmpty();
    }

    return false;
  }

  /**
   * Returns the list of polymorphic subclasses for a base class.
   *
   * @param typeElement the base class
   * @return an immutable list of subclass elements (may be empty)
   */
  public List<TypeElement> getPolymorphicSubclasses (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return polymorphicInformation.getSubClassList();
    }

    return Collections.emptyList();
  }

  /**
   * Indicates whether the polymorphic base requires an attribute-based discriminator.
   *
   * @param typeElement the polymorphic base
   * @return {@code true} if the generated adapter should use attributes instead of elements
   */
  public boolean usePolymorphicAttribute (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return polymorphicInformation.isUseAttribute();
    }

    return false;
  }

  /**
   * Checks if a class is a known polymorphic subclass.
   *
   * @param typeElement the class to check
   * @return {@code true} if a polymorphic base was recorded for the class
   */
  public boolean hasPolymorphicBaseClass (TypeElement typeElement) {

    return polymorphicBaseClassMap.containsKey(typeElement);
  }

  /**
   * Resolves the polymorphic base class for a subclass.
   *
   * @param subClassElement the subclass
   * @return the polymorphic base class, or {@code null} when not registered
   */
  public TypeElement getPolymorphicBaseClass (TypeElement subClassElement) {

    return polymorphicBaseClassMap.get(subClassElement);
  }

  /**
   * Registers a class annotated with {@link Hierarchy} and its subclasses.
   *
   * @param typeElement         the base of the hierarchy
   * @param hierachyInformation parsed hierarchy metadata
   */
  public void addHierarchy (TypeElement typeElement, HierarchyInformation hierachyInformation) {

    hierarchyInformationMap.put(typeElement, hierachyInformation);
    for (TypeElement subClassElement : hierachyInformation.getSubClassList()) {
      hierarchyBaseClassMap.put(subClassElement, typeElement);
    }
  }

  /**
   * Checks if a hierarchy base has any subclasses.
   *
   * @param typeElement the base class
   * @return {@code true} when at least one subclass exists
   */
  public boolean hasHierarchySubClasses (TypeElement typeElement) {

    HierarchyInformation hierarchyInformation;

    if ((hierarchyInformation = hierarchyInformationMap.get(typeElement)) != null) {

      return !hierarchyInformation.getSubClassList().isEmpty();
    }

    return false;
  }

  /**
   * Returns the subclasses registered under a hierarchy base.
   *
   * @param typeElement the base class
   * @return an immutable list of subclasses (may be empty)
   */
  public List<TypeElement> getHierarchySubclasses (TypeElement typeElement) {

    HierarchyInformation hierarchyInformation;

    if ((hierarchyInformation = hierarchyInformationMap.get(typeElement)) != null) {

      return hierarchyInformation.getSubClassList();
    }

    return Collections.emptyList();
  }

  /**
   * Checks if a class is a member of a registered hierarchy.
   *
   * @param typeElement the class to test
   * @return {@code true} if a hierarchy base was recorded for the class
   */
  public boolean hasHierarchyBaseClass (TypeElement typeElement) {

    return hierarchyBaseClassMap.containsKey(typeElement);
  }

  /**
   * Indicates whether a view class for the supplied type has already been compiled and available on the classpath.
   *
   * @param typeElement the class or interface to check
   * @return {@code true} if a compiled type exists
   */
  public boolean isPreCompiled (TypeElement typeElement) {

    return isPreCompiled(typeElement.getQualifiedName().toString());
  }

  /**
   * Indicates whether a generated view with a given purpose/direction/type name is already available.
   *
   * @param processingEnvironment current annotation processing environment
   * @param purpose               the view purpose suffix
   * @param direction             the view direction suffix
   * @param typeElement           the originating class
   * @return {@code true} if the generated view class can be loaded
   */
  public boolean isPreCompiled (ProcessingEnvironment processingEnvironment, String purpose, Direction direction, TypeElement typeElement) {

    return isPreCompiled(NameUtility.getPackageName(processingEnvironment, typeElement) + '.' + NameUtility.getSimpleName(processingEnvironment, purpose, direction, typeElement));
  }

  /**
   * Attempts to load a class by its fully qualified name and caches the result.
   *
   * @param qualifiedName the class name to resolve
   * @return {@code true} if the class is present on the classpath
   */
  private boolean isPreCompiled (String qualifiedName) {

    Boolean preCompiled;

    if ((preCompiled = preCompiledMap.get(qualifiedName)) == null) {
      try {
        Class.forName(qualifiedName);

        preCompiledMap.put(qualifiedName, preCompiled = Boolean.TRUE);
      } catch (ClassNotFoundException classNotFoundException) {
        preCompiledMap.put(qualifiedName, preCompiled = Boolean.FALSE);
      }
    }

    return preCompiled;
  }
}
