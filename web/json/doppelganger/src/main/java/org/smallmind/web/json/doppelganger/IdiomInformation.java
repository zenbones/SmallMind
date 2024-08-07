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

import java.util.LinkedList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;

public class IdiomInformation {

  private final List<ConstraintInformation> constraintList = new LinkedList<>();
  private final List<String> purposeList;
  private final Visibility visibility;
  private Boolean required;

  public IdiomInformation (ProcessingEnvironment processingEnvironment, UsefulTypeMirrors usefulTypeMirrors, AnnotationMirror idiomAnnotationMirror) {

    visibility = AptUtility.extractAnnotationValue(idiomAnnotationMirror, "visibility", Visibility.class, Visibility.BOTH);
    purposeList = AptUtility.extractAnnotationValueAsList(idiomAnnotationMirror, "purposes", String.class);

    for (AnnotationMirror constraintAnnotationMirror : AptUtility.extractAnnotationValueAsList(idiomAnnotationMirror, "constraints", AnnotationMirror.class)) {
      constraintList.add(new ConstraintInformation(constraintAnnotationMirror));
    }

    if (!(required = AptUtility.extractAnnotationValue(idiomAnnotationMirror, "required", Boolean.class, Boolean.FALSE))) {
      for (ConstraintInformation constraintInformation : constraintList) {
        if (processingEnvironment.getTypeUtils().isSameType(usefulTypeMirrors.getNotNullTypeMirror(), constraintInformation.getType())) {
          required = true;
          break;
        }
      }
    }
  }

  public Visibility getVisibility () {

    return visibility;
  }

  public List<String> getPurposeList () {

    return purposeList;
  }

  public List<ConstraintInformation> getConstraintList () {

    return constraintList;
  }

  public boolean isRequired () {

    return (required != null) && required;
  }
}
