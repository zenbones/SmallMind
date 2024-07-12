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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public class ClassTracker {

  private final HashMap<String, Boolean> preCompiledMap = new HashMap<>();
  private final HashMap<TypeElement, PolymorphicInformation> polymorphicInformationMap = new HashMap<>();
  private final HashMap<TypeElement, HierarchyInformation> hierarchyInformationMap = new HashMap<>();
  private final HashMap<TypeElement, TypeElement> polymorphicBaseClassMap = new HashMap<>();
  private final HashMap<TypeElement, TypeElement> hierarchyBaseClassMap = new HashMap<>();

  public void addPolymorphic (TypeElement typeElement, PolymorphicInformation polymorphicInformation) {

    polymorphicInformationMap.put(typeElement, polymorphicInformation);
    for (TypeElement subClassElement : polymorphicInformation.getSubClassList()) {
      polymorphicBaseClassMap.put(subClassElement, typeElement);
    }
  }

  public boolean isPolymorphic (TypeElement typeElement) {

    return polymorphicInformationMap.containsKey(typeElement) || polymorphicBaseClassMap.containsKey(typeElement);
  }

  public boolean hasPolymorphicSubClasses (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return !polymorphicInformation.getSubClassList().isEmpty();
    }

    return false;
  }

  public List<TypeElement> getPolymorphicSubclasses (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return polymorphicInformation.getSubClassList();
    }

    return Collections.emptyList();
  }

  public boolean usePolymorphicAttribute (TypeElement typeElement) {

    PolymorphicInformation polymorphicInformation;

    if ((polymorphicInformation = polymorphicInformationMap.get(typeElement)) != null) {

      return polymorphicInformation.isUseAttribute();
    }

    return false;
  }

  public boolean hasPolymorphicBaseClass (TypeElement typeElement) {

    return polymorphicBaseClassMap.containsKey(typeElement);
  }

  public TypeElement getPolymorphicBaseClass (TypeElement subClassElement) {

    return polymorphicBaseClassMap.get(subClassElement);
  }

  public void addHierarchy (TypeElement typeElement, HierarchyInformation hierachyInformation) {

    hierarchyInformationMap.put(typeElement, hierachyInformation);
    for (TypeElement subClassElement : hierachyInformation.getSubClassList()) {
      hierarchyBaseClassMap.put(subClassElement, typeElement);
    }
  }

  public boolean hasHierarchySubClasses (TypeElement typeElement) {

    HierarchyInformation hierarchyInformation;

    if ((hierarchyInformation = hierarchyInformationMap.get(typeElement)) != null) {

      return !hierarchyInformation.getSubClassList().isEmpty();
    }

    return false;
  }

  public List<TypeElement> getHierarchySubclasses (TypeElement typeElement) {

    HierarchyInformation hierarchyInformation;

    if ((hierarchyInformation = hierarchyInformationMap.get(typeElement)) != null) {

      return hierarchyInformation.getSubClassList();
    }

    return Collections.emptyList();
  }

  public boolean hasHierarchyBaseClass (TypeElement typeElement) {

    return hierarchyBaseClassMap.containsKey(typeElement);
  }

  public boolean isPreCompiled (TypeElement typeElement) {

    return isPreCompiled(typeElement.getQualifiedName().toString());
  }

  public boolean isPreCompiled (ProcessingEnvironment processingEnvironment, String purpose, Direction direction, TypeElement typeElement) {

    return isPreCompiled(new StringBuilder(NameUtility.getPackageName(processingEnvironment, typeElement)).append('.').append(NameUtility.getSimpleName(processingEnvironment, purpose, direction, typeElement)).toString());
  }

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
