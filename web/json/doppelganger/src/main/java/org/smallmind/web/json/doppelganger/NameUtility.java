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

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Utility methods for deriving generated view class names and rendering type mirrors as source strings.
 */
public class NameUtility {

  /**
   * Returns the fully qualified package name of the given type element.
   *
   * @param processingEnvironment the current annotation processing environment
   * @param typeElement           the type whose package is required
   * @return the fully qualified package name
   */
  public static String getPackageName (ProcessingEnvironment processingEnvironment, TypeElement typeElement) {

    return processingEnvironment.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
  }

  /**
   * Builds the simple (unqualified) class name for a generated view by combining an optional prefix,
   * the originating class name, purpose, direction code, and the {@code "View"} suffix.
   *
   * @param processingEnvironment the current annotation processing environment (used for the {@code prefix} option)
   * @param purpose               the idiom purpose to append to the name, or empty for the default idiom
   * @param direction             the view direction whose code is appended to the name
   * @param typeElement           the originating annotated type
   * @return the simple class name of the generated view
   */
  public static String getSimpleName (ProcessingEnvironment processingEnvironment, String purpose, Direction direction, TypeElement typeElement) {

    StringBuilder viewNameBuilder = new StringBuilder((processingEnvironment.getOptions().get("prefix") == null) ? "" : processingEnvironment.getOptions().get("prefix")).append(typeElement.getSimpleName());

    if ((purpose != null) && (!purpose.isEmpty())) {
      viewNameBuilder.append(Character.toUpperCase(purpose.charAt(0))).append(purpose.substring(1));
    }

    return viewNameBuilder.append(direction.getCode()).append("View").toString();
  }

  /**
   * Renders a {@link TypeMirror} as a string suitable for inclusion in generated source, substituting
   * the generated view class name whenever a type is visible as a view for the given purpose and direction.
   *
   * @param processingEnvironment the current annotation processing environment
   * @param visibilityTracker     tracker that determines whether a type has a generated view
   * @param classTracker          tracker for polymorphic and hierarchy class relationships
   * @param purpose               the idiom purpose
   * @param direction             the view direction
   * @param typeMirror            the type to render
   * @return the string representation of the type as it should appear in generated source
   */
  public static String processTypeMirror (ProcessingEnvironment processingEnvironment, VisibilityTracker visibilityTracker, ClassTracker classTracker, String purpose, Direction direction, TypeMirror typeMirror) {

    StringBuilder nameBuilder = new StringBuilder();

    walkTypeMirror(nameBuilder, processingEnvironment, visibilityTracker, classTracker, purpose, direction, typeMirror);

    return nameBuilder.toString();
  }

  /**
   * Recursively builds the source string for a type mirror into the provided builder, handling arrays,
   * generic type arguments, and view name substitution.
   *
   * @param nameBuilder           the buffer to append to
   * @param processingEnvironment the current annotation processing environment
   * @param visibilityTracker     tracker used to check view visibility
   * @param classTracker          tracker for polymorphic and hierarchy classes
   * @param purpose               the idiom purpose
   * @param direction             the view direction
   * @param typeMirror            the type being rendered
   */
  private static void walkTypeMirror (StringBuilder nameBuilder, ProcessingEnvironment processingEnvironment, VisibilityTracker visibilityTracker, ClassTracker classTracker, String purpose, Direction direction, TypeMirror typeMirror) {

    switch (typeMirror.getKind()) {
      case TYPEVAR:
        nameBuilder.append('?');
        break;
      case ARRAY:
        walkTypeMirror(nameBuilder, processingEnvironment, visibilityTracker, classTracker, purpose, direction, ((ArrayType)typeMirror).getComponentType());
        nameBuilder.append("[]");
        break;
      case DECLARED:

        Element element = processingEnvironment.getTypeUtils().asElement(typeMirror);
        List<? extends TypeMirror> typeArgumentList;

        if (ElementKind.CLASS.equals(element.getKind())) {
          if (visibilityTracker.isVisible(processingEnvironment, classTracker, purpose, direction, (TypeElement)element)) {
            nameBuilder.append(NameUtility.getPackageName(processingEnvironment, (TypeElement)element)).append('.').append(NameUtility.getSimpleName(processingEnvironment, purpose, direction, (TypeElement)element));
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

            walkTypeMirror(nameBuilder, processingEnvironment, visibilityTracker, classTracker, purpose, direction, typeArgumentTypeMirror);
          }
          nameBuilder.append('>');
        }

        break;
      default:
        nameBuilder.append(typeMirror);
    }
  }
}
