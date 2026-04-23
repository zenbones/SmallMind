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

import java.util.LinkedList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;

/**
 * Parsed representation of an {@link Idiom} annotation, capturing constraints, purposes, visibility, and required flag.
 */
public class IdiomInformation {

  private final List<ConstraintInformation> constraintList = new LinkedList<>();
  private final List<String> purposeList;
  private final Visibility visibility;
  private Boolean required;

  /**
   * Extracts idiom metadata from the given annotation mirror, inferring {@code required} from a {@code @NotNull}
   * constraint when the explicit flag is false.
   *
   * @param processingEnvironment the current annotation processing environment
   * @param usefulTypeMirrors     cached type mirrors used to detect a {@code @NotNull} constraint
   * @param idiomAnnotationMirror the annotation mirror of the {@link Idiom} to parse
   */
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

  /**
   * @return the visibility (IN, OUT, or BOTH) for which this idiom applies
   */
  public Visibility getVisibility () {

    return visibility;
  }

  /**
   * @return the list of purpose identifiers this idiom is scoped to
   */
  public List<String> getPurposeList () {

    return purposeList;
  }

  /**
   * @return the constraints to apply under this idiom
   */
  public List<ConstraintInformation> getConstraintList () {

    return constraintList;
  }

  /**
   * @return {@code true} if the property is required under this idiom, either explicitly or via {@code @NotNull}
   */
  public boolean isRequired () {

    return (required != null) && required;
  }
}
