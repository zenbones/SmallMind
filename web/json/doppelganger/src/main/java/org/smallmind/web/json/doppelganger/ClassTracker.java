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
 * Tracks polymorphic and hierarchy relationships discovered while processing {@link Doppelganger} annotations,
 * and caches whether generated view classes are already compiled on the classpath.
 */
public class ClassTracker {

  private final HashMap<String, Boolean> preCompiledMap = new HashMap<>();
  private final HashMap<TypeElement, PolymorphicInformation> polymorphicInformationMap = new HashMap<>();
  private final HashMap<TypeElement, HierarchyInformation> hierarchyInformationMap = new HashMap<>();
  private final HashMap<TypeElement, TypeElement> polymorphicBaseClassMap = new HashMap<>();
  private final HashMap<TypeElement, TypeElement> hierarchyBaseClassMap = new HashMap<>();

  /**
   * Registers a polymorphic base class and indexes all of its declared subclasses.
   *
   * @param typeElement            the polymorphic base class element
   * @param polymorphicInformation parsed metadata from the {@link Polymorphic} annotation
   */
  public void addPolymorphic (TypeElement typeElement, PolymorphicInformation polymorphicInformation) {

    polymorphicInformationMap.put(typeElement, polymorphicInformation);
    for (TypeElement subClassElement : polymorphicInformation.getSubClassList()) {
      polymorphicBaseClassMap.put(subClassElement, typeElement);
    }
  }

  /**
   * Returns whether the given type element is part of any registered polymorphic relationship,
   * either as a base class or as a subclass.
   *
   * @param typeElement the type element to test
   * @return {@code true} if the element is a polymorphic base or subclass
   */
  public boolean isPolymorphic (TypeElement typeElement) {

    return polymorphicInformationMap.containsKey(typeElement) || polymorphicBaseClassMap.containsKey(typeElement);
  }

  /**
   * Returns whether a registered polymorphic base class has at least one declared subclass.
   *
   * @param typeElement the polymorphic base class element
   * @return {@code true} if at least one subclass was declared
   */
  public boolean hasPolymorphicSubClasses (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return !polymorphicInformation.getSubClassList().isEmpty();
    }

    return false;
  }

  /**
   * Returns the list of declared polymorphic subclasses for the given base class.
   *
   * @param typeElement the polymorphic base class element
   * @return list of subclass elements, or an empty list if none are registered
   */
  public List<TypeElement> getPolymorphicSubclasses (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return polymorphicInformation.getSubClassList();
    }

    return Collections.emptyList();
  }

  /**
   * Returns whether the polymorphic discriminator for the given base class should be emitted as an XML attribute.
   *
   * @param typeElement the polymorphic base class element
   * @return {@code true} if the attribute-based discriminator style is requested
   */
  public boolean usePolymorphicAttribute (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return polymorphicInformation.isUseAttribute();
    }

    return false;
  }

  /**
   * Returns whether the given type element is a registered polymorphic subclass.
   *
   * @param typeElement the type element to test
   * @return {@code true} if a polymorphic base has been recorded for this element
   */
  public boolean hasPolymorphicBaseClass (TypeElement typeElement) {

    return polymorphicBaseClassMap.containsKey(typeElement);
  }

  /**
   * Returns the polymorphic base class for the given subclass element.
   *
   * @param subClassElement the subclass element
   * @return the base class element, or {@code null} if not registered
   */
  public TypeElement getPolymorphicBaseClass (TypeElement subClassElement) {

    return polymorphicBaseClassMap.get(subClassElement);
  }

  /**
   * Registers a hierarchy base class and indexes all of its declared subclasses.
   *
   * @param typeElement          the hierarchy base class element
   * @param hierarchyInformation parsed metadata from the {@link Hierarchy} annotation
   */
  public void addHierarchy (TypeElement typeElement, HierarchyInformation hierarchyInformation) {

    hierarchyInformationMap.put(typeElement, hierarchyInformation);
    for (TypeElement subClassElement : hierarchyInformation.getSubClassList()) {
      hierarchyBaseClassMap.put(subClassElement, typeElement);
    }
  }

  /**
   * Returns whether a registered hierarchy base class has at least one declared subclass.
   *
   * @param typeElement the hierarchy base class element
   * @return {@code true} if at least one subclass was declared
   */
  public boolean hasHierarchySubClasses (TypeElement typeElement) {

    HierarchyInformation hierarchyInformation;

    if ((hierarchyInformation = hierarchyInformationMap.get(typeElement)) != null) {

      return !hierarchyInformation.getSubClassList().isEmpty();
    }

    return false;
  }

  /**
   * Returns the list of declared hierarchy subclasses for the given base class.
   *
   * @param typeElement the hierarchy base class element
   * @return list of subclass elements, or an empty list if none are registered
   */
  public List<TypeElement> getHierarchySubclasses (TypeElement typeElement) {

    HierarchyInformation hierarchyInformation;

    if ((hierarchyInformation = hierarchyInformationMap.get(typeElement)) != null) {

      return hierarchyInformation.getSubClassList();
    }

    return Collections.emptyList();
  }

  /**
   * Returns whether the given type element is a registered hierarchy subclass.
   *
   * @param typeElement the type element to test
   * @return {@code true} if a hierarchy base has been recorded for this element
   */
  public boolean hasHierarchyBaseClass (TypeElement typeElement) {

    return hierarchyBaseClassMap.containsKey(typeElement);
  }

  /**
   * Returns whether a generated view for the given type element already exists on the classpath.
   *
   * @param typeElement the type element to check
   * @return {@code true} if the type's qualified name resolves to a loadable class
   */
  public boolean isPreCompiled (TypeElement typeElement) {

    return isPreCompiled(typeElement.getQualifiedName().toString());
  }

  /**
   * Returns whether the generated view for the given purpose, direction, and type already exists on the classpath.
   *
   * @param processingEnvironment the current annotation processing environment
   * @param purpose               the view purpose suffix
   * @param direction             the view direction suffix
   * @param typeElement           the originating annotated type
   * @return {@code true} if the computed view class name resolves to a loadable class
   */
  public boolean isPreCompiled (ProcessingEnvironment processingEnvironment, String purpose, Direction direction, TypeElement typeElement) {

    return isPreCompiled(NameUtility.getPackageName(processingEnvironment, typeElement) + '.' + NameUtility.getSimpleName(processingEnvironment, purpose, direction, typeElement));
  }

  /**
   * Attempts to load a class by fully qualified name and caches whether it is present on the classpath.
   *
   * @param qualifiedName the fully qualified class name to probe
   * @return {@code true} if the class can be loaded
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
