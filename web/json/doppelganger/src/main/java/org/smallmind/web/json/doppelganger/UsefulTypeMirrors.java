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

import java.io.Serializable;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import org.smallmind.nutsnbolts.reflection.OverlayNullifier;

public class UsefulTypeMirrors {

  private final TypeMirror serializableTypeMirror;
  private final TypeMirror viewTypeMirror;
  private final TypeMirror overlayNullifierTypeMirror;
  private final TypeMirror notNullTypeMirror;
  private final TypeMirror listTypeMirror;
  private final TypeMirror jsonNodeTypeMirror;
  private final TypeMirror objectTypeMirror;

  public UsefulTypeMirrors (ProcessingEnvironment processingEnvironment) {

    serializableTypeMirror = processingEnvironment.getElementUtils().getTypeElement(Serializable.class.getName()).asType();
    viewTypeMirror = processingEnvironment.getElementUtils().getTypeElement(View.class.getName()).asType();
    overlayNullifierTypeMirror = processingEnvironment.getElementUtils().getTypeElement(OverlayNullifier.class.getName()).asType();
    notNullTypeMirror = processingEnvironment.getElementUtils().getTypeElement(NotNull.class.getName()).asType();
    listTypeMirror = processingEnvironment.getElementUtils().getTypeElement(List.class.getName()).asType();
    jsonNodeTypeMirror = processingEnvironment.getElementUtils().getTypeElement(JsonNode.class.getName()).asType();
    objectTypeMirror = processingEnvironment.getElementUtils().getTypeElement(Object.class.getName()).asType();
  }

  public TypeMirror getSerializableTypeMirror () {

    return serializableTypeMirror;
  }

  public TypeMirror getViewTypeMirror () {

    return viewTypeMirror;
  }

  public TypeMirror getOverlayNullifierTypeMirror () {

    return overlayNullifierTypeMirror;
  }

  public TypeMirror getNotNullTypeMirror () {

    return notNullTypeMirror;
  }

  public TypeMirror getListTypeMirror () {

    return listTypeMirror;
  }

  public TypeMirror getJsonNodeTypeMirror () {

    return jsonNodeTypeMirror;
  }

  public TypeMirror getObjectTypeMirror () {

    return objectTypeMirror;
  }
}
