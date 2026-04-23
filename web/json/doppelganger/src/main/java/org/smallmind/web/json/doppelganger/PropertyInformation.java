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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;

/**
 * Captures all metadata for a single property to be emitted in a generated view, including its type,
 * constraints, JAXB adapter, element name, comment, and nullifier message.
 */
public class PropertyInformation {

  private final List<ConstraintInformation> constraintList;
  private final TypeMirror adapter;
  private final TypeMirror as;
  private final TypeMirror type;
  private final String name;
  private final String comment;
  private final Boolean required;
  private final boolean virtual;
  private final String nullifierMessage;

  /**
   * Extracts property metadata from a {@link View}, {@link Virtual}, or {@link Real} annotation mirror.
   *
   * @param propertyAnnotationMirror the annotation mirror of the property annotation
   * @param constraintList           constraints to apply to this property in the generated view
   * @param idiomRequired            whether the containing idiom marks this property as required
   * @param type                     the resolved type mirror for the property
   * @param nullifierMessage         the nullifier message extracted from a nullifier annotation, or {@code null}
   * @param virtual                  {@code true} if the property is virtual and not backed by an entity field
   */
  public PropertyInformation (AnnotationMirror propertyAnnotationMirror, List<ConstraintInformation> constraintList, boolean idiomRequired, TypeMirror type, String nullifierMessage, boolean virtual) {

    this.type = type;
    this.virtual = virtual;
    this.constraintList = constraintList;
    this.nullifierMessage = nullifierMessage;

    adapter = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "adapter", TypeMirror.class, null);
    as = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "as", TypeMirror.class, null);
    name = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "name", String.class, "");
    comment = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "comment", String.class, "");
    required = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "required", Boolean.class, Boolean.FALSE) || idiomRequired;
  }

  /**
   * @return {@code true} if this property is virtual and has no corresponding field on the entity
   */
  public boolean isVirtual () {

    return virtual;
  }

  /**
   * @return the JAXB adapter type to apply on the generated property, or {@code null} if none was specified
   */
  public TypeMirror getAdapter () {

    return adapter;
  }

  /**
   * @return the {@code @As} override type for tool hints, or {@code null} if none was specified
   */
  public TypeMirror getAs () {

    return as;
  }

  /**
   * @return the resolved type of this property
   */
  public TypeMirror getType () {

    return type;
  }

  /**
   * @return the XML element name override for this property, or empty to default to the field name
   */
  public String getName () {

    return name;
  }

  /**
   * @return the comment text to attach to the generated property, or empty when none
   */
  public String getComment () {

    return comment;
  }

  /**
   * @return the nullifier message to apply via {@code @NullifiedBy}, or {@code null} if none
   */
  public String getNullifierMessage () {

    return nullifierMessage;
  }

  /**
   * @return the constraints to apply to this property in the generated view
   */
  public Iterable<ConstraintInformation> constraints () {

    return constraintList;
  }

  /**
   * @return {@code true} if this property is required in the current idiom
   */
  public boolean isRequired () {

    return (required != null) && required;
  }
}
