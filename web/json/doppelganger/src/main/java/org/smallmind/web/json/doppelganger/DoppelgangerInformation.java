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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Aggregates all metadata extracted from a single {@link Doppelganger} annotation, including properties,
 * constraints, imports, pledges, and polymorphic/hierarchy declarations. Acts as the source of truth while
 * generating view classes for different purposes and directions.
 */
public class DoppelgangerInformation {

  private final DirectionalGuide inDirectionalGuide = new DirectionalGuide(Direction.IN);
  private final DirectionalGuide outDirectionalGuide = new DirectionalGuide(Direction.OUT);
  private final List<IdiomInformation> constrainingIdiomList = new LinkedList<>();
  private final HashMap<IdiomKey, HashSet<TypeMirror>> implementationMap = new HashMap<>();
  private final HashMap<IdiomKey, HashSet<String>> importMap = new HashMap<>();
  private final HashMap<String, Visibility> pledgedMap = new HashMap<>();
  private final HashMap<String, Visibility> fulfilledMap = new HashMap<>();
  private final String name;
  private final String namespace;
  private final String comment;
  private final boolean serializable;

  /**
   * Parses Doppelganger annotations on a class and populates property/idiom maps.
   *
   * @param processingEnvironment           current annotation processing environment
   * @param usefulTypeMirrors               cached type mirrors for common annotations
   * @param doppelgangerAnnotationProcessor processor that may recurse into referenced types
   * @param classElement                    the annotated class
   * @param visibilityTracker               tracker that records purposes/visibility per class
   * @param classTracker                    tracker that records polymorphic and hierarchy relationships
   * @param doppelgangerAnnotationMirror    the mirror of the {@link Doppelganger} annotation on the class
   * @throws IOException         if nested type processing fails
   * @throws DefinitionException if the annotation configuration is invalid
   */
  public DoppelgangerInformation (ProcessingEnvironment processingEnvironment, UsefulTypeMirrors usefulTypeMirrors, DoppelgangerAnnotationProcessor doppelgangerAnnotationProcessor, TypeElement classElement, VisibilityTracker visibilityTracker, ClassTracker classTracker, AnnotationMirror doppelgangerAnnotationMirror)
    throws IOException, DefinitionException {

    AnnotationMirror polymorphicAnnotationMirror;
    AnnotationMirror hierarchyAnnotationMirror;

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

    for (AnnotationMirror idiomAnnotationMirror : AptUtility.extractAnnotationValueAsList(doppelgangerAnnotationMirror, "constrainingIdioms", AnnotationMirror.class)) {
      constrainingIdiomList.add(new IdiomInformation(processingEnvironment, usefulTypeMirrors, idiomAnnotationMirror));
    }

    if ((polymorphicAnnotationMirror = AptUtility.extractAnnotationValue(doppelgangerAnnotationMirror, "polymorphic", AnnotationMirror.class, null)) != null) {
      classTracker.addPolymorphic(classElement, new PolymorphicInformation(processingEnvironment, polymorphicAnnotationMirror));
    } else if ((hierarchyAnnotationMirror = AptUtility.extractAnnotationValue(doppelgangerAnnotationMirror, "hierarchy", AnnotationMirror.class, null)) != null) {
      classTracker.addHierarchy(classElement, new HierarchyInformation(processingEnvironment, hierarchyAnnotationMirror));
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

      for (PropertyBox propertyBox : new PropertyParser(processingEnvironment, usefulTypeMirrors, virtualAnnotationMirror, extractType(classElement, fieldName, processingEnvironment, virtualAnnotationMirror), null, true)) {

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
      TypeMirror fieldTypeMirror = extractType(classElement, fieldName, processingEnvironment, realAnnotationMirror);
      AnnotationMirror nullifierAnnotationMirror = null;
      Element fieldElement;

      if ((fieldElement = extractFieldElement(classElement, fieldName)) != null) {
        nullifierAnnotationMirror = AptUtility.extractAnnotationMirrorAnnotatedBy(processingEnvironment, fieldElement, usefulTypeMirrors.getOverlayNullifierTypeMirror());
      }

      for (PropertyBox propertyBox : new PropertyParser(processingEnvironment, usefulTypeMirrors, realAnnotationMirror, fieldTypeMirror, nullifierAnnotationMirror, false)) {

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

  /**
   * Reconstructs a {@link TypeMirror} from the type and parameter values stored in a property annotation.
   *
   * @param classElement             the annotated class containing the field
   * @param fieldName                name of the field being described
   * @param processingEnvironment    current processing environment
   * @param propertyAnnotationMirror the mirror of the property annotation containing the type definition
   * @return the resolved type mirror representing the property type
   * @throws DefinitionException if the type definition is malformed
   */
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

  /**
   * Retrieves the field element with the given name from the class, if it exists.
   *
   * @param classElement the class to search
   * @param fieldName    field name to locate
   * @return the matching field element, or {@code null} when not present
   */
  private Element extractFieldElement (TypeElement classElement, String fieldName) {

    for (Element enclosedElement : classElement.getEnclosedElements()) {
      if (ElementKind.FIELD.equals(enclosedElement.getKind()) && fieldName.equals(enclosedElement.getSimpleName().toString())) {

        return enclosedElement;
      }
    }

    return null;
  }

  /**
   * Updates the {@link VisibilityTracker} with the properties discovered in this annotation.
   *
   * @param classElement      the annotated class
   * @param visibilityTracker tracker to update with new visibility information
   */
  public void update (TypeElement classElement, VisibilityTracker visibilityTracker) {

    for (Map.Entry<String, PropertyLexicon> purposeEntry : inDirectionalGuide.lexiconEntrySet()) {
      visibilityTracker.add(classElement, purposeEntry.getKey(), Visibility.IN, purposeEntry.getValue());
    }
    for (Map.Entry<String, PropertyLexicon> purposeEntry : outDirectionalGuide.lexiconEntrySet()) {
      visibilityTracker.add(classElement, purposeEntry.getKey(), Visibility.OUT, purposeEntry.getValue());
    }
  }

  /**
   * @return explicit root element name configured on the annotation, or an empty string for default naming
   */
  public String getName () {

    return name;
  }

  /**
   * @return namespace configured for generated XML root elements
   */
  public String getNamespace () {

    return namespace;
  }

  /**
   * @return optional comment text to attach to generated views
   */
  public String getComment () {

    return comment;
  }

  /**
   * @return whether generated views should implement {@link java.io.Serializable}
   */
  public boolean isSerializable () {

    return serializable;
  }

  /**
   * Retrieves additional imports that should be applied to a generated view.
   *
   * @param direction the direction (in/out) of the view
   * @param purpose   the purpose identifier
   * @return array of fully qualified import strings
   */
  public String[] getImports (Direction direction, String purpose) {

    HashSet<String> importSet;

    return ((importSet = importMap.get(new IdiomKey(direction, purpose))) == null) ? new String[0] : importSet.toArray(new String[0]);
  }

  /**
   * Retrieves any additional interfaces the generated view should implement.
   *
   * @param direction the direction (in/out) of the view
   * @param purpose   the purpose identifier
   * @return array of interface type mirrors
   */
  public TypeMirror[] getImplementations (Direction direction, String purpose) {

    HashSet<TypeMirror> implementationSet;

    return ((implementationSet = implementationMap.get(new IdiomKey(direction, purpose))) == null) ? new TypeMirror[0] : implementationSet.toArray(new TypeMirror[0]);
  }

  /**
   * @return all constraining idioms configured on the annotated class
   */
  public Iterable<IdiomInformation> constrainingIdioms () {

    return constrainingIdiomList;
  }

  /**
   * @return guide describing inbound properties grouped by purpose
   */
  public DirectionalGuide getInDirectionalGuide () {

    return inDirectionalGuide;
  }

  /**
   * @return guide describing outbound properties grouped by purpose
   */
  public DirectionalGuide getOutDirectionalGuide () {

    return outDirectionalGuide;
  }

  /**
   * Marks a purpose/direction as fulfilled to detect unused pledges.
   *
   * @param purpose   the purpose that has been generated
   * @param direction the direction in which it has been generated
   */
  public void denotePurpose (String purpose, Direction direction) {

    fulfilledMap.put(purpose, direction.getVisibility().compose(fulfilledMap.get(purpose)));
  }

  /**
   * Determines which pledged purposes remain unfulfilled for a given direction.
   *
   * @param classElement      the annotated class
   * @param visibilityTracker tracker holding pledged/realized purposes
   * @param direction         direction to test
   * @return iterable of purposes still lacking generated output
   */
  public Iterable<String> unfulfilledPurposes (TypeElement classElement, VisibilityTracker visibilityTracker, Direction direction) {

    return visibilityTracker.unfulfilledPurposes(classElement, direction, fulfilledMap);
  }

  /**
   * Calculates pledged purposes that were unnecessary because actual properties already satisfied visibility.
   *
   * @param classElement      the annotated class
   * @param visibilityTracker tracker holding pledged/realized purposes
   * @param direction         direction to test
   * @return array of overwrought purpose names
   */
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

    /**
     * Builds a key combining direction and purpose for lookup in maps.
     *
     * @param direction direction in which a view is generated
     * @param purpose   idiom purpose identifier (empty for default)
     */
    public IdiomKey (Direction direction, String purpose) {

      this.direction = direction;
      this.purpose = purpose;
    }

    /**
     * @return the direction component of the key
     */
    public Direction getDirection () {

      return direction;
    }

    /**
     * @return the purpose component of the key (may be empty)
     */
    public String getPurpose () {

      return purpose;
    }

    /**
     * Computes a composite hash from direction and purpose.
     *
     * @return hash code for the key
     */
    @Override
    public int hashCode () {

      return (direction.hashCode() * 31) + (purpose == null ? 0 : purpose.hashCode());
    }

    /**
     * Compares keys for equality based on direction and purpose.
     *
     * @param obj object to compare
     * @return {@code true} when both direction and purpose match
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof IdiomKey) && ((IdiomKey)obj).getDirection().equals(direction) && Objects.equals(((IdiomKey)obj).getPurpose(), purpose);
    }
  }
}
