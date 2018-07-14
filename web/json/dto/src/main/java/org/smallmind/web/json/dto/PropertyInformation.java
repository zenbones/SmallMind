/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;

public class PropertyInformation {

  private final List<ConstraintInformation> constraintInformationtList = new LinkedList<>();
  private final TypeMirror adapter;
  private final TypeMirror type;
  private final Visibility visibility;
  private final String name;
  private final Boolean required;
  private final boolean virtual;

  public PropertyInformation (TypeMirror type, AnnotationMirror propertyAnnotationMirror, UsefulTypeMirrors usefulTypeMirrors, boolean virtual) {

    this.type = type;
    this.virtual = virtual;

    adapter = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "adapter", TypeMirror.class, usefulTypeMirrors.getDefaultXmlAdapterTypeMirror());
    visibility = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "visibility", Visibility.class, Visibility.BOTH);
    name = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "name", String.class, "");
    required = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "required", Boolean.class, Boolean.FALSE);

    for (AnnotationMirror constraintAnnotationMirror : AptUtility.extractAnnotationValueAsList(propertyAnnotationMirror, "constraints", AnnotationMirror.class)) {
      constraintInformationtList.add(new ConstraintInformation(constraintAnnotationMirror));
    }
  }

  public boolean isVirtual () {

    return virtual;
  }

  public TypeMirror getAdapter () {

    return adapter;
  }

  public TypeMirror getType () {

    return type;
  }

  public Visibility getVisibility () {

    return visibility;
  }

  public String getName () {

    return name;
  }

  public Iterable<ConstraintInformation> getConstraints () {

    return constraintInformationtList;
  }

  public boolean isRequired () {

    return (required != null) && required;
  }
}

