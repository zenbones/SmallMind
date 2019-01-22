/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.web.json.dto.translator;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.smallmind.web.json.dto.Direction;
import org.smallmind.web.json.dto.ClassTracker;
import org.smallmind.web.json.dto.TypeElementIterable;
import org.smallmind.web.json.dto.UsefulTypeMirrors;
import org.smallmind.web.json.dto.VisibilityTracker;

public class DtoTranslatorFactory {

  private static final ArrayDtoTranslator ARRAY_DTO_TRANSLATOR = new ArrayDtoTranslator();
  private static final ClassDtoTranslator CLASS_DTO_TRANSLATOR = new ClassDtoTranslator();
  private static final ListDtoTranslator LIST_DTO_TRANSLATOR = new ListDtoTranslator();
  private static final NonDtoTranslator NON_DTO_TRANSLATOR = new NonDtoTranslator();

  public static DtoTranslator create (ProcessingEnvironment processingEnvironment, UsefulTypeMirrors usefulTypeMirrors, VisibilityTracker visibilityTracker, ClassTracker classTracker, String purpose, Direction direction, TypeMirror typeMirror) {

    boolean visible = false;

    for (TypeElement typeElement : new TypeElementIterable(processingEnvironment, typeMirror)) {
      if (visibilityTracker.isVisible(processingEnvironment, classTracker, purpose, direction, typeElement)) {
        visible = true;
        break;
      }
    }

    if (visible) {
      if (TypeKind.ARRAY.equals(typeMirror.getKind())) {

        TypeMirror componentTypeMirror = ((ArrayType)typeMirror).getComponentType();

        if (TypeKind.DECLARED.equals(componentTypeMirror.getKind()) && (((DeclaredType)componentTypeMirror).getTypeArguments().isEmpty())) {

          return ARRAY_DTO_TRANSLATOR;
        }
      } else if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

        List<? extends TypeMirror> typeArguments = ((DeclaredType)typeMirror).getTypeArguments();

        if (processingEnvironment.getTypeUtils().isAssignable(processingEnvironment.getTypeUtils().erasure(typeMirror), usefulTypeMirrors.getListTypeMirror()) && (typeArguments.size() == 1)) {

          return LIST_DTO_TRANSLATOR;
        } else if (typeArguments.size() == 0) {

          return CLASS_DTO_TRANSLATOR;
        }
      }
    }

    return NON_DTO_TRANSLATOR;
  }
}
