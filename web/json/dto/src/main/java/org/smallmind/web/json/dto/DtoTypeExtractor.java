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

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class DtoTypeExtractor {

  public static DtoType extract (ProcessingEnvironment processingEnvironment, UsefulTypeMirrors usefulTypeMirrors, VisibilityTracker visibilityTracker, String purpose, Direction direction, TypeMirror typeMirror) {

    if (TypeKind.ARRAY.equals(typeMirror.getKind())) {

      TypeElement componentTypeElement;

      if ((componentTypeElement = getTypeElement(processingEnvironment, ((ArrayType)typeMirror).getComponentType())) != null) {
        if (isVisibleType(visibilityTracker, purpose, direction, componentTypeElement)) {

          return new ArrayDtoType(purpose, direction, componentTypeElement);
        }
      }
    } else if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

      TypeElement typeElement;

      if ((typeElement = getTypeElement(processingEnvironment, typeMirror)) != null) {
        if (isVisibleType(visibilityTracker, purpose, direction, typeElement)) {

          return new ClassDtoType(purpose, direction, typeElement);
        } else if (processingEnvironment.getTypeUtils().isAssignable(typeMirror, usefulTypeMirrors.getCollectionTypeMirror())) {

          List<? extends TypeParameterElement> typeParameterElementList;

          if ((typeParameterElementList = typeElement.getTypeParameters()).size() == 1) {

            TypeElement genericTypeElement;

            if ((genericTypeElement = getTypeElement(processingEnvironment, typeParameterElementList.get(0).asType())) != null) {
              if (isVisibleType(visibilityTracker, purpose, direction, genericTypeElement)) {

                return new CollectionDtoType(purpose, direction, typeElement, genericTypeElement);
              }
            }
          }
        }
      }
    }

    return new NonDtoType(typeMirror);
  }

  private static TypeElement getTypeElement (ProcessingEnvironment processingEnvironment, TypeMirror typeMirror) {

    Element element;

    if (ElementKind.CLASS.equals((element = processingEnvironment.getTypeUtils().asElement(typeMirror)).getKind())) {

      return (TypeElement)element;
    }

    return null;
  }

  private static boolean isVisibleType (VisibilityTracker visibilityTracker, String purpose, Direction direction, TypeElement typeElement) {

    Visibility visibility;

    return ((visibility = visibilityTracker.getVisibility(typeElement, purpose)) != null) && visibility.matches(direction);
  }
}
