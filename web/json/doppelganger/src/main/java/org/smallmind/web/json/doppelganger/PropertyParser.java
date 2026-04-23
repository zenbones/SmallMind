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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;

/**
 * Parses a property annotation into one or more {@link PropertyBox} entries, expanding any declared
 * {@link Idiom}s into individual purpose/visibility combinations.
 */
public class PropertyParser implements Iterable<PropertyBox> {

  private final LinkedList<PropertyBox> entryList = new LinkedList<>();

  /**
   * Builds property boxes from the given annotation mirror, creating one entry per idiom/purpose combination,
   * or a single default entry when no idioms are declared.
   *
   * @param processingEnvironment     the current annotation processing environment
   * @param usefulTypeMirrors         cached type mirrors for commonly referenced annotation types
   * @param propertyAnnotationMirror  the annotation mirror of the property annotation to parse
   * @param type                      the resolved type mirror of the property
   * @param nullifierAnnotationMirror the annotation mirror of an associated nullifier annotation, or {@code null}
   * @param virtual                   {@code true} if the property is virtual and not backed by an entity field
   */
  public PropertyParser (ProcessingEnvironment processingEnvironment, UsefulTypeMirrors usefulTypeMirrors, AnnotationMirror propertyAnnotationMirror, TypeMirror type, AnnotationMirror nullifierAnnotationMirror, boolean virtual) {

    String nullifierMessage = (nullifierAnnotationMirror == null) ? null : AptUtility.extractAnnotationValueWithDefault(processingEnvironment, nullifierAnnotationMirror, "message", String.class);
    boolean hasIdioms = false;

    for (AnnotationMirror idiomAnnotationMirror : AptUtility.extractAnnotationValueAsList(propertyAnnotationMirror, "idioms", AnnotationMirror.class)) {

      IdiomInformation idiomInformation = new IdiomInformation(processingEnvironment, usefulTypeMirrors, idiomAnnotationMirror);

      hasIdioms = true;
      if (idiomInformation.getPurposeList().isEmpty()) {
        entryList.add(new PropertyBox(idiomInformation.getVisibility(), "", new PropertyInformation(propertyAnnotationMirror, idiomInformation.getConstraintList(), idiomInformation.isRequired(), type, nullifierMessage, virtual)));
      } else {
        for (String purpose : idiomInformation.getPurposeList()) {
          entryList.add(new PropertyBox(idiomInformation.getVisibility(), purpose, new PropertyInformation(propertyAnnotationMirror, idiomInformation.getConstraintList(), idiomInformation.isRequired(), type, nullifierMessage, virtual)));
        }
      }
    }
    if (!hasIdioms) {
      entryList.add(new PropertyBox(Visibility.BOTH, "", new PropertyInformation(propertyAnnotationMirror, Collections.emptyList(), false, type, nullifierMessage, virtual)));
    }
  }

  /**
   * @return an iterator over the parsed {@link PropertyBox} entries
   */
  @Override
  public Iterator<PropertyBox> iterator () {

    return entryList.iterator();
  }
}
