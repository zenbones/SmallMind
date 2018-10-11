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
import java.util.HashSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class GeneratorInformation {

  private final DirectionalGuide inDirectionalGuide = new DirectionalGuide(Direction.IN);
  private final DirectionalGuide outDirectionalGuide = new DirectionalGuide(Direction.OUT);
  private final HashSet<String> inPledgedSet = new HashSet<>();
  private final HashSet<String> outPledgedSet = new HashSet<>();
  private final HashSet<String> inOverwroughtSet;
  private final HashSet<String> outOverwroughtSet;
  private final List<TypeElement> polymorphicSubClassList;
  private final TypeMirror polymorphicBaseClass;
  private final String name;

  public GeneratorInformation (ProcessingEnvironment processingEnvironment, DtoAnnotationProcessor dtoAnnotationProcessor, AnnotationMirror generatorAnnotationMirror)
    throws IOException, DtoDefinitionException {

    name = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "name", String.class, "");
    polymorphicBaseClass = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "polymorphicBaseClass", TypeMirror.class, null);
    polymorphicSubClassList = AptUtility.toConcreteList(processingEnvironment, AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "polymorphicSubClasses", TypeMirror.class));

    for (AnnotationMirror pledgeAnnotationMirror : AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "pledges", AnnotationMirror.class)) {

      PledgeInformation pledgeInformation = new PledgeInformation(pledgeAnnotationMirror);

      switch (pledgeInformation.getVisibility()) {
        case BOTH:
          inPledgedSet.addAll(pledgeInformation.getPurposeList());
          outPledgedSet.addAll(pledgeInformation.getPurposeList());
          break;
        case IN:
          inPledgedSet.addAll(pledgeInformation.getPurposeList());
          break;
        case OUT:
          outPledgedSet.addAll(pledgeInformation.getPurposeList());
          break;
        default:
          throw new UnknownSwitchCaseException(pledgeInformation.getVisibility().name());
      }
    }

    inOverwroughtSet = new HashSet<>(inPledgedSet);
    outOverwroughtSet = new HashSet<>(outPledgedSet);

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

  public String getName () {

    return name;
  }

  public TypeMirror getPolymorphicBaseClass () {

    return polymorphicBaseClass;
  }

  public List<TypeElement> getPolymorphicSubClassList () {

    return polymorphicSubClassList;
  }

  public DirectionalGuide getInDirectionalGuide () {

    return inDirectionalGuide;
  }

  public DirectionalGuide getOutDirectionalGuide () {

    return outDirectionalGuide;
  }

  public void denotePurpose (Direction direction, String purpose) {

    switch (direction) {
      case IN:
        inPledgedSet.remove(purpose);
        break;
      case OUT:
        outPledgedSet.remove(purpose);
        break;
      default:
        throw new UnknownSwitchCaseException(direction.name());
    }
  }

  public Iterable<String> pledgedPurposes (Direction direction) {

    switch (direction) {
      case IN:
        return inPledgedSet;
      case OUT:
        return outPledgedSet;
      default:
        throw new UnknownSwitchCaseException(direction.name());
    }
  }

  public Iterable<String> unfulfilledPurposes (Direction direction) {

    switch (direction) {
      case IN:
        // Avoids concurrent modification
        return new HashSet<>(inPledgedSet);
      case OUT:
        // Avoids concurrent modification
        return new HashSet<>(outPledgedSet);
      default:
        throw new UnknownSwitchCaseException(direction.name());
    }
  }

  public String[] overwroughtPurposes (Direction direction) {

    String[] purposes;

    switch (direction) {
      case IN:
        inOverwroughtSet.retainAll(inDirectionalGuide.keySet());
        purposes = new String[inOverwroughtSet.size()];
        inOverwroughtSet.toArray(purposes);

        return purposes;
      case OUT:
        outOverwroughtSet.retainAll(outDirectionalGuide.keySet());
        purposes = new String[outOverwroughtSet.size()];
        outOverwroughtSet.toArray(purposes);

        return purposes;
      default:
        throw new UnknownSwitchCaseException(direction.name());
    }
  }
}