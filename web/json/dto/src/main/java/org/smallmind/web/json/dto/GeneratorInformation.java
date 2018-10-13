/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class GeneratorInformation {

  private final DirectionalGuide inDirectionalGuide = new DirectionalGuide(Direction.IN);
  private final DirectionalGuide outDirectionalGuide = new DirectionalGuide(Direction.OUT);
  private final HashMap<String, Visibility> pledgedMap = new HashMap<>();
  private final HashMap<String, Visibility> fulfilledMap = new HashMap<>();
  private final List<TypeElement> polymorphicSubClassList;
  private final TypeMirror polymorphicBaseClass;
  private final String name;
  private final boolean attributedPolymorphism;

  public GeneratorInformation (ProcessingEnvironment processingEnvironment, DtoAnnotationProcessor dtoAnnotationProcessor, TypeElement classElement, VisibilityTracker visibilityTracker, AnnotationMirror generatorAnnotationMirror)
    throws IOException, DtoDefinitionException {

    name = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "name", String.class, "");
    polymorphicBaseClass = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "polymorphicBaseClass", TypeMirror.class, null);
    polymorphicSubClassList = AptUtility.toConcreteList(processingEnvironment, AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "polymorphicSubClasses", TypeMirror.class));
    attributedPolymorphism = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "attributedPolymorphism", Boolean.class, false);

    for (AnnotationMirror pledgeAnnotationMirror : AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "pledges", AnnotationMirror.class)) {

      PledgeInformation pledgeInformation = new PledgeInformation(pledgeAnnotationMirror);

      for (String purpose : pledgeInformation.getPurposeList()) {
        visibilityTracker.add(classElement, purpose, pledgeInformation.getVisibility(), true);
        pledgedMap.put(purpose, pledgeInformation.getVisibility().compose(pledgedMap.get(purpose)));
      }
    }

    for (AnnotationMirror propertyAnnotationMirror : AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "properties", AnnotationMirror.class)) {

      String fieldName = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "field", String.class, null);

      for (PropertyBox propertyBox : new PropertyParser(propertyAnnotationMirror, AptUtility.extractAnnotationValue(propertyAnnotationMirror, "type", TypeMirror.class, null), true)) {

        dtoAnnotationProcessor.processTypeMirror(propertyBox.getPropertyInformation().getType());

        switch (propertyBox.getVisibility()) {
          case IN:
            inDirectionalGuide.put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
            break;
          case OUT:
            outDirectionalGuide.put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
            break;
          case BOTH:
            inDirectionalGuide.put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
            outDirectionalGuide.put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
            break;
          default:
            throw new UnknownSwitchCaseException(propertyBox.getVisibility().name());
        }
      }
    }
  }

  public void update (TypeElement classElement, VisibilityTracker visibilityTracker) {

    for (Map.Entry<String, PropertyLexicon> purposeEntry : inDirectionalGuide.entrySet()) {
      visibilityTracker.add(classElement, purposeEntry.getKey(), Visibility.IN, purposeEntry.getValue());
    }
    for (Map.Entry<String, PropertyLexicon> purposeEntry : outDirectionalGuide.entrySet()) {
      visibilityTracker.add(classElement, purposeEntry.getKey(), Visibility.OUT, purposeEntry.getValue());
    }
  }

  public String getName () {

    return name;
  }

  public TypeMirror getPolymorphicBaseClass () {

    return polymorphicBaseClass;
  }

  public List<TypeElement> getPolymorphicSubClassList () {

    return polymorphicSubClassList;
  }

  public boolean isAttributedPolymorphism () {

    return attributedPolymorphism;
  }

  public DirectionalGuide getInDirectionalGuide () {

    return inDirectionalGuide;
  }

  public DirectionalGuide getOutDirectionalGuide () {

    return outDirectionalGuide;
  }

  public void denotePurpose (String purpose, Direction direction) {

    fulfilledMap.put(purpose, direction.getVisibility().compose(fulfilledMap.get(purpose)));
  }

  public Iterable<String> unfulfilledPurposes (TypeElement classElement, VisibilityTracker visibilityTracker, Direction direction) {

    return visibilityTracker.unfulfilledPurposes(classElement, direction, fulfilledMap);
  }

  public String[] overwroughtPurposes (TypeElement classElement, VisibilityTracker visibilityTracker, Direction direction) {

    HashSet<String> overwroughtSet = new HashSet<>();
    String[] purposes;

    for (Map.Entry<String, Visibility> pledgedEntry : pledgedMap.entrySet()) {
      if (pledgedEntry.getValue().matches(direction)) {
        if (visibilityTracker.isForsworn(classElement, pledgedEntry.getKey(), direction)) {
          overwroughtSet.add(pledgedEntry.getKey());
        }
      }
    }

    purposes = new String[overwroughtSet.size()];
    overwroughtSet.toArray(purposes);

    return purposes;
  }
}