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

import java.util.Iterator;
import java.util.LinkedList;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class TypeElementIterable implements Iterable<TypeElement> {

  private final LinkedList<TypeElement> typeElementList = new LinkedList<>();

  public TypeElementIterable (ProcessingEnvironment processingEnvironment, TypeMirror typeMirror) {

    pushTypeMirror(processingEnvironment, typeMirror);
  }

  private void pushTypeMirror (ProcessingEnvironment processingEnvironment, TypeMirror typeMirror) {

    if (TypeKind.ARRAY.equals(typeMirror.getKind())) {
      pushTypeMirror(processingEnvironment, ((ArrayType)typeMirror).getComponentType());
    } else if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

      Element element = processingEnvironment.getTypeUtils().asElement(typeMirror);

      if (ElementKind.CLASS.equals(element.getKind())) {
        typeElementList.add((TypeElement)element);
      }

      for (TypeMirror typeArgumentTyoeMirror : ((DeclaredType)typeMirror).getTypeArguments()) {
        pushTypeMirror(processingEnvironment, typeArgumentTyoeMirror);
      }
    }
  }

  private void pushTypeElement (ProcessingEnvironment processingEnvironment, TypeElement typeElement) {

    if (ElementKind.CLASS.equals(typeElement.getKind())) {
      typeElementList.add(typeElement);
    }

    for (TypeParameterElement typeParameterElement : typeElement.getTypeParameters()) {
      pushTypeMirror(processingEnvironment, typeParameterElement.asType());
    }
  }

  @Override
  public Iterator<TypeElement> iterator () {

    return typeElementList.iterator();
  }
}
