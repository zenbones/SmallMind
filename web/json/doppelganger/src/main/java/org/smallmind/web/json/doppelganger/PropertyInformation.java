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

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;

public class PropertyInformation {

  private final List<ConstraintInformation> constraintList;
  private final TypeMirror adapter;
  private final TypeMirror as;
  private final TypeMirror type;
  private final String name;
  private final String comment;
  private final Boolean required;
  private final boolean virtual;
  private final String nullifierAnnotationName;

  public PropertyInformation (AnnotationMirror propertyAnnotationMirror, List<ConstraintInformation> constraintList, boolean idiomRequired, TypeMirror type, String nullifierAnnotationName, boolean virtual) {

    this.type = type;
    this.virtual = virtual;
    this.constraintList = constraintList;
    this.nullifierAnnotationName = nullifierAnnotationName;

    adapter = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "adapter", TypeMirror.class, null);
    as = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "as", TypeMirror.class, null);
    name = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "name", String.class, "");
    comment = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "comment", String.class, "");
    required = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "required", Boolean.class, Boolean.FALSE) || idiomRequired;
  }

  public boolean isVirtual () {

    return virtual;
  }

  public TypeMirror getAdapter () {

    return adapter;
  }

  public TypeMirror getAs () {

    return as;
  }

  public TypeMirror getType () {

    return type;
  }

  public String getName () {

    return name;
  }

  public String getComment () {

    return comment;
  }

  public String getNullifierAnnotationName () {

    return nullifierAnnotationName;
  }

  public Iterable<ConstraintInformation> constraints () {

    return constraintList;
  }

  public boolean isRequired () {

    return (required != null) && required;
  }
}
