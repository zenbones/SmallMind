/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class DoppelgangerInformation {

  private final DirectionalGuide inDirectionalGuide = new DirectionalGuide(Direction.IN);
  private final DirectionalGuide outDirectionalGuide = new DirectionalGuide(Direction.OUT);
  private final List<ConstraintInformation> constraintList = new LinkedList<>();
  private final HashMap<IdiomKey, HashSet<TypeMirror>> implementationMap = new HashMap<>();
  private final HashMap<IdiomKey, HashSet<String>> importMap = new HashMap<>();
  private final HashMap<String, Visibility> pledgedMap = new HashMap<>();
  private final HashMap<String, Visibility> fulfilledMap = new HashMap<>();
  private final String name;
  private final String namespace;
  private final String comment;
  private final boolean serializable;

  public DoppelgangerInformation (ProcessingEnvironment processingEnvironment, UsefulTypeMirrors usefulTypeMirrors, DoppelgangerAnnotationProcessor doppelgangerAnnotationProcessor, TypeElement classElement, VisibilityTracker visibilityTracker, ClassTracker classTracker, AnnotationMirror doppelgangerAnnotationMirror)
    throws IOException, DefinitionException {

    AnnotationMirror polymorphicAnnotationMirror;

    name = AptUtility.extractAnnotationValue(doppelgangerAnnotationMirror, "name", String.class, "");
    namespace = AptUtility.extractAnnotationValue(doppelgangerAnnotationMirror, "namespace", String.class, "http://org.smallmind/web/json/doppelganger");
    serializable = AptUtility.extractAnnotationValue(doppelgangerAnnotationMirror, "serializable", Boolean.class, false);
    comment = AptUtility.extractAnnotationValue(doppelgangerAnnotationMirror, "comment", String.class, "");

    for (AnnotationMirror importAnnotationMirror : AptUtility.extractAnnotationValueAsList(doppelgangerAnnotationMirror, "imports", AnnotationMirror.class)) {

      List<String> purposeList = AptUtility.extractAnnotationValueAsList(importAnnotationMirror, "purposes", String.class);
      Visibility visibility = AptUtility.extractAnnotationValue(importAnnotationMirror, "visibility", Visibility.class, Visibility.BOTH);

      for (Direction direction : Direction.values()) {
        if (visibility.matches(direction)) {
          if (purposeList.isEmpty()) {

            HashSet<String> importSet;
            IdiomKey idiomKey = new IdiomKey(direction, "");

            if ((importSet = importMap.get(idiomKey)) == null) {
              importMap.put(idiomKey, importSet = new HashSet<>());
            }
            importSet.addAll(AptUtility.extractAnnotationValueAsList(importAnnotationMirror, "value", String.class));
          } else {
            for (String purpose : purposeList) {

              HashSet<String> importSet;
              IdiomKey idiomKey = new IdiomKey(direction, purpose);

              if ((importSet = importMap.get(idiomKey)) == null) {
                importMap.put(idiomKey, importSet = new HashSet<>());
              }
              importSet.addAll(AptUtility.extractAnnotationValueAsList(importAnnotationMirror, "value", String.class));
            }
          }
        }
      }
    }

    for (AnnotationMirror implementationAnnotationMirror : AptUtility.extractAnnotationValueAsList(doppelgangerAnnotationMirror, "implementations", AnnotationMirror.class)) {

      List<String> purposeList = AptUtility.extractAnnotationValueAsList(implementationAnnotationMirror, "purposes", String.class);
      Visibility visibility = AptUtility.extractAnnotationValue(implementationAnnotationMirror, "visibility", Visibility.class, Visibility.BOTH);

      for (Direction direction : Direction.values()) {
        if (visibility.matches(direction)) {
          if (purposeList.isEmpty()) {

            HashSet<TypeMirror> implementationSet;
            IdiomKey idiomKey = new IdiomKey(direction, "");

            if ((implementationSet = implementationMap.get(idiomKey)) == null) {
              implementationMap.put(idiomKey, implementationSet = new HashSet<>());
            }
            implementationSet.addAll(AptUtility.extractAnnotationValueAsList(implementationAnnotationMirror, "value", TypeMirror.class));
          } else {
            for (String purpose : purposeList) {

              HashSet<TypeMirror> implementationSet;
              IdiomKey idiomKey = new IdiomKey(direction, purpose);

              if ((implementationSet = implementationMap.get(idiomKey)) == null) {
                implementationMap.put(idiomKey, implementationSet = new HashSet<>());
              }
              implementationSet.addAll(AptUtility.extractAnnotationValueAsList(implementationAnnotationMirror, "value", TypeMirror.class));
            }
          }
        }
      }
    }

    for (AnnotationMirror constraintAnnotationMirror : AptUtility.extractAnnotationValueAsList(doppelgangerAnnotationMirror, "constraints", AnnotationMirror.class)) {
      constraintList.add(new ConstraintInformation(constraintAnnotationMirror));
    }

    if ((polymorphicAnnotationMirror = AptUtility.extractAnnotationValue(doppelgangerAnnotationMirror, "polymorphic", AnnotationMirror.class, null)) != null) {
      classTracker.addPolymorphic(classElement, new PolymorphicInformation(processingEnvironment, polymorphicAnnotationMirror));
    }

    for (AnnotationMirror pledgeAnnotationMirror : AptUtility.extractAnnotationValueAsList(doppelgangerAnnotationMirror, "pledges", AnnotationMirror.class)) {

      PledgeInformation pledgeInformation = new PledgeInformation(pledgeAnnotationMirror);

      for (String purpose : pledgeInformation.getPurposeList()) {
        visibilityTracker.add(classElement, purpose, pledgeInformation.getVisibility(), true);
        pledgedMap.put(purpose, pledgeInformation.getVisibility().compose(pledgedMap.get(purpose)));
      }
    }

    for (AnnotationMirror virtualAnnotationMirror : AptUtility.extractAnnotationValueAsList(doppelgangerAnnotationMirror, "virtual", AnnotationMirror.class)) {

      String fieldName = AptUtility.extractAnnotationValue(virtualAnnotationMirror, "field", String.class, null);

      for (PropertyBox propertyBox : new PropertyParser(processingEnvironment, usefulTypeMirrors, virtualAnnotationMirror, extractType(classElement, fieldName, processingEnvironment, virtualAnnotationMirror), true)) {

        doppelgangerAnnotationProcessor.processTypeMirror(propertyBox.getPropertyInformation().getType());

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

    for (AnnotationMirror realAnnotationMirror : AptUtility.extractAnnotationValueAsList(doppelgangerAnnotationMirror, "real", AnnotationMirror.class)) {

      String fieldName = AptUtility.extractAnnotationValue(realAnnotationMirror, "field", String.class, null);

      for (PropertyBox propertyBox : new PropertyParser(processingEnvironment, usefulTypeMirrors, realAnnotationMirror, extractType(classElement, fieldName, processingEnvironment, realAnnotationMirror), false)) {

        doppelgangerAnnotationProcessor.processTypeMirror(propertyBox.getPropertyInformation().getType());

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

  private TypeMirror extractType (TypeElement classElement, String fieldName, ProcessingEnvironment processingEnvironment, AnnotationMirror propertyAnnotationMirror)
    throws DefinitionException {

    AnnotationMirror typeAnnotationMirror = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "type", AnnotationMirror.class, null);
    TypeMirror baseTypeMirror = AptUtility.extractAnnotationValue(typeAnnotationMirror, "value", TypeMirror.class, null);
    List<TypeMirror> argumentTypeMirrorList = AptUtility.extractAnnotationValueAsList(typeAnnotationMirror, "parameters", TypeMirror.class);
    TypeMirror[] argumentTypeMirrors = new TypeMirror[argumentTypeMirrorList.size()];

    argumentTypeMirrorList.toArray(argumentTypeMirrors);

    try {
      if (TypeKind.ARRAY.equals(baseTypeMirror.getKind())) {
        if (argumentTypeMirrors.length > 0) {
          throw new DefinitionException("Illegal type definition in field(%s) of class(%s), array types can't have type arguments", fieldName, classElement);
        }

        return processingEnvironment.getTypeUtils().getArrayType(((ArrayType)baseTypeMirror).getComponentType());
      } else {

        return processingEnvironment.getTypeUtils().getDeclaredType((TypeElement)processingEnvironment.getTypeUtils().asElement(baseTypeMirror), argumentTypeMirrors);
      }
    } catch (Exception exception) {
      throw new DefinitionException(exception, "Illegal type definition in field(%s) of class(%s)", fieldName, classElement);
    }
  }

  public void update (TypeElement classElement, VisibilityTracker visibilityTracker) {

    for (Map.Entry<String, PropertyLexicon> purposeEntry : inDirectionalGuide.lexiconEntrySet()) {
      visibilityTracker.add(classElement, purposeEntry.getKey(), Visibility.IN, purposeEntry.getValue());
    }
    for (Map.Entry<String, PropertyLexicon> purposeEntry : outDirectionalGuide.lexiconEntrySet()) {
      visibilityTracker.add(classElement, purposeEntry.getKey(), Visibility.OUT, purposeEntry.getValue());
    }
  }

  public String getName () {

    return name;
  }

  public String getNamespace () {

    return namespace;
  }

  public String getComment () {

    return comment;
  }

  public boolean isSerializable () {

    return serializable;
  }

  public String[] getImports (Direction direction, String purpose) {

    HashSet<String> importSet;

    return ((importSet = importMap.get(new IdiomKey(direction, purpose))) == null) ? new String[0] : importSet.toArray(new String[0]);
  }

  public TypeMirror[] getImplementations (Direction direction, String purpose) {

    HashSet<TypeMirror> implementationSet;

    return ((implementationSet = implementationMap.get(new IdiomKey(direction, purpose))) == null) ? new TypeMirror[0] : implementationSet.toArray(new TypeMirror[0]);
  }

  public Iterable<ConstraintInformation> constraints () {

    return constraintList;
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

  private static class IdiomKey {

    private final Direction direction;
    private final String purpose;

    public IdiomKey (Direction direction, String purpose) {

      this.direction = direction;
      this.purpose = purpose;
    }

    public Direction getDirection () {

      return direction;
    }

    public String getPurpose () {

      return purpose;
    }

    @Override
    public int hashCode () {

      return (direction.hashCode() * 31) + (purpose == null ? 0 : purpose.hashCode());
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof IdiomKey) && ((IdiomKey)obj).getDirection().equals(direction) && Objects.equals(((IdiomKey)obj).getPurpose(), purpose);
    }
  }
}