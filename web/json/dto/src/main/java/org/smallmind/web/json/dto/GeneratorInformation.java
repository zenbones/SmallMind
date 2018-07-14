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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class GeneratorInformation {

  private final DirectionalMap inMap = new DirectionalMap(Direction.IN);
  private final DirectionalMap outMap = new DirectionalMap(Direction.OUT);
  private final HashSet<String> inPurposeSet = new HashSet<>();
  private final HashSet<String> outPurposeSet = new HashSet<>();
  private final List<TypeElement> polymorphicSubclassList;
  private final String name;
  private final Boolean polymorphic;

  public GeneratorInformation (ProcessingEnvironment processingEnvironment, DtoAnnotationProcessor dtoAnnotationProcessor, AnnotationMirror generatorAnnotationMirror, UsefulTypeMirrors usefulTypeMirrors)
    throws IOException, DtoDefinitionException {

    name = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "name", String.class, "");
    polymorphicSubclassList = AptUtility.toConcreteList(processingEnvironment, AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "polymorphicSubClasses", TypeMirror.class));
    polymorphic = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "polymorphic", Boolean.class, Boolean.FALSE);

    inPurposeSet.addAll(AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "purposes", String.class));
    outPurposeSet.addAll(inPurposeSet);

    for (AnnotationMirror propertyAnnotationMirror : AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "properties", AnnotationMirror.class)) {

      PropertyInformation propertyInformation = new PropertyInformation(AptUtility.extractAnnotationValue(propertyAnnotationMirror, "type", TypeMirror.class, null), propertyAnnotationMirror, usefulTypeMirrors, true);
      List<String> purposes = AptUtility.extractAnnotationValueAsList(propertyAnnotationMirror, "purposes", String.class);
      String fieldName = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "field", String.class, null);

      dtoAnnotationProcessor.processTypeMirror(propertyInformation.getType());

      switch (propertyInformation.getVisibility()) {
        case IN:
          inMap.put(purposes, fieldName, propertyInformation);
          break;
        case OUT:
          outMap.put(purposes, fieldName, propertyInformation);
          break;
        case BOTH:
          inMap.put(purposes, fieldName, propertyInformation);
          outMap.put(purposes, fieldName, propertyInformation);
          break;
        default:
          throw new UnknownSwitchCaseException(propertyInformation.getVisibility().name());
      }
    }
  }

  public String getName () {

    return name;
  }

  public boolean isPolymorphic () {

    return ((polymorphic != null) && polymorphic) || (!polymorphicSubclassList.isEmpty());
  }

  public List<TypeElement> getPolymorphicSubclassList () {

    return polymorphicSubclassList;
  }

  public DirectionalMap getInMap () {

    return inMap;
  }

  public DirectionalMap getOutMap () {

    return outMap;
  }

  public void denotePurpose (Direction direction, String purpose) {

    switch (direction) {
      case IN:
        inPurposeSet.remove(purpose);
        break;
      case OUT:
        outPurposeSet.remove(purpose);
        break;
      default:
        throw new UnknownSwitchCaseException(direction.name());
    }
  }

  public Iterable<String> unfulfilledPurposes (Direction direction) {

    switch (direction) {
      case IN:

        return inPurposeSet;
      case OUT:

        return outPurposeSet;
      default:
        throw new UnknownSwitchCaseException(direction.name());
    }
  }
}