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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class DtoNameUtility {

  public static String getPackageName (ProcessingEnvironment processingEnvironment, TypeElement typeElement) {

    return processingEnvironment.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
  }

  public static String getSimpleName (ProcessingEnvironment processingEnvironment, String purpose, Direction direction, TypeElement typeElement) {

    StringBuilder dtoNameBuilder = new StringBuilder((processingEnvironment.getOptions().get("prefix") == null) ? "" : processingEnvironment.getOptions().get("prefix")).append(typeElement.getSimpleName());

    if ((purpose != null) && (!purpose.isEmpty())) {
      dtoNameBuilder.append(Character.toUpperCase(purpose.charAt(0))).append(purpose.substring(1));
    }

    return dtoNameBuilder.append(direction.getCode()).append("Dto").toString();
  }

  public static String processTypeMirror (ProcessingEnvironment processingEnvironment, VisibilityTracker visibilityTracker, String purpose, Direction direction, TypeMirror typeMirror) {

    StringBuilder nameBuilder = new StringBuilder();

    walkTypeMirror(nameBuilder, processingEnvironment, visibilityTracker, purpose, direction, typeMirror);

    return nameBuilder.toString();
  }

  private static void walkTypeMirror (StringBuilder nameBuilder, ProcessingEnvironment processingEnvironment, VisibilityTracker visibilityTracker, String purpose, Direction direction, TypeMirror typeMirror) {

    switch (typeMirror.getKind()) {
      case TYPEVAR:
        nameBuilder.append('?');
        break;
      case ARRAY:
        walkTypeMirror(nameBuilder, processingEnvironment, visibilityTracker, purpose, direction, ((ArrayType)typeMirror).getComponentType());
        nameBuilder.append("[]");
        break;
      case DECLARED:

        Element element = processingEnvironment.getTypeUtils().asElement(typeMirror);
        List<? extends TypeMirror> typeArgumentList;

        if (ElementKind.CLASS.equals(element.getKind())) {
          if (isVisible(visibilityTracker, purpose, direction, (TypeElement)element)) {
            nameBuilder.append(DtoNameUtility.getPackageName(processingEnvironment, (TypeElement)element)).append('.').append(DtoNameUtility.getSimpleName(processingEnvironment, purpose, direction, (TypeElement)element));
          } else {
            nameBuilder.append(((TypeElement)element).getQualifiedName());
          }
        } else {
          nameBuilder.append(((TypeElement)element).getQualifiedName());
        }

        if (!(typeArgumentList = ((DeclaredType)typeMirror).getTypeArguments()).isEmpty()) {

          boolean first = true;

          nameBuilder.append('<');
          for (TypeMirror typeArgumentTypeMirror : typeArgumentList) {
            if (!first) {
              nameBuilder.append(", ");
            }
            first = false;

            walkTypeMirror(nameBuilder, processingEnvironment, visibilityTracker, purpose, direction, typeArgumentTypeMirror);
          }
          nameBuilder.append('>');
        }

        break;
      default:
        nameBuilder.append(typeMirror);
    }
  }

  private static boolean isVisible (VisibilityTracker visibilityTracker, String purpose, Direction direction, TypeElement typeElement) {

    Visibility visibility;

    return ((visibility = visibilityTracker.getVisibility(typeElement, purpose)) != null) && visibility.matches(direction);
  }
}