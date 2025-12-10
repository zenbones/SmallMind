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

/**
 * Captures metadata for a single property appearing in a generated view, including constraints,
 * naming overrides, adapters, and nullifier comments.
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
   * Extracts property metadata from a {@link View}, {@link Virtual}, or {@link Real} annotation.
   *
   * @param propertyAnnotationMirror the annotation describing the property
   * @param constraintList           constraints to apply
   * @param idiomRequired            whether the idiom marks the property as required
   * @param type                     resolved property type
   * @param nullifierMessage         optional nullifier message string
   * @param virtual                  whether the property is virtual (not backed by a real field)
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
   * @return {@code true} when the property is virtual (not present on the entity)
   */
  public boolean isVirtual () {

    return virtual;
  }

  /**
   * @return adapter type to apply on the generated view property, or {@code null} if none was specified
   */
  public TypeMirror getAdapter () {

    return adapter;
  }

  /**
   * @return override target class for {@link org.smallmind.web.json.scaffold.util.As}, or {@code null}
   */
  public TypeMirror getAs () {

    return as;
  }

  /**
   * @return the resolved type of the property
   */
  public TypeMirror getType () {

    return type;
  }

  /**
   * @return custom element name override for serialization, or empty when defaulting to field name
   */
  public String getName () {

    return name;
  }

  /**
   * @return comment text to apply to the generated property, or empty when none
   */
  public String getComment () {

    return comment;
  }

  /**
   * @return nullifier message set on the property (may be {@code null})
   */
  public String getNullifierMessage () {

    return nullifierMessage;
  }

  /**
   * @return constraints applied to this property
   */
  public Iterable<ConstraintInformation> constraints () {

    return constraintList;
  }

  /**
   * @return {@code true} if the property is required in the current idiom
   */
  public boolean isRequired () {

    return (required != null) && required;
  }
}
